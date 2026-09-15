package com.sinh.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveWorkflowGraphRequest {

    private String name;
    private String description;
    private String status; // "Draft" hoặc "Published"
    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDTO {
        private String id; // vd: "node-start", "node-approval-1234"
        private String type; // start, approval, review, assignment, notification, system_action, end
        private String name;
        private String description;
        private PositionDTO position;
        private Map<String, Object> config;
        private Integer formId;
        private Object formBinding;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PositionDTO {
        private Double x;
        private Double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EdgeDTO {
        private String id;
        private String fromNodeId;
        private String toNodeId;
        private String label;
        private Integer priority;
        private String conditionExpression;
        private String matchType;
        private Object conditions;
        private String branchType; // "approved", "rejected", "default"
    }
}
