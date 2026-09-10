package com.sinh.backend.repository;

import com.sinh.backend.entity.ChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeLogRepository extends JpaRepository<ChangeLog, Integer> {

    List<ChangeLog> findByWorkflowIdOrderByUpdatedAtDesc(Integer workflowId);
}
