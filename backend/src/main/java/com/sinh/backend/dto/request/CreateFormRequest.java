package com.sinh.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFormRequest {

    @NotBlank(message = "Tên biểu mẫu không được để trống")
    @Size(max = 255, message = "Tên biểu mẫu không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Cấu trúc trường biểu mẫu (schema) không được để trống")
    private Map<String, Object> schema;

    private Integer createdById;
}
