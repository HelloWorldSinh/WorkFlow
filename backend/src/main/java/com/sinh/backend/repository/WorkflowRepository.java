package com.sinh.backend.repository;

import com.sinh.backend.entity.Workflow;
import com.sinh.backend.entity.enums.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Integer> {

    Optional<Workflow> findByIdAndDeletedAtIsNull(Integer id);

    @Query("SELECT w FROM Workflow w WHERE w.deletedAt IS NULL " +
           "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR CAST(w.id AS string) LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR w.status = :status) " +
           "AND (:ownerId IS NULL OR w.owner.id = :ownerId)")
    Page<Workflow> searchWorkflows(
            @Param("keyword") String keyword,
            @Param("status") WorkflowStatus status,
            @Param("ownerId") Integer ownerId,
            Pageable pageable
    );

    @Query("SELECT w FROM Workflow w WHERE " +
           "(:includeDeleted = true OR w.deletedAt IS NULL) " +
           "AND (:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR CAST(w.id AS string) LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:status IS NULL OR w.status = :status) " +
           "AND (:ownerId IS NULL OR w.owner.id = :ownerId)")
    Page<Workflow> searchAllWorkflows(
            @Param("keyword") String keyword,
            @Param("status") WorkflowStatus status,
            @Param("ownerId") Integer ownerId,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable
    );
}
