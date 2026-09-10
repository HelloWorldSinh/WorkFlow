package com.sinh.backend.dto.response;

import com.sinh.backend.entity.enums.WorkflowStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowResponseDTO {

    private Integer id;
    private String name;
    private String description;
    private OwnerInfo owner;
    private WorkflowStatus status;
    private Long activeInstances;
    private Long totalInstances;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnerInfo {
        private Integer id;
        private String fullName;
        private String email;
    }
}
