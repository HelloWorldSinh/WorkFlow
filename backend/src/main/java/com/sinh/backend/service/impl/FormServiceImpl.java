package com.sinh.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinh.backend.dto.request.CreateFormRequest;
import com.sinh.backend.dto.request.UpdateFormRequest;
import com.sinh.backend.dto.response.FormResponseDTO;
import com.sinh.backend.entity.Form;
import com.sinh.backend.entity.FormField;
import com.sinh.backend.entity.User;
import com.sinh.backend.exception.BadRequestException;
import com.sinh.backend.exception.ResourceNotFoundException;
import com.sinh.backend.repository.FormRepository;
import com.sinh.backend.repository.UserRepository;
import com.sinh.backend.service.FormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FormServiceImpl implements FormService {

    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public FormResponseDTO createForm(CreateFormRequest request) {
        String trimmedName = request.getName().trim();
        if (formRepository.existsByNameIgnoreCaseAndIsActiveTrue(trimmedName)) {
            throw new BadRequestException("Tên biểu mẫu đã tồn tại trong hệ thống: " + trimmedName);
        }

        User createdBy = null;
        if (request.getCreatedById() != null) {
            createdBy = userRepository.findById(request.getCreatedById()).orElse(null);
        }
        if (createdBy == null) {
            createdBy = userRepository.findAll().stream().findFirst().orElse(null);
        }

        Form form = Form.builder()
                .name(trimmedName)
                .description(request.getDescription())
                .createdBy(createdBy)
                .isActive(true)
                .fields(new ArrayList<>())
                .build();

        parseAndAddFields(form, request.getSchema());

        Form savedForm = formRepository.save(form);
        log.info("Đã tạo mới Form ID: {}, Tên: {} với {} fields", savedForm.getId(), savedForm.getName(), savedForm.getFields().size());
        return mapToResponseDTO(savedForm);
    }

    @Override
    public FormResponseDTO updateForm(Integer id, UpdateFormRequest request) {
        Form form = formRepository.findByIdWithFields(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biểu mẫu với ID: " + id));

        String trimmedName = request.getName().trim();
        if (formRepository.existsByNameIgnoreCaseAndIdNotAndIsActiveTrue(trimmedName, id)) {
            throw new BadRequestException("Tên biểu mẫu đã trùng với biểu mẫu khác: " + trimmedName);
        }

        form.setName(trimmedName);
        form.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            form.setIsActive(request.getIsActive());
        }

        // Xóa các fields cũ và thêm lại danh sách fields mới (CascadeType.ALL + orphanRemoval = true)
        form.getFields().clear();
        parseAndAddFields(form, request.getSchema());

        Form updatedForm = formRepository.save(form);
        log.info("Đã cập nhật Form ID: {}, Tên: {} với {} fields", updatedForm.getId(), updatedForm.getName(), updatedForm.getFields().size());
        return mapToResponseDTO(updatedForm);
    }

    @Override
    @Transactional(readOnly = true)
    public FormResponseDTO getFormById(Integer id) {
        Form form = formRepository.findByIdWithFields(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biểu mẫu với ID: " + id));
        return mapToResponseDTO(form);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormResponseDTO> getAllForms(String keyword) {
        List<Form> forms;
        if (keyword != null && !keyword.trim().isEmpty()) {
            forms = formRepository.searchActiveFormsWithFields(keyword.trim());
        } else {
            forms = formRepository.findAllActiveFormsWithFields();
        }

        return forms.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteForm(Integer id) {
        Form form = formRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biểu mẫu với ID: " + id));

        // Xóa mềm: đánh dấu isActive = false
        form.setIsActive(false);
        formRepository.save(form);
        log.info("Đã xóa mềm Form ID: {}", id);
    }

    private void parseAndAddFields(Form form, Map<String, Object> schemaMap) {
        if (schemaMap == null) return;

        Object fieldsObj = schemaMap.get("fields");
        if (fieldsObj == null && schemaMap.containsKey("schema")) {
            Object nested = schemaMap.get("schema");
            if (nested instanceof Map<?, ?> nestedMap) {
                fieldsObj = nestedMap.get("fields");
            }
        }
        if (fieldsObj == null) {
            fieldsObj = schemaMap;
        }

        if (fieldsObj instanceof List<?> fieldsList) {
            int order = 0;
            for (Object item : fieldsList) {
                if (item instanceof Map<?, ?> fMap) {
                    String key = fMap.get("key") != null ? String.valueOf(fMap.get("key")) : "field_" + (order + 1);
                    String label = fMap.get("label") != null ? String.valueOf(fMap.get("label")) : key;
                    String type = fMap.get("type") != null ? String.valueOf(fMap.get("type")) : "text";
                    String placeholder = fMap.get("placeholder") != null ? String.valueOf(fMap.get("placeholder")) : null;
                    String helpText = fMap.get("helpText") != null ? String.valueOf(fMap.get("helpText")) : null;
                    boolean required = fMap.get("required") != null && Boolean.parseBoolean(String.valueOf(fMap.get("required")));
                    String defaultValue = fMap.get("defaultValue") != null ? String.valueOf(fMap.get("defaultValue")) : null;

                    Integer minVal = null;
                    if (fMap.get("min") instanceof Number num) {
                        minVal = num.intValue();
                    } else if (fMap.get("min") != null) {
                        try { minVal = Integer.parseInt(String.valueOf(fMap.get("min"))); } catch (Exception ignored) {}
                    }

                    Integer maxVal = null;
                    if (fMap.get("max") instanceof Number num) {
                        maxVal = num.intValue();
                    } else if (fMap.get("max") != null) {
                        try { maxVal = Integer.parseInt(String.valueOf(fMap.get("max"))); } catch (Exception ignored) {}
                    }

                    String optionsJson = null;
                    Map<String, Object> extraProps = new LinkedHashMap<>();
                    if (fMap.get("options") != null) {
                        extraProps.put("options", fMap.get("options"));
                    }
                    if (fMap.get("columns") != null) {
                        extraProps.put("columns", fMap.get("columns"));
                    }
                    if (fMap.get("summaryFieldKey") != null) {
                        extraProps.put("summaryFieldKey", fMap.get("summaryFieldKey"));
                    }
                    if (fMap.get("addBtnText") != null) {
                        extraProps.put("addBtnText", fMap.get("addBtnText"));
                    }

                    if (!extraProps.isEmpty()) {
                        try {
                            optionsJson = objectMapper.writeValueAsString(extraProps);
                        } catch (Exception ignored) {}
                    }

                    FormField fieldEntity = FormField.builder()
                            .form(form)
                            .fieldKey(key)
                            .label(label)
                            .type(type)
                            .placeholder(placeholder)
                            .helpText(helpText)
                            .required(required)
                            .defaultValue(defaultValue)
                            .minVal(minVal)
                            .maxVal(maxVal)
                            .options(optionsJson)
                            .orderIndex(order++)
                            .build();

                    form.getFields().add(fieldEntity);
                }
            }
        }
    }

    private FormResponseDTO mapToResponseDTO(Form form) {
        Map<String, Object> schemaMap = new LinkedHashMap<>();
        List<Map<String, Object>> fieldsList = new ArrayList<>();

        if (form.getFields() != null) {
            for (FormField f : form.getFields()) {
                Map<String, Object> fObj = new LinkedHashMap<>();
                fObj.put("id", f.getId() != null ? f.getId() : "f_" + f.getFieldKey());
                fObj.put("key", f.getFieldKey());
                fObj.put("label", f.getLabel());
                fObj.put("type", f.getType());
                if (f.getPlaceholder() != null) fObj.put("placeholder", f.getPlaceholder());
                if (f.getHelpText() != null) fObj.put("helpText", f.getHelpText());
                fObj.put("required", Boolean.TRUE.equals(f.getRequired()));
                if (f.getDefaultValue() != null) fObj.put("defaultValue", f.getDefaultValue());
                if (f.getMinVal() != null) fObj.put("min", f.getMinVal());
                if (f.getMaxVal() != null) fObj.put("max", f.getMaxVal());

                if (f.getOptions() != null && !f.getOptions().trim().isEmpty()) {
                    try {
                        Object parsed = objectMapper.readValue(f.getOptions(), Object.class);
                        if (parsed instanceof Map<?, ?> pMap) {
                            if (pMap.containsKey("options")) fObj.put("options", pMap.get("options"));
                            else fObj.put("options", Collections.emptyList());

                            if (pMap.containsKey("columns")) fObj.put("columns", pMap.get("columns"));
                            if (pMap.containsKey("summaryFieldKey")) fObj.put("summaryFieldKey", pMap.get("summaryFieldKey"));
                            if (pMap.containsKey("addBtnText")) fObj.put("addBtnText", pMap.get("addBtnText"));
                        } else if (parsed instanceof List<?> pList) {
                            fObj.put("options", pList);
                        } else {
                            fObj.put("options", Collections.emptyList());
                        }
                    } catch (Exception ignored) {
                        fObj.put("options", Collections.emptyList());
                    }
                } else {
                    fObj.put("options", Collections.emptyList());
                }

                fieldsList.add(fObj);
            }
        }
        schemaMap.put("fields", fieldsList);

        return FormResponseDTO.builder()
                .id(form.getId())
                .name(form.getName())
                .description(form.getDescription())
                .schema(schemaMap)
                .createdById(form.getCreatedBy() != null ? form.getCreatedBy().getId() : null)
                .createdByName(form.getCreatedBy() != null ? form.getCreatedBy().getFullName() : null)
                .createdAt(form.getCreatedAt())
                .isActive(form.getIsActive())
                .build();
    }
}
