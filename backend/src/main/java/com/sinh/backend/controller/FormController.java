package com.sinh.backend.controller;

import com.sinh.backend.dto.request.CreateFormRequest;
import com.sinh.backend.dto.request.UpdateFormRequest;
import com.sinh.backend.dto.response.ApiResponse;
import com.sinh.backend.dto.response.FormResponseDTO;
import com.sinh.backend.service.FormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class FormController {

    private final FormService formService;

    /**
     * Lấy toàn bộ danh sách biểu mẫu đang hoạt động (hỗ trợ tìm kiếm theo keyword)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FormResponseDTO>>> getAllForms(
            @RequestParam(required = false) String keyword) {
        List<FormResponseDTO> forms = formService.getAllForms(keyword);
        return ResponseEntity.ok(ApiResponse.success(forms, "Lấy danh sách biểu mẫu thành công"));
    }

    /**
     * Xem chi tiết 1 biểu mẫu theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FormResponseDTO>> getFormById(@PathVariable Integer id) {
        FormResponseDTO form = formService.getFormById(id);
        return ResponseEntity.ok(ApiResponse.success(form, "Lấy thông tin biểu mẫu thành công"));
    }

    /**
     * Tạo mới biểu mẫu
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FormResponseDTO>> createForm(@Valid @RequestBody CreateFormRequest request) {
        FormResponseDTO createdForm = formService.createForm(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdForm, "Tạo mới biểu mẫu thành công"));
    }

    /**
     * Cập nhật thông tin biểu mẫu
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FormResponseDTO>> updateForm(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateFormRequest request) {
        FormResponseDTO updatedForm = formService.updateForm(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedForm, "Cập nhật biểu mẫu thành công"));
    }

    /**
     * Xóa biểu mẫu (xóa mềm isActive = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable Integer id) {
        formService.deleteForm(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa biểu mẫu thành công"));
    }
}
