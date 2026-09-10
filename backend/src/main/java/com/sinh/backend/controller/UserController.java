package com.sinh.backend.controller;

import com.sinh.backend.dto.response.ApiResponse;
import com.sinh.backend.dto.response.UserSummaryDTO;
import com.sinh.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Tìm kiếm danh sách người dùng theo tên hoặc email, hỗ trợ lọc theo roles
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) List<String> roles) {
        List<UserSummaryDTO> users = userService.searchUsers(keyword, roles);
        return ResponseEntity.ok(ApiResponse.success(users, "Tìm kiếm người dùng thành công"));
    }

    /**
     * Lấy danh sách người dùng có vai trò phê duyệt (Admin, Approver, WorkflowOwner)
     */
    @GetMapping("/approvers")
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getApprovers(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<UserSummaryDTO> users = userService.getApprovers(keyword);
        return ResponseEntity.ok(ApiResponse.success(users, "Lấy danh sách người phê duyệt thành công"));
    }

    /**
     * Lấy thông tin chi tiết một người dùng theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSummaryDTO>> getUserById(@PathVariable Integer id) {
        UserSummaryDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "Lấy thông tin người dùng thành công"));
    }
}
