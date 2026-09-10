package com.sinh.backend.service.impl;

import com.sinh.backend.dto.request.LoginRequest;
import com.sinh.backend.dto.response.LoginResponseDTO;
import com.sinh.backend.entity.User;
import com.sinh.backend.exception.BadRequestException;
import com.sinh.backend.exception.ResourceNotFoundException;
import com.sinh.backend.repository.UserRepository;
import com.sinh.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public LoginResponseDTO login(LoginRequest request) {
        String identifier = request.getEmail() != null ? request.getEmail().trim() : "";
        String rawPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        // 1. Tìm user theo Email
        User user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Không tìm thấy tài khoản [{}] trong cơ sở dữ liệu", identifier);
                    return new BadRequestException("Email hoặc mật khẩu không chính xác");
                });

        // 2. Kiểm tra trạng thái hoạt động của tài khoản
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Đăng nhập thất bại: Tài khoản [{}] đang bị khóa (is_active = false)", identifier);
            throw new BadRequestException("Tài khoản này đã bị khóa hoặc ngừng hoạt động");
        }

        // 3. Kiểm tra mật khẩu trực tiếp (So sánh chuỗi thuần túy)
        String dbPassword = user.getPassword() != null ? user.getPassword().trim() : "";
        if (!rawPassword.equals(dbPassword)) {
            log.warn("Đăng nhập thất bại: Mật khẩu không trùng khớp cho tài khoản [{}]", identifier);
            throw new BadRequestException("Email hoặc mật khẩu không chính xác");
        }

        // 4. Tạo token phiên đăng nhập đơn giản
        String token = "bearer-" + UUID.randomUUID().toString();

        log.info("Người dùng ({}) đăng nhập thành công", user.getEmail());

        return LoginResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .departmentId(user.getDepartmentId())
                .token(token)
                .build();
    }

    @Override
    public LoginResponseDTO getUserProfile(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        return LoginResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .departmentId(user.getDepartmentId())
                .build();
    }
}
