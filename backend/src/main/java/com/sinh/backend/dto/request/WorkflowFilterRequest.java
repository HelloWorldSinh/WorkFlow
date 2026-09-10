package com.sinh.backend.dto.request;

import com.sinh.backend.entity.enums.WorkflowStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowFilterRequest {

    private String keyword;
    private WorkflowStatus status;
    private Integer ownerId;

    @Builder.Default
    private Boolean includeDeleted = false;
}
