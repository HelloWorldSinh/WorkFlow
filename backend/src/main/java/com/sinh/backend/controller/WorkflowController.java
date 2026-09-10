package com.sinh.backend.controller;

import com.sinh.backend.dto.request.CreateWorkflowRequest;
import com.sinh.backend.dto.request.UpdateWorkflowRequest;
import com.sinh.backend.dto.request.WorkflowFilterRequest;
import com.sinh.backend.dto.response.ApiResponse;
import com.sinh.backend.dto.response.DeleteWorkflowResponseDTO;
import com.sinh.backend.dto.response.PageResponse;
import com.sinh.backend.dto.response.WorkflowGraphResponseDTO;
import com.sinh.backend.dto.response.WorkflowResponseDTO;
import com.sinh.backend.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * Lấy danh sách workflow, mặc định page = 0, size = 10,
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponseDTO>>> getAllWorkflows(
            @ModelAttribute WorkflowFilterRequest filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<WorkflowResponseDTO> result = workflowService.getAllWorkflows(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách workflow thành công"));
    }

    /**
     * Xem chi tiết thông tin 1 workflow theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> getWorkflowById(@PathVariable Integer id) {
        WorkflowResponseDTO workflow = workflowService.getWorkflowById(id);
        return ResponseEntity.ok(ApiResponse.success(workflow, "Lấy thông tin workflow thành công"));
    }

    /**
     * Tạo mới một workflow
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowResponseDTO createdWorkflow = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdWorkflow, "Tạo mới workflow thành công"));
    }

    /**
     * Chỉnh sửa thông tin workflow
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> updateWorkflow(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateWorkflowRequest request) {
        WorkflowResponseDTO updatedWorkflow = workflowService.updateWorkflow(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedWorkflow, "Cập nhật workflow thành công"));
    }

    /**
     * Xóa workflow:xoá mềm/cứng tuỳ theo đã có instance?
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DeleteWorkflowResponseDTO>> deleteWorkflow(@PathVariable Integer id) {
        DeleteWorkflowResponseDTO result = workflowService.deleteWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    /**
     * Khôi phục (Restore) workflow đã xóa mềm
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<WorkflowResponseDTO>> restoreWorkflow(@PathVariable Integer id) {
        WorkflowResponseDTO restored = workflowService.restoreWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success(restored, "Khôi phục workflow thành công"));
    }

    /**
     * Lấy toàn bộ sơ đồ (Nodes & Transitions) của Workflow
     */
    @GetMapping("/{id}/graph")
    public ResponseEntity<ApiResponse<WorkflowGraphResponseDTO>> getWorkflowGraph(@PathVariable Integer id) {
        com.sinh.backend.dto.response.WorkflowGraphResponseDTO graph = workflowService.getWorkflowGraph(id);
        return ResponseEntity.ok(ApiResponse.success(graph, "Lấy sơ đồ workflow thành công"));
    }

    /**
     * Lưu toàn bộ sơ đồ (Nodes & Transitions) của Workflow xuống Database
     */
    @PostMapping("/{id}/graph")
    public ResponseEntity<ApiResponse<WorkflowGraphResponseDTO>> saveWorkflowGraph(
            @PathVariable Integer id,
            @RequestBody com.sinh.backend.dto.request.SaveWorkflowGraphRequest request) {
        com.sinh.backend.dto.response.WorkflowGraphResponseDTO savedGraph = workflowService.saveWorkflowGraph(id,
                request);
        return ResponseEntity.ok(ApiResponse.success(savedGraph, "Lưu sơ đồ quy trình thành công"));
    }
}
