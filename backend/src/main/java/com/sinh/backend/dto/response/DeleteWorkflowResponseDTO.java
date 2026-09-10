package com.sinh.backend.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteWorkflowResponseDTO {

    private Integer workflowId;
    private String deleteType;
    private String message;
}
