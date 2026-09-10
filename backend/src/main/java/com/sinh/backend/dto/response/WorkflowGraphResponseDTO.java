package com.sinh.backend.dto.response;

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
public class WorkflowGraphResponseDTO {

    private Integer workflowId;
    private String workflowName;
    private String workflowCode;
    private String status;
    private List<NodeDetailDTO> nodes;
    private List<EdgeDetailDTO> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDetailDTO {
        private String id;
        private String type;
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
    public static class EdgeDetailDTO {
        private String id;
        private String fromNodeId;
        private String toNodeId;
        private String label;
        private String conditionExpression;
        private String matchType;
        private Object conditions;
        private String branchType;
    }
}
