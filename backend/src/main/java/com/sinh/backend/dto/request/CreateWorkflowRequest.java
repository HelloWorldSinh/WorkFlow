package com.sinh.backend.dto.request;

import com.sinh.backend.entity.enums.WorkflowStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorkflowRequest {

    @NotBlank(message = "Tên workflow không được để trống")
    @Size(max = 255, message = "Tên workflow không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "ID người sở hữu (ownerId) không được để trống")
    private Integer ownerId;

    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.Draft;
}
