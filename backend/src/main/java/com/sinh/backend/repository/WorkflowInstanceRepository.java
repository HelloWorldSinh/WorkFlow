package com.sinh.backend.repository;

import com.sinh.backend.entity.WorkflowInstance;
import com.sinh.backend.entity.enums.WorkflowInstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Integer> {

    boolean existsByWorkflowId(Integer workflowId);

    boolean existsByWorkflowIdAndStatus(Integer workflowId, WorkflowInstanceStatus status);

    long countByWorkflowId(Integer workflowId);

    long countByWorkflowIdAndStatus(Integer workflowId, WorkflowInstanceStatus status);

    long countByStatus(WorkflowInstanceStatus status);

    @Query("SELECT wi FROM WorkflowInstance wi " +
           "JOIN FETCH wi.workflow w " +
           "LEFT JOIN FETCH wi.creator c " +
           "WHERE (:status IS NULL OR wi.status = :status) " +
           "AND (:workflowId IS NULL OR w.id = :workflowId) " +
           "AND (:creatorId IS NULL OR c.id = :creatorId) " +
           "AND (:keyword IS NULL OR LOWER(wi.requestCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY wi.id DESC")
    Page<WorkflowInstance> searchInstances(
            @Param("keyword") String keyword,
            @Param("status") WorkflowInstanceStatus status,
            @Param("workflowId") Integer workflowId,
            @Param("creatorId") Integer creatorId,
            Pageable pageable
    );

    List<WorkflowInstance> findByWorkflowIdOrderByIdDesc(Integer workflowId);
}
