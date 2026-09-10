package com.sinh.backend.dto.request;

import com.sinh.backend.entity.enums.WorkflowStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkflowRequest {

    @NotBlank(message = "Tên workflow không được để trống")
    @Size(max = 255, message = "Tên workflow không được vượt quá 255 ký tự")
    private String name;

    private String description;

    private Integer ownerId;

    private WorkflowStatus status;
}
