package com.sinh.backend.repository;

import com.sinh.backend.entity.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NodeRepository extends JpaRepository<Node, Integer> {
    List<Node> findByWorkflowId(Integer workflowId);

    void deleteByWorkflowId(Integer workflowId);

    long countByFormId(Integer formId);
}
