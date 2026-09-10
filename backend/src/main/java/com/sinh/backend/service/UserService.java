package com.sinh.backend.service;

import com.sinh.backend.dto.response.UserSummaryDTO;

import java.util.List;

public interface UserService {
    List<UserSummaryDTO> searchUsers(String keyword);
    List<UserSummaryDTO> searchUsers(String keyword, List<String> roles);
    List<UserSummaryDTO> getApprovers(String keyword);
    UserSummaryDTO getUserById(Integer id);
}
