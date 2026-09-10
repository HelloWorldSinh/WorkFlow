package com.sinh.backend.repository;

import com.sinh.backend.entity.Transition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransitionRepository extends JpaRepository<Transition, Integer> {
    List<Transition> findByWorkflowId(Integer workflowId);
    void deleteByWorkflowId(Integer workflowId);
}
