package com.sinh.backend.dto.response;

import com.sinh.backend.entity.enums.WorkflowInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDTO {

    private Integer id;

    private String requestCode;

    private Integer workflowId;

    private String workflowName;

    private String workflowCode;

    private String title;

    private UserSummaryDTO creator;

    private WorkflowInstanceStatus status;

    private String currentNodeName;

    private String currentNodeType;

    private UserSummaryDTO currentAssignee;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
