package com.sinh.backend.repository;

import com.sinh.backend.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, Integer> {
    List<WorkflowVersion> findByWorkflowId(Integer workflowId);
    void deleteByWorkflowId(Integer workflowId);
}
