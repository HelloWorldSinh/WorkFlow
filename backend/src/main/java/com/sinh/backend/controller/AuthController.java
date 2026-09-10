package com.sinh.backend.controller;

import com.sinh.backend.dto.request.LoginRequest;
import com.sinh.backend.dto.response.ApiResponse;
import com.sinh.backend.dto.response.LoginResponseDTO;
import com.sinh.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * API Đăng nhập hệ thống bằng Email
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    /**
     * API Lấy thông tin tài khoản người dùng theo ID
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> getUserProfile(@PathVariable Integer id) {
        LoginResponseDTO response = authService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin người dùng thành công"));
    }
}
