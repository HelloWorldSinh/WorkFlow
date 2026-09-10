package com.sinh.backend.entity;

import com.sinh.backend.entity.enums.NodeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NodeType type;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "config", columnDefinition = "json")
    private String config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    private Form form;
}
