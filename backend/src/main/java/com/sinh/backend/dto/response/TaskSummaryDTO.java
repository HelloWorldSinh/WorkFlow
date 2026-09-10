package com.sinh.backend.dto.response;

import com.sinh.backend.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryDTO {

    private Integer taskId;

    private Integer ticketId;

    private String requestCode;

    private String ticketTitle;

    private String workflowName;

    private String nodeName;

    private String nodeType;

    private UserSummaryDTO creator;

    private TaskStatus status;

    private LocalDateTime dueDate;

    private LocalDateTime createdAt;
}
