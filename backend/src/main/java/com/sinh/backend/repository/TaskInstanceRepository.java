package com.sinh.backend.repository;

import com.sinh.backend.entity.TaskInstance;
import com.sinh.backend.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskInstanceRepository extends JpaRepository<TaskInstance, Integer> {

    List<TaskInstance> findByWorkflowInstanceIdOrderByIdAsc(Integer workflowInstanceId);

    @Query("SELECT t FROM TaskInstance t " +
           "JOIN FETCH t.workflowInstance wi " +
           "JOIN FETCH t.node n " +
           "WHERE t.assignedUser.id = :userId AND t.status = :status " +
           "ORDER BY t.id DESC")
    List<TaskInstance> findByAssignedUserIdAndStatus(
            @Param("userId") Integer userId,
            @Param("status") TaskStatus status
    );

    long countByAssignedUserIdAndStatus(Integer userId, TaskStatus status);

    Optional<TaskInstance> findFirstByWorkflowInstanceIdAndStatusOrderByIdDesc(
            Integer workflowInstanceId,
            TaskStatus status
    );
}
