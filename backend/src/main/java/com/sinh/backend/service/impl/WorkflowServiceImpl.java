package com.sinh.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinh.backend.dto.request.CreateWorkflowRequest;
import com.sinh.backend.dto.request.SaveWorkflowGraphRequest;
import com.sinh.backend.dto.request.UpdateWorkflowRequest;
import com.sinh.backend.dto.request.WorkflowFilterRequest;
import com.sinh.backend.dto.response.DeleteWorkflowResponseDTO;
import com.sinh.backend.dto.response.PageResponse;
import com.sinh.backend.dto.response.WorkflowGraphResponseDTO;
import com.sinh.backend.dto.response.WorkflowResponseDTO;
import com.sinh.backend.entity.Form;
import com.sinh.backend.entity.Node;
import com.sinh.backend.entity.Transition;
import com.sinh.backend.entity.User;
import com.sinh.backend.entity.Workflow;
import com.sinh.backend.entity.WorkflowVersion;
import com.sinh.backend.entity.enums.NodeType;
import com.sinh.backend.entity.enums.WorkflowInstanceStatus;
import com.sinh.backend.entity.enums.WorkflowStatus;
import com.sinh.backend.exception.BadRequestException;
import com.sinh.backend.exception.ResourceNotFoundException;
import com.sinh.backend.repository.*;
import com.sinh.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final UserRepository userRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final NodeRepository nodeRepository;
    private final TransitionRepository transitionRepository;
    private final FormRepository formRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkflowResponseDTO> getAllWorkflows(WorkflowFilterRequest filter, Pageable pageable) {
        if (filter == null) {
            filter = new WorkflowFilterRequest();
        }

        String keyword = null;
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            keyword = filter.getKeyword().trim();
            // Nếu người dùng gõ tìm kiếm theo mã kiểu "WF-001" hoặc "WF1", chuẩn hóa để tìm được theo ID
            String cleanCode = keyword.replaceAll("(?i)^wf-?", "");
            if (!cleanCode.isEmpty() && cleanCode.matches("\\d+")) {
                keyword = String.valueOf(Integer.parseInt(cleanCode));
            }
        }
        boolean includeDeleted = Boolean.TRUE.equals(filter.getIncludeDeleted());

        Page<Workflow> workflowPage = workflowRepository.searchAllWorkflows(
                keyword,
                filter.getStatus(),
                filter.getOwnerId(),
                includeDeleted,
                pageable
        );

        Page<WorkflowResponseDTO> dtoPage = workflowPage.map(this::mapToResponseDTO);
        return PageResponse.of(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponseDTO getWorkflowById(Integer id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy workflow với ID: " + id));

        return mapToResponseDTO(workflow);
    }

    @Override
    public WorkflowResponseDTO createWorkflow(CreateWorkflowRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng (owner) với ID: " + request.getOwnerId()));

        Workflow workflow = Workflow.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .owner(owner)
                .status(request.getStatus() != null ? request.getStatus() : WorkflowStatus.Draft)
                .build();

        Workflow savedWorkflow = workflowRepository.save(workflow);

        // Tạo mặc định phiên bản đầu tiên (Version 1.0)
        WorkflowVersion initialVersion = WorkflowVersion.builder()
                .workflow(savedWorkflow)
                .versionNumber("1.0")
                .isActive(true)
                .build();
        workflowVersionRepository.save(initialVersion);

        log.info("Đã tạo mới Workflow ID: {}, Tên: {}", savedWorkflow.getId(), savedWorkflow.getName());
        return mapToResponseDTO(savedWorkflow);
    }

    @Override
    public WorkflowResponseDTO updateWorkflow(Integer id, UpdateWorkflowRequest request) {
        Workflow workflow = workflowRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy workflow còn hoạt động với ID: " + id));

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng (owner) với ID: " + request.getOwnerId()));
            workflow.setOwner(owner);
        }

        workflow.setName(request.getName().trim());
        workflow.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            workflow.setStatus(request.getStatus());
        }

        Workflow updatedWorkflow = workflowRepository.save(workflow);
        log.info("Đã cập nhật Workflow ID: {}", updatedWorkflow.getId());
        return mapToResponseDTO(updatedWorkflow);
    }

    @Override
    public DeleteWorkflowResponseDTO deleteWorkflow(Integer id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy workflow với ID: " + id));

        if (workflow.getDeletedAt() != null || workflow.getStatus() == WorkflowStatus.Deleted) {
            throw new BadRequestException("Workflow với ID " + id + " đã bị xóa trước đó.");
        }

        // 1. Kiểm tra xem có instance nào đang chạy (Running) hay không
        boolean hasRunningInstances = workflowInstanceRepository.existsByWorkflowIdAndStatus(id, WorkflowInstanceStatus.Running);
        if (hasRunningInstances) {
            throw new BadRequestException("Không thể xóa workflow vì đang có instance đang chạy (Running). Vui lòng kết thúc hoặc hủy instance trước khi xóa.");
        }

        // 2. Kiểm tra xem workflow đã từng có instance nào trong lịch sử hay chưa
        boolean hasAnyInstances = workflowInstanceRepository.existsByWorkflowId(id);

        if (hasAnyInstances) {
            // === XÓA MỀM (Soft Delete) ===
            workflow.setStatus(WorkflowStatus.Deleted);
            workflow.setDeletedAt(LocalDateTime.now());
            workflowRepository.save(workflow);

            log.info("Workflow ID: {} đã có instance thực thi -> Thực hiện XÓA MỀM (Soft Delete)", id);

            return DeleteWorkflowResponseDTO.builder()
                    .workflowId(id)
                    .deleteType("SOFT_DELETE")
                    .message("Workflow đã từng có instance thực thi, hệ thống đã thực hiện XÓA MỀM (Soft Delete).")
                    .build();
        } else {
            // === XÓA CỨNG (Hard Delete) ===
            transitionRepository.deleteByWorkflowId(id);
            nodeRepository.deleteByWorkflowId(id);
            workflowVersionRepository.deleteByWorkflowId(id);
            workflowRepository.delete(workflow);

            log.info("Workflow ID: {} chưa có instance nào -> Thực hiện XÓA CỨNG (Hard Delete)", id);

            return DeleteWorkflowResponseDTO.builder()
                    .workflowId(id)
                    .deleteType("HARD_DELETE")
                    .message("Workflow chưa có instance nào, hệ thống đã thực hiện XÓA CỨNG (Hard Delete) thành công khỏi cơ sở dữ liệu.")
                    .build();
        }
    }

    @Override
    public WorkflowResponseDTO restoreWorkflow(Integer id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy workflow với ID: " + id));

        if (workflow.getDeletedAt() == null && workflow.getStatus() != WorkflowStatus.Deleted) {
            throw new BadRequestException("Workflow với ID " + id + " không nằm trong trạng thái đã xóa.");
        }

        workflow.setDeletedAt(null);
        workflow.setStatus(WorkflowStatus.Draft);
        Workflow restoredWorkflow = workflowRepository.save(workflow);

        log.info("Đã khôi phục (Restore) Workflow ID: {} về trạng thái Draft", id);
        return mapToResponseDTO(restoredWorkflow);
    }

    @Override
    public WorkflowGraphResponseDTO saveWorkflowGraph(Integer id, SaveWorkflowGraphRequest request) {
        Workflow workflow = workflowRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy workflow với ID: " + id));

        // 1. Cập nhật thông tin chung nếu có
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            workflow.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription().trim());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                WorkflowStatus nextStatus = WorkflowStatus.valueOf(request.getStatus().trim());
                workflow.setStatus(nextStatus);
            } catch (IllegalArgumentException ignored) {
            }
        }
        workflowRepository.save(workflow);

        // 2. Xóa các transitions cũ của workflow này để ghi đè sơ đồ mới
        transitionRepository.deleteByWorkflowId(id);

        // Lấy danh sách nodes hiện tại của workflow để cập nhật hoặc tái sử dụng
        List<Node> existingNodes = nodeRepository.findByWorkflowId(id);
        Map<String, Node> existingNodeClientMap = new HashMap<>();
        for (Node en : existingNodes) {
            Map<String, Object> cfg = parseJsonMap(en.getConfig());
            String clientId = (String) cfg.get("clientNodeId");
            if (clientId != null) {
                existingNodeClientMap.put(clientId, en);
            }
        }

        Map<String, Node> createdNodeMap = new HashMap<>();
        Set<Integer> preservedNodeIds = new HashSet<>();

        // 3. Lưu danh sách Nodes mới hoặc cập nhật node đã có
        if (request.getNodes() != null) {
            for (SaveWorkflowGraphRequest.NodeDTO nodeDTO : request.getNodes()) {
                NodeType nodeType = parseNodeType(nodeDTO.getType());

                Map<String, Object> nodeConfigMap = new HashMap<>();
                if (nodeDTO.getConfig() != null) {
                    nodeConfigMap.putAll(nodeDTO.getConfig());
                }
                if (nodeDTO.getPosition() != null) {
                    nodeConfigMap.put("position", Map.of(
                            "x", nodeDTO.getPosition().getX() != null ? nodeDTO.getPosition().getX() : 0.0,
                            "y", nodeDTO.getPosition().getY() != null ? nodeDTO.getPosition().getY() : 0.0
                    ));
                }
                if (nodeDTO.getDescription() != null) {
                    nodeConfigMap.put("description", nodeDTO.getDescription());
                }
                if (nodeDTO.getId() != null) {
                    nodeConfigMap.put("clientNodeId", nodeDTO.getId());
                }

                // Lấy formId từ nodeDTO (trực tiếp hoặc từ formBinding)
                Integer formId = nodeDTO.getFormId();
                if (formId == null && nodeDTO.getFormBinding() instanceof Map<?, ?> fbMap) {
                    Object fidObj = fbMap.get("formId");
                    if (fidObj instanceof Number) {
                        formId = ((Number) fidObj).intValue();
                    } else if (fidObj instanceof String str && !str.trim().isEmpty()) {
                        try {
                            formId = Integer.parseInt(str.trim());
                        } catch (NumberFormatException ignored) {
                            // Ignored
                        }
                    }
                }

                Form form = null;
                if (formId != null) {
                    form = formRepository.findById(formId).orElse(null);
                }

                if (nodeDTO.getFormBinding() != null) {
                    nodeConfigMap.put("formBinding", nodeDTO.getFormBinding());
                } else if (form != null) {
                    nodeConfigMap.put("formBinding", Map.of(
                            "formId", form.getId(),
                            "formName", form.getName()
                    ));
                }

                String configJson = null;
                try {
                    configJson = objectMapper.writeValueAsString(nodeConfigMap);
                } catch (Exception e) {
                    log.error("Lỗi parse config JSON node {}: {}", nodeDTO.getName(), e.getMessage());
                }

                // Tái sử dụng Node cũ nếu có cùng clientNodeId
                Node targetNode = nodeDTO.getId() != null ? existingNodeClientMap.get(nodeDTO.getId()) : null;
                if (targetNode != null) {
                    targetNode.setName(nodeDTO.getName() != null ? nodeDTO.getName() : nodeType.name());
                    targetNode.setType(nodeType);
                    targetNode.setConfig(configJson);
                    targetNode.setForm(form);
                } else {
                    targetNode = Node.builder()
                            .workflow(workflow)
                            .type(nodeType)
                            .name(nodeDTO.getName() != null ? nodeDTO.getName() : nodeType.name())
                            .config(configJson)
                            .form(form)
                            .build();
                }

                Node savedNode = nodeRepository.save(targetNode);
                preservedNodeIds.add(savedNode.getId());
                if (nodeDTO.getId() != null) {
                    createdNodeMap.put(nodeDTO.getId(), savedNode);
                }
            }
        }

        // Xóa những node cũ không còn trong đồ thị và không bị tham chiếu
        for (Node en : existingNodes) {
            if (!preservedNodeIds.contains(en.getId())) {
                try {
                    nodeRepository.delete(en);
                } catch (Exception e) {
                    log.warn("Không thể xóa node cũ ID {} do có ràng buộc khóa ngoại: {}", en.getId(), e.getMessage());
                }
            }
        }

        // 4. Lưu danh sách Transitions (Đường nối)
        if (request.getEdges() != null) {
            for (SaveWorkflowGraphRequest.EdgeDTO edgeDTO : request.getEdges()) {
                Node sourceNode = createdNodeMap.get(edgeDTO.getFromNodeId());
                Node targetNode = createdNodeMap.get(edgeDTO.getToNodeId());

                if (sourceNode != null && targetNode != null) {
                    // 1. Re-validate & parse expression to nested JSON tree with DataType inference
                    String expression = edgeDTO.getConditionExpression();
                    if ((expression == null || expression.trim().isEmpty()) && edgeDTO.getConditions() != null) {
                        expression = generateConditionExpression(edgeDTO.getConditions());
                    }

                    Map<String, Object> conditionTree = Collections.emptyMap();
                    if (expression != null && !expression.trim().isEmpty()) {
                        try {
                            conditionTree = com.sinh.backend.util.ConditionExpressionParser.parseToTree(expression);
                        } catch (IllegalArgumentException e) {
                            log.error("Cú pháp biểu thức không hợp lệ cho edge {}: {}", edgeDTO.getId(), e.getMessage());
                            throw e;
                        }
                    }

                    // 2. Nhãn hiển thị: ưu tiên nhãn người dùng nhập, nếu để trống thì lấy biểu thức điều kiện
                    String label = edgeDTO.getLabel();
                    if ((label == null || label.trim().isEmpty()) && expression != null && !expression.trim().isEmpty()) {
                        label = expression;
                    }

                    // 3. Đóng gói payload JSON cấu hình điều kiện
                    Map<String, Object> conditionMap = new HashMap<>();
                    if (expression != null && !expression.trim().isEmpty()) {
                        conditionMap.put("expression", expression);
                        conditionMap.put("tree", conditionTree);
                        conditionMap.put("conditionTree", conditionTree);
                    }
                    if (edgeDTO.getMatchType() != null) {
                        conditionMap.put("matchType", edgeDTO.getMatchType());
                    } else if (edgeDTO.getConditions() != null) {
                        conditionMap.put("matchType", "CUSTOM");
                    } else {
                        conditionMap.put("matchType", "ALWAYS");
                    }

                    if (edgeDTO.getConditions() != null) {
                        conditionMap.put("rules", edgeDTO.getConditions());
                        conditionMap.put("conditions", edgeDTO.getConditions());
                    }
                    if (edgeDTO.getId() != null) {
                        conditionMap.put("clientEdgeId", edgeDTO.getId());
                    }
                    if (edgeDTO.getBranchType() != null) {
                        conditionMap.put("branchType", edgeDTO.getBranchType());
                    }

                    String conditionJson = null;
                    try {
                        conditionJson = objectMapper.writeValueAsString(conditionMap);
                    } catch (Exception e) {
                        log.error("Lỗi parse condition JSON edge {}: {}", edgeDTO.getId(), e.getMessage());
                    }

                    // 4. Lưu Transition xuống database (cột label và cột conditions)
                    Transition transition = Transition.builder()
                            .workflow(workflow)
                            .sourceNode(sourceNode)
                            .targetNode(targetNode)
                            .label(label)
                            .conditions(conditionJson)
                            .build();

                    transitionRepository.save(transition);
                }
            }
        }

        log.info("Đã lưu thành công Graph cho Workflow ID: {} ({} nodes, {} edges)",
                id,
                request.getNodes() != null ? request.getNodes().size() : 0,
                request.getEdges() != null ? request.getEdges().size() : 0);

        return getWorkflowGraph(id);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowGraphResponseDTO getWorkflowGraph(Integer id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy workflow với ID: " + id));

        List<Node> dbNodes = nodeRepository.findByWorkflowId(id);
        List<Transition> dbTransitions = transitionRepository.findByWorkflowId(id);

        Map<Integer, String> nodeDbIdToClientIdMap = new HashMap<>();
        List<WorkflowGraphResponseDTO.NodeDetailDTO> nodeDetailDTOs = new ArrayList<>();

        for (Node node : dbNodes) {
            Map<String, Object> configMap = new HashMap<>();
            if (node.getConfig() != null && !node.getConfig().trim().isEmpty()) {
                try {
                    configMap = objectMapper.readValue(node.getConfig(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception ignored) {
                }
            }

            String clientId = (String) configMap.getOrDefault("clientNodeId", "node-" + node.getId());
            nodeDbIdToClientIdMap.put(node.getId(), clientId);

            String description = (String) configMap.getOrDefault("description", "");

            WorkflowGraphResponseDTO.PositionDTO position = new WorkflowGraphResponseDTO.PositionDTO(100.0, 200.0);
            if (configMap.get("position") instanceof Map<?, ?> posMap) {
                Object xObj = posMap.get("x");
                Object yObj = posMap.get("y");
                Double x = xObj instanceof Number ? ((Number) xObj).doubleValue() : 100.0;
                Double y = yObj instanceof Number ? ((Number) yObj).doubleValue() : 200.0;
                position = new WorkflowGraphResponseDTO.PositionDTO(x, y);
            }

            // Chuẩn hóa type về dạng lowercase frontend (start, approval, review, assignment, notification, system_action, end)
            String frontendType = mapNodeTypeToFrontend(node.getType());

            Integer formId = null;
            if (node.getForm() != null) {
                formId = node.getForm().getId();
            } else if (configMap.get("formBinding") instanceof Map<?, ?> fbMap) {
                Object fidObj = fbMap.get("formId");
                if (fidObj instanceof Number) {
                    formId = ((Number) fidObj).intValue();
                } else if (fidObj instanceof String str && !str.trim().isEmpty()) {
                    try {
                        formId = Integer.parseInt(str.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            Object formBinding = configMap.get("formBinding");
            if (formBinding == null && node.getForm() != null) {
                formBinding = Map.of(
                        "formId", node.getForm().getId(),
                        "formName", node.getForm().getName(),
                        "fieldPermissions", Map.of()
                );
            }

            nodeDetailDTOs.add(WorkflowGraphResponseDTO.NodeDetailDTO.builder()
                    .id(clientId)
                    .type(frontendType)
                    .name(node.getName())
                    .description(description)
                    .position(position)
                    .config(configMap)
                    .formId(formId)
                    .formBinding(formBinding)
                    .build());
        }

        List<WorkflowGraphResponseDTO.EdgeDetailDTO> edgeDetailDTOs = new ArrayList<>();
        for (Transition transition : dbTransitions) {
            Map<String, Object> conditionMap = new HashMap<>();
            if (transition.getConditions() != null && !transition.getConditions().trim().isEmpty()) {
                try {
                    conditionMap = objectMapper.readValue(transition.getConditions(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception ignored) {
                }
            }

            String edgeId = (String) conditionMap.getOrDefault("clientEdgeId", "edge-" + transition.getId());
            String label = transition.getLabel() != null ? transition.getLabel() : (String) conditionMap.getOrDefault("label", "");
            String conditionExpression = (String) conditionMap.getOrDefault("expression", conditionMap.getOrDefault("conditionExpression", ""));
            String matchType = (String) conditionMap.getOrDefault("matchType", "ALWAYS");
            String branchType = (String) conditionMap.get("branchType");
            Object conditions = conditionMap.containsKey("rules") ? conditionMap.get("rules") : conditionMap.get("conditions");

            String fromClientId = nodeDbIdToClientIdMap.get(transition.getSourceNode().getId());
            String toClientId = nodeDbIdToClientIdMap.get(transition.getTargetNode().getId());

            if (fromClientId != null && toClientId != null) {
                edgeDetailDTOs.add(WorkflowGraphResponseDTO.EdgeDetailDTO.builder()
                        .id(edgeId)
                        .fromNodeId(fromClientId)
                        .toNodeId(toClientId)
                        .label(label)
                        .conditionExpression(conditionExpression)
                        .matchType(matchType)
                        .conditions(conditions)
                        .branchType(branchType)
                        .build());
            }
        }

        return WorkflowGraphResponseDTO.builder()
                .workflowId(workflow.getId())
                .workflowName(workflow.getName())
                .workflowCode(String.format("WF-%03d", workflow.getId()))
                .status(workflow.getStatus().name())
                .nodes(nodeDetailDTOs)
                .edges(edgeDetailDTOs)
                .build();
    }

    private NodeType parseNodeType(String typeStr) {
        if (typeStr == null) return NodeType.Start;
        String normalized = typeStr.trim().toLowerCase();
        return switch (normalized) {
            case "start" -> NodeType.Start;
            case "forminput", "form_input" -> NodeType.FormInput;
            case "approval" -> NodeType.Approval;
            case "review" -> NodeType.Review;
            case "assignment" -> NodeType.Assignment;
            case "notification" -> NodeType.Notification;
            case "systemaction", "system_action" -> NodeType.SystemAction;
            case "end" -> NodeType.End;
            default -> NodeType.Start;
        };
    }

    private String mapNodeTypeToFrontend(NodeType type) {
        if (type == null) return "start";
        return switch (type) {
            case Start -> "start";
            case FormInput -> "form_input";
            case Approval -> "approval";
            case Review -> "review";
            case Assignment -> "assignment";
            case Notification -> "notification";
            case SystemAction -> "system_action";
            case End -> "end";
        };
    }

    private String generateConditionExpression(Object conditionsObj) {
        if (!(conditionsObj instanceof List<?> list) || list.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> ruleMap)) {
                continue;
            }

            String fieldKey = (String) ruleMap.get("fieldKey");
            String operator = (String) ruleMap.get("operator");
            Object compareValue = ruleMap.get("compareValue");
            String dataType = (String) ruleMap.get("dataType");
            String logicOp = (String) ruleMap.get("logicOp");

            if (fieldKey == null || fieldKey.trim().isEmpty()) {
                continue;
            }

            String op = "==";
            if ("NOT_EQUALS".equals(operator)) op = "!=";
            else if ("GREATER_THAN".equals(operator)) op = ">";
            else if ("GREATER_THAN_OR_EQUAL".equals(operator)) op = ">=";
            else if ("LESS_THAN".equals(operator)) op = "<";
            else if ("LESS_THAN_OR_EQUAL".equals(operator)) op = "<=";
            else if ("CONTAINS".equals(operator)) op = "contains";

            String formattedVal;
            if ("NUMBER".equalsIgnoreCase(dataType) || "BOOLEAN".equalsIgnoreCase(dataType)) {
                formattedVal = compareValue != null ? compareValue.toString() : "null";
            } else {
                formattedVal = "'" + (compareValue != null ? compareValue.toString() : "") + "'";
            }

            String ruleExpr = "CONTAINS".equals(operator)
                    ? fieldKey + " contains " + formattedVal
                    : fieldKey + " " + op + " " + formattedVal;

            if (sb.length() == 0) {
                sb.append(ruleExpr);
            } else {
                String connector = "OR".equalsIgnoreCase(logicOp) ? " || " : " && ";
                sb.append(connector).append(ruleExpr);
            }
        }
        return sb.toString();
    }

    private WorkflowResponseDTO mapToResponseDTO(Workflow workflow) {
        long activeInstances = workflowInstanceRepository.countByWorkflowIdAndStatus(workflow.getId(), WorkflowInstanceStatus.Running);
        long totalInstances = workflowInstanceRepository.countByWorkflowId(workflow.getId());

        WorkflowResponseDTO.OwnerInfo ownerInfo = null;
        if (workflow.getOwner() != null) {
            ownerInfo = WorkflowResponseDTO.OwnerInfo.builder()
                    .id(workflow.getOwner().getId())
                    .fullName(workflow.getOwner().getFullName())
                    .email(workflow.getOwner().getEmail())
                    .build();
        }

        return WorkflowResponseDTO.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .owner(ownerInfo)
                .status(workflow.getStatus())
                .activeInstances(activeInstances)
                .totalInstances(totalInstances)
                .createdAt(workflow.getCreatedAt())
                .deletedAt(workflow.getDeletedAt())
                .build();
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}

