package com.sinh.backend.dto.response;

import com.sinh.backend.entity.enums.TaskStatus;
import com.sinh.backend.entity.enums.WorkflowInstanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailDTO {

    private Integer id;

    private String requestCode;

    private Integer workflowId;

    private String workflowName;

    private String workflowCode;

    private String title;

    private UserSummaryDTO creator;

    private WorkflowInstanceStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer formId;

    private String formName;

    private Map<String, Object> variables;

    private String activeNodeClientId;

    private List<String> completedNodeClientIds;

    private List<TaskDetailDTO> taskHistory;

    private WorkflowGraphResponseDTO graph;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDetailDTO {
        private Integer id;
        private Integer nodeId;
        private String clientNodeId;
        private String nodeName;
        private String nodeType;
        private UserSummaryDTO assignedUser;
        private TaskStatus status;
        private LocalDateTime dueDate;
        private LocalDateTime completedAt;
        private String comment;
        private Map<String, Object> submittedData;
    }
}
