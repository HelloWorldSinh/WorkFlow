package com.sinh.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotNull(message = "ID của quy trình (workflowId) không được để trống")
    private Integer workflowId;

    private String title;

    private Integer creatorId;

    private Map<String, Object> formData;
}
