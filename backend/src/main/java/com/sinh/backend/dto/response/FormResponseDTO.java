package com.sinh.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormResponseDTO {

    private Integer id;
    private String name;
    private String description;
    private Map<String, Object> schema;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private Boolean isActive;
}
