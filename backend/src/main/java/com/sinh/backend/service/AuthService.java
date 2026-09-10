package com.sinh.backend.service;

import com.sinh.backend.dto.request.LoginRequest;
import com.sinh.backend.dto.response.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequest request);
    LoginResponseDTO getUserProfile(Integer userId);
}
