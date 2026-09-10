package com.sinh.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id")
    private Node sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id")
    private Node targetNode;

    @Column(name = "label")
    private String label;

    @Column(name = "conditions", columnDefinition = "json")
    private String conditions;
}
