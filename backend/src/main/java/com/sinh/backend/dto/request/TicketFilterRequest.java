package com.sinh.backend.dto.request;

import com.sinh.backend.entity.enums.WorkflowInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketFilterRequest {

    private String keyword;

    private WorkflowInstanceStatus status;

    private Integer workflowId;

    private Integer creatorId;
}
