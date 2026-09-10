package com.sinh.backend.controller;

import com.sinh.backend.dto.request.CreateTicketRequest;
import com.sinh.backend.dto.request.TaskActionRequest;
import com.sinh.backend.dto.request.TicketFilterRequest;
import com.sinh.backend.dto.response.*;
import com.sinh.backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * Khởi tạo một Ticket mới từ quy trình và dữ liệu biểu mẫu
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponseDTO>> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        TicketResponseDTO result = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Khởi tạo yêu cầu (Ticket) thành công"));
    }

    /**
     * Lấy danh sách Ticket (Instances) có hỗ trợ tìm kiếm, lọc trạng thái, phân trang
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TicketResponseDTO>>> getAllTickets(
            @ModelAttribute TicketFilterRequest filter,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TicketResponseDTO> result = ticketService.getAllTickets(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách ticket thành công"));
    }

    /**
     * Xem chi tiết 1 Ticket kèm toàn bộ tiến trình, form variables, lịch sử task và sơ đồ graph
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailDTO>> getTicketById(@PathVariable Integer id) {
        TicketDetailDTO result = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy thông tin chi tiết ticket thành công"));
    }

    /**
     * Thực hiện phê duyệt (Approve) hoặc từ chối (Reject) một nhiệm vụ trong Ticket
     */
    @PostMapping("/tasks/{taskId}/action")
    public ResponseEntity<ApiResponse<TicketDetailDTO>> processTaskAction(
            @PathVariable Integer taskId,
            @Valid @RequestBody TaskActionRequest request) {
        TicketDetailDTO result = ticketService.processTaskAction(taskId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Xử lý nhiệm vụ thành công"));
    }

    /**
     * Lấy danh sách các nhiệm vụ đang chờ phê duyệt của người dùng (My Tasks)
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<TaskSummaryDTO>>> getMyPendingTasks(
            @RequestParam(required = false) Integer userId) {
        List<TaskSummaryDTO> result = ticketService.getMyPendingTasks(userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách nhiệm vụ chờ duyệt thành công"));
    }

    /**
     * Lấy số liệu thống kê tổng quan của các Ticket và công việc
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TicketStatsDTO>> getTicketStats(
            @RequestParam(required = false) Integer userId) {
        TicketStatsDTO result = ticketService.getTicketStats(userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy số liệu thống kê ticket thành công"));
    }
}
