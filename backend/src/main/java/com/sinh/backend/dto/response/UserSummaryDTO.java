package com.sinh.backend.dto.response;

import com.sinh.backend.entity.enums.UserRole;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {
    private Integer id;
    private String email;
    private String fullName;
    private Integer departmentId;
    private UserRole role;
    private Boolean isActive;
}
