package com.sinh.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "form_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "form")
@EqualsAndHashCode(exclude = "form")
public class FormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private Form form;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 255)
    private String placeholder;

    @Column(name = "help_text", length = 255)
    private String helpText;

    @Column(name = "required")
    @Builder.Default
    private Boolean required = false;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "min_val")
    private Integer minVal;

    @Column(name = "max_val")
    private Integer maxVal;

    @Column(name = "options", columnDefinition = "json")
    private String options;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
