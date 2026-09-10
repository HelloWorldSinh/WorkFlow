package com.sinh.backend.service.impl;

import com.sinh.backend.dto.response.UserSummaryDTO;
import com.sinh.backend.entity.User;
import com.sinh.backend.exception.ResourceNotFoundException;
import com.sinh.backend.repository.UserRepository;
import com.sinh.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sinh.backend.entity.enums.UserRole;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserSummaryDTO> searchUsers(String keyword) {
        return searchUsers(keyword, null);
    }

    @Override
    public List<UserSummaryDTO> searchUsers(String keyword, List<String> roles) {
        List<User> users;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        List<UserRole> roleEnums = (roles != null) ? roles.stream()
                .map(this::parseRole)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()) : Collections.emptyList();

        boolean hasRoles = !roleEnums.isEmpty();

        if (hasRoles) {
            if (hasKeyword) {
                users = userRepository.searchUsersByRoles(keyword.trim(), roleEnums);
            } else {
                users = userRepository.findByRoleIn(roleEnums);
            }
        } else {
            if (hasKeyword) {
                users = userRepository.searchUsers(keyword.trim());
            } else {
                users = userRepository.findAll();
            }
        }

        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSummaryDTO> getApprovers(String keyword) {
        List<UserRole> approverRoles = List.of(UserRole.Admin, UserRole.Approver, UserRole.WorkflowOwner);
        List<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userRepository.searchUsersByRoles(keyword.trim(), approverRoles);
        } else {
            users = userRepository.findByRoleIn(approverRoles);
        }
        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private UserRole parseRole(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) return null;
        String normalized = roleStr.trim().toLowerCase().replaceAll("[\\s_-]+", "");
        if (normalized.equals("admin")) return UserRole.Admin;
        if (normalized.equals("approval") || normalized.equals("approver")) return UserRole.Approver;
        if (normalized.equals("workflowowner")) return UserRole.WorkflowOwner;
        if (normalized.equals("editor")) return UserRole.Editor;
        if (normalized.equals("viewer")) return UserRole.Viewer;
        try {
            return UserRole.valueOf(roleStr.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public UserSummaryDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapToDTO(user);
    }

    private UserSummaryDTO mapToDTO(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .departmentId(user.getDepartmentId())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }
}
