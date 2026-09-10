package com.sinh.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsDTO {

    private long total;

    private long running;

    private long completed;

    private long rejected;

    private long pendingMyTasks;
}
