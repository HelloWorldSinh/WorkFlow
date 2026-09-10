package com.sinh.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskActionRequest {

    /**
     * Hành động xử lý: "APPROVE" (Phê duyệt) hoặc "REJECT" (Từ chối)
     */
    @NotBlank(message = "Hành động (action: APPROVE / REJECT) không được để trống")
    private String action;

    /**
     * Ý kiến, nhận xét hoặc lý do phê duyệt/từ chối
     */
    private String comment;

    /**
     * ID người thực hiện hành động
     */
    private Integer userId;

    /**
     * Dữ liệu form bổ sung (nếu có)
     */
    private Map<String, Object> submittedData;
}
