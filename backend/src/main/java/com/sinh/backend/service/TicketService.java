package com.sinh.backend.service;

import com.sinh.backend.dto.request.CreateTicketRequest;
import com.sinh.backend.dto.request.TaskActionRequest;
import com.sinh.backend.dto.request.TicketFilterRequest;
import com.sinh.backend.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TicketService {

    TicketResponseDTO createTicket(CreateTicketRequest request);

    PageResponse<TicketResponseDTO> getAllTickets(TicketFilterRequest filter, Pageable pageable);

    TicketDetailDTO getTicketById(Integer id);

    TicketDetailDTO processTaskAction(Integer taskId, TaskActionRequest request);

    List<TaskSummaryDTO> getMyPendingTasks(Integer userId);

    TicketStatsDTO getTicketStats(Integer userId);
}
