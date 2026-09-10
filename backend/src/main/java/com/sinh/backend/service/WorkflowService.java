package com.sinh.backend.service;

import com.sinh.backend.dto.request.CreateWorkflowRequest;
import com.sinh.backend.dto.request.UpdateWorkflowRequest;
import com.sinh.backend.dto.request.WorkflowFilterRequest;
import com.sinh.backend.dto.response.DeleteWorkflowResponseDTO;
import com.sinh.backend.dto.response.PageResponse;
import com.sinh.backend.dto.response.WorkflowGraphResponseDTO;
import com.sinh.backend.dto.response.WorkflowResponseDTO;
import org.springframework.data.domain.Pageable;

public interface WorkflowService {

    PageResponse<WorkflowResponseDTO> getAllWorkflows(WorkflowFilterRequest filter, Pageable pageable);

    WorkflowResponseDTO getWorkflowById(Integer id);

    WorkflowResponseDTO createWorkflow(CreateWorkflowRequest request);

    WorkflowResponseDTO updateWorkflow(Integer id, UpdateWorkflowRequest request);

    DeleteWorkflowResponseDTO deleteWorkflow(Integer id);

    WorkflowResponseDTO restoreWorkflow(Integer id);

    WorkflowGraphResponseDTO saveWorkflowGraph(Integer id,
            com.sinh.backend.dto.request.SaveWorkflowGraphRequest request);

    WorkflowGraphResponseDTO getWorkflowGraph(Integer id);
}
