package com.sinh.backend.dto.response;

import com.sinh.backend.entity.enums.UserRole;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private Integer id;
    private String email;
    private String fullName;
    private UserRole role;
    private Integer departmentId;
    private String token;
}
