package com.sinh.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinh.backend.dto.request.CreateTicketRequest;
import com.sinh.backend.dto.request.TaskActionRequest;
import com.sinh.backend.dto.request.TicketFilterRequest;
import com.sinh.backend.dto.response.*;
import com.sinh.backend.entity.*;
import com.sinh.backend.entity.enums.NodeType;
import com.sinh.backend.entity.enums.UserRole;
import com.sinh.backend.entity.enums.TaskStatus;
import com.sinh.backend.entity.enums.WorkflowInstanceStatus;
import com.sinh.backend.entity.enums.WorkflowStatus;
import com.sinh.backend.exception.BadRequestException;
import com.sinh.backend.exception.ResourceNotFoundException;
import com.sinh.backend.repository.*;
import com.sinh.backend.service.TicketService;
import com.sinh.backend.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketServiceImpl implements TicketService {

    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final TaskInstanceRepository taskInstanceRepository;
    private final WorkflowRepository workflowRepository;
    private final NodeRepository nodeRepository;
    private final TransitionRepository transitionRepository;
    private final UserRepository userRepository;
    private final ChangeLogRepository changeLogRepository;
    private final WorkflowService workflowService;
    private final ObjectMapper objectMapper;

    @Override
    public TicketResponseDTO createTicket(CreateTicketRequest request) {
        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy trình với ID: " + request.getWorkflowId()));

        if (workflow.getDeletedAt() != null || workflow.getStatus() == WorkflowStatus.Deleted) {
            throw new BadRequestException("Quy trình này đã bị xóa, không thể khởi tạo yêu cầu mới.");
        }

        // Người tạo yêu cầu
        User creator = null;
        if (request.getCreatorId() != null) {
            creator = userRepository.findById(request.getCreatorId()).orElse(null);
        }
        if (creator == null && workflow.getOwner() != null) {
            creator = workflow.getOwner();
        }
        if (creator == null) {
            creator = userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new BadRequestException("Không có người dùng nào trong hệ thống để gán người tạo"));
        }

        // Lấy danh sách nodes và transitions của quy trình
        List<Node> nodes = nodeRepository.findByWorkflowId(workflow.getId());
        if (nodes.isEmpty()) {
            throw new BadRequestException("Quy trình chưa được cấu hình các bước (nodes), vui lòng thiết kế sơ đồ trước khi chạy.");
        }

        Node startNode = nodes.stream()
                .filter(n -> n.getType() == NodeType.Start)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Quy trình chưa có bước 'Bắt đầu (Start)'"));

        // Dữ liệu biểu mẫu ban đầu
        Map<String, Object> variables = request.getFormData() != null ? new HashMap<>(request.getFormData()) : new HashMap<>();
        String ticketTitle = request.getTitle();
        if (ticketTitle == null || ticketTitle.trim().isEmpty()) {
            if (variables.containsKey("title") && variables.get("title") != null) {
                ticketTitle = String.valueOf(variables.get("title"));
            } else {
                ticketTitle = "Yêu cầu: " + workflow.getName();
            }
        }
        variables.put("__ticket_title", ticketTitle);

        // Sinh mã yêu cầu tự động: TK-YYYYMM-XXXX
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long instanceCount = workflowInstanceRepository.count() + 1;
        String requestCode = String.format("TK-%s-%04d", datePrefix, instanceCount);

        String variablesJson = "{}";
        try {
            variablesJson = objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            log.error("Lỗi serialize variables ticket: {}", e.getMessage());
        }

        WorkflowInstance instance = WorkflowInstance.builder()
                .workflow(workflow)
                .requestCode(requestCode)
                .creator(creator)
                .status(WorkflowInstanceStatus.Running)
                .variables(variablesJson)
                .build();

        WorkflowInstance savedInstance = workflowInstanceRepository.save(instance);
        log.info("Đã tạo mới Ticket Instance ID: {}, Mã: {}", savedInstance.getId(), requestCode);

        // Ghi nhận Audit Log
        recordChangeLog(workflow, creator, "CREATE_TICKET", Map.of(
                "ticketId", savedInstance.getId(),
                "requestCode", requestCode,
                "title", ticketTitle
        ));

        // Tiến hành điều phối luồng từ Start node
        advanceFromNode(savedInstance, startNode, variables, "INITIAL_START");

        return mapToTicketResponseDTO(savedInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponseDTO> getAllTickets(TicketFilterRequest filter, Pageable pageable) {
        if (filter == null) {
            filter = new TicketFilterRequest();
        }

        String keyword = null;
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            keyword = filter.getKeyword().trim();
        }

        Page<WorkflowInstance> page = workflowInstanceRepository.searchInstances(
                keyword,
                filter.getStatus(),
                filter.getWorkflowId(),
                filter.getCreatorId(),
                pageable
        );

        return PageResponse.of(page.map(this::mapToTicketResponseDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailDTO getTicketById(Integer id) {
        WorkflowInstance instance = workflowInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Ticket với ID: " + id));

        Workflow workflow = instance.getWorkflow();
        WorkflowGraphResponseDTO graph = workflowService.getWorkflowGraph(workflow.getId());

        Map<String, Object> variables = new HashMap<>();
        if (instance.getVariables() != null && !instance.getVariables().trim().isEmpty()) {
            try {
                variables = objectMapper.readValue(instance.getVariables(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
            }
        }

        // Lấy lịch sử task
        List<TaskInstance> tasks = taskInstanceRepository.findByWorkflowInstanceIdOrderByIdAsc(instance.getId());
        List<TicketDetailDTO.TaskDetailDTO> taskDTOs = new ArrayList<>();
        List<String> completedNodeClientIds = new ArrayList<>();
        String activeNodeClientId = null;

        // Start node luôn được xem là completed nếu instance đã tạo
        graph.getNodes().stream()
                .filter(n -> "start".equalsIgnoreCase(n.getType()))
                .findFirst()
                .ifPresent(sn -> completedNodeClientIds.add(sn.getId()));

        for (TaskInstance t : tasks) {
            Map<String, Object> nodeConfig = parseJsonMap(t.getNode().getConfig());
            String clientNodeId = (String) nodeConfig.getOrDefault("clientNodeId", "node-" + t.getNode().getId());

            Map<String, Object> stepData = parseJsonMap(t.getStepSubmittedData());
            String comment = (String) stepData.getOrDefault("comment", "");

            UserSummaryDTO assignedUserDTO = null;
            if (t.getAssignedUser() != null) {
                assignedUserDTO = mapToUserSummaryDTO(t.getAssignedUser());
            }

            taskDTOs.add(TicketDetailDTO.TaskDetailDTO.builder()
                    .id(t.getId())
                    .nodeId(t.getNode().getId())
                    .clientNodeId(clientNodeId)
                    .nodeName(t.getNode().getName())
                    .nodeType(t.getNode().getType().name())
                    .assignedUser(assignedUserDTO)
                    .status(t.getStatus())
                    .dueDate(t.getDueDate())
                    .completedAt(t.getCompletedAt())
                    .comment(comment)
                    .submittedData(stepData)
                    .build());

            if (t.getStatus() == TaskStatus.Pending) {
                activeNodeClientId = clientNodeId;
            } else {
                if (!completedNodeClientIds.contains(clientNodeId)) {
                    completedNodeClientIds.add(clientNodeId);
                }
            }
        }

        // Nếu instance đã kết thúc (Completed hoặc Rejected), thêm End node vào danh sách hoàn thành
        if (instance.getStatus() == WorkflowInstanceStatus.Completed || instance.getStatus() == WorkflowInstanceStatus.Rejected) {
            graph.getNodes().stream()
                    .filter(n -> "end".equalsIgnoreCase(n.getType()))
                    .findFirst()
                    .ifPresent(en -> {
                        completedNodeClientIds.add(en.getId());
                    });
            activeNodeClientId = null;
        }

        // Lấy thông tin Form từ Start Node
        Integer formId = null;
        String formName = null;
        Node startNode = nodeRepository.findByWorkflowId(workflow.getId()).stream()
                .filter(n -> n.getType() == NodeType.Start)
                .findFirst()
                .orElse(null);

        if (startNode != null && startNode.getForm() != null) {
            formId = startNode.getForm().getId();
            formName = startNode.getForm().getName();
        }

        String ticketTitle = (String) variables.getOrDefault("__ticket_title", "Yêu cầu " + instance.getRequestCode());

        return TicketDetailDTO.builder()
                .id(instance.getId())
                .requestCode(instance.getRequestCode())
                .workflowId(workflow.getId())
                .workflowName(workflow.getName())
                .workflowCode(String.format("WF-%03d", workflow.getId()))
                .title(ticketTitle)
                .creator(mapToUserSummaryDTO(instance.getCreator()))
                .status(instance.getStatus())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .formId(formId)
                .formName(formName)
                .variables(variables)
                .activeNodeClientId(activeNodeClientId)
                .completedNodeClientIds(completedNodeClientIds)
                .taskHistory(taskDTOs)
                .graph(graph)
                .build();
    }

    @Override
    public TicketDetailDTO processTaskAction(Integer taskId, TaskActionRequest request) {
        TaskInstance task = taskInstanceRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhiệm vụ với ID: " + taskId));

        if (task.getStatus() != TaskStatus.Pending) {
            throw new BadRequestException("Nhiệm vụ này đã được xử lý trước đó với trạng thái: " + task.getStatus());
        }

        WorkflowInstance instance = task.getWorkflowInstance();
        if (instance.getStatus() != WorkflowInstanceStatus.Running) {
            throw new BadRequestException("Yêu cầu này không ở trạng thái hoạt động (Running). Trạng thái hiện tại: " + instance.getStatus());
        }

        String action = request.getAction() != null ? request.getAction().trim().toUpperCase() : "APPROVE";
        boolean isApproved = "APPROVE".equalsIgnoreCase(action) || "APPROVED".equalsIgnoreCase(action);

        // Cập nhật trạng thái task
        task.setStatus(isApproved ? TaskStatus.Approved : TaskStatus.Rejected);
        task.setCompletedAt(LocalDateTime.now());

        Map<String, Object> submittedPayload = new HashMap<>();
        submittedPayload.put("action", action);
        submittedPayload.put("comment", request.getComment() != null ? request.getComment().trim() : "");
        if (request.getSubmittedData() != null) {
            submittedPayload.put("data", request.getSubmittedData());
        }

        try {
            task.setStepSubmittedData(objectMapper.writeValueAsString(submittedPayload));
        } catch (Exception e) {
            log.error("Lỗi serialize stepSubmittedData: {}", e.getMessage());
        }
        taskInstanceRepository.save(task);

        // Đọc variables của ticket
        Map<String, Object> variables = parseJsonMap(instance.getVariables());
        if (request.getSubmittedData() != null) {
            variables.putAll(request.getSubmittedData());
            try {
                instance.setVariables(objectMapper.writeValueAsString(variables));
                workflowInstanceRepository.save(instance);
            } catch (Exception ignored) {
            }
        }

        // Ghi nhận Audit Log
        recordChangeLog(instance.getWorkflow(), task.getAssignedUser(), isApproved ? "APPROVE_TASK" : "REJECT_TASK", Map.of(
                "ticketId", instance.getId(),
                "taskId", task.getId(),
                "stepName", task.getNode().getName(),
                "comment", request.getComment() != null ? request.getComment() : ""
        ));

        // Kiểm tra xem Node hiện tại có phải là Approval với nhiều người duyệt (Council / Multi-approvers)
        Node currentNode = task.getNode();
        Map<String, Object> nodeConfig = parseJsonMap(currentNode.getConfig());
        String approvalMode = (String) nodeConfig.getOrDefault("approvalMode", "single");

        if (currentNode.getType() == NodeType.Approval && "multi".equalsIgnoreCase(approvalMode)) {
            evaluateMultiApprovalDecision(instance, currentNode, variables);
        } else {
            // Tiến hành điều hướng bước tiếp theo cho Single node
            advanceFromNode(instance, currentNode, variables, action);
        }

        return getTicketById(instance.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskSummaryDTO> getMyPendingTasks(Integer userId) {
        if (userId == null) {
            // Mặc định lấy user đầu tiên nếu không truyền
            userId = 1;
        }

        List<TaskInstance> pendingTasks = taskInstanceRepository.findByAssignedUserIdAndStatus(userId, TaskStatus.Pending);
        List<TaskSummaryDTO> result = new ArrayList<>();

        for (TaskInstance t : pendingTasks) {
            WorkflowInstance wi = t.getWorkflowInstance();
            Map<String, Object> vars = parseJsonMap(wi.getVariables());
            String title = (String) vars.getOrDefault("__ticket_title", "Yêu cầu " + wi.getRequestCode());

            result.add(TaskSummaryDTO.builder()
                    .taskId(t.getId())
                    .ticketId(wi.getId())
                    .requestCode(wi.getRequestCode())
                    .ticketTitle(title)
                    .workflowName(wi.getWorkflow().getName())
                    .nodeName(t.getNode().getName())
                    .nodeType(t.getNode().getType().name())
                    .creator(mapToUserSummaryDTO(wi.getCreator()))
                    .status(t.getStatus())
                    .dueDate(t.getDueDate())
                    .createdAt(t.getWorkflowInstance().getStartTime())
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatsDTO getTicketStats(Integer userId) {
        long total = workflowInstanceRepository.count();
        long running = workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.Running);
        long completed = workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.Completed);
        long rejected = workflowInstanceRepository.countByStatus(WorkflowInstanceStatus.Rejected);

        long myTasksCount = 0;
        if (userId != null) {
            myTasksCount = taskInstanceRepository.countByAssignedUserIdAndStatus(userId, TaskStatus.Pending);
        }

        return TicketStatsDTO.builder()
                .total(total)
                .running(running)
                .completed(completed)
                .rejected(rejected)
                .pendingMyTasks(myTasksCount)
                .build();
    }

    // =========================================================================
    // WORKFLOW RUNTIME EXECUTION ENGINE CORE LOGIC
    // =========================================================================

    private void advanceFromNode(WorkflowInstance instance, Node fromNode, Map<String, Object> variables, String action) {
        List<Transition> outgoingTransitions = transitionRepository.findByWorkflowId(instance.getWorkflow().getId()).stream()
                .filter(t -> t.getSourceNode().getId().equals(fromNode.getId()))
                .toList();

        boolean isReject = "REJECT".equalsIgnoreCase(action) || "REJECTED".equalsIgnoreCase(action);

        if (outgoingTransitions.isEmpty()) {
            log.info("Không tìm thấy đường nối nào từ Node ID: {}. Kết thúc hoặc dừng luồng.", fromNode.getId());
            if (fromNode.getType() != NodeType.End) {
                // Nếu không phải End node mà không có đường đi:
                completeInstance(instance, isReject ? WorkflowInstanceStatus.Rejected : WorkflowInstanceStatus.Completed);
            }
            return;
        }

        // Đánh giá transition phù hợp nhất
        Transition matchedTransition = findMatchingTransition(outgoingTransitions, variables, action);
        if (matchedTransition == null) {
            log.warn("Không có transition nào thỏa mãn điều kiện từ Node ID: {}. Dừng luồng.", fromNode.getId());
            if (isReject) {
                completeInstance(instance, WorkflowInstanceStatus.Rejected);
            }
            return;
        }

        Node nextNode = matchedTransition.getTargetNode();
        log.info("Chuyển tiếp Ticket ID: {} sang Node ID: {}, Tên: {}, Loại: {}",
                instance.getId(), nextNode.getId(), nextNode.getName(), nextNode.getType());

        processNode(instance, nextNode, variables, action);
    }

    private void processNode(WorkflowInstance instance, Node node, Map<String, Object> variables, String action) {
        NodeType type = node.getType();
        boolean isReject = "REJECT".equalsIgnoreCase(action) || "REJECTED".equalsIgnoreCase(action);

        if (type == NodeType.End) {
            // Bước Kết thúc
            Map<String, Object> endConfig = parseJsonMap(node.getConfig());
            String outcome = (String) endConfig.getOrDefault("outcome", "completed");
            WorkflowInstanceStatus finalStatus = ("rejected".equalsIgnoreCase(outcome) || isReject)
                    ? WorkflowInstanceStatus.Rejected
                    : WorkflowInstanceStatus.Completed;

            completeInstance(instance, finalStatus);
            return;
        }

        if (type == NodeType.Notification) {
            // Bước thông báo tự động (ghi nhận task tự động hoàn thành & log)
            Map<String, Object> notiConfig = parseJsonMap(node.getConfig());
            String subject = (String) notiConfig.getOrDefault("subject", "Thông báo cập nhật yêu cầu " + instance.getRequestCode());
            log.info("-> [Notification Step] Gửi thông báo: '{}' cho ticket {}", subject, instance.getRequestCode());

            TaskInstance task = TaskInstance.builder()
                    .workflowInstance(instance)
                    .node(node)
                    .assignedUser(null)
                    .status(TaskStatus.Approved)
                    .dueDate(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .stepSubmittedData("{\"action\":\"AUTO\",\"comment\":\"Đã tự động gửi thông báo: " + subject.replace("\"", "\\\"") + "\"}")
                    .build();
            taskInstanceRepository.save(task);

            recordChangeLog(instance.getWorkflow(), instance.getCreator(), "SEND_NOTIFICATION", Map.of(
                    "ticketId", instance.getId(),
                    "nodeName", node.getName(),
                    "subject", subject
            ));

            // Tự động chuyển tiếp đến bước tiếp theo
            advanceFromNode(instance, node, variables, "NEXT");
            return;
        }

        if (type == NodeType.SystemAction) {
            // Bước tác vụ hệ thống (mô phỏng gọi webhook / cập nhật db)
            Map<String, Object> sysConfig = parseJsonMap(node.getConfig());
            String endpoint = (String) sysConfig.getOrDefault("endpointUrl", "local-system-action");
            log.info("-> [System Action Step] Thực thi tác vụ hệ thống: '{}' cho ticket {}", endpoint, instance.getRequestCode());

            TaskInstance task = TaskInstance.builder()
                    .workflowInstance(instance)
                    .node(node)
                    .assignedUser(null)
                    .status(TaskStatus.Approved)
                    .dueDate(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .stepSubmittedData("{\"action\":\"AUTO\",\"comment\":\"Hệ thống thực thi: " + endpoint.replace("\"", "\\\"") + "\"}")
                    .build();
            taskInstanceRepository.save(task);

            recordChangeLog(instance.getWorkflow(), instance.getCreator(), "SYSTEM_ACTION", Map.of(
                    "ticketId", instance.getId(),
                    "nodeName", node.getName(),
                    "endpoint", endpoint
            ));

            // Tự động chuyển tiếp đến bước tiếp theo
            advanceFromNode(instance, node, variables, "NEXT");
            return;
        }

        if (type == NodeType.Approval) {
            List<User> approvers = resolveApprovers(instance, node);
            LocalDateTime dueDate = calculateDueDate(node);

            for (User approver : approvers) {
                TaskInstance task = TaskInstance.builder()
                        .workflowInstance(instance)
                        .node(node)
                        .assignedUser(approver)
                        .status(TaskStatus.Pending)
                        .dueDate(dueDate)
                        .build();

                taskInstanceRepository.save(task);
                log.info("-> [Approval Node] Đã tạo Task ID: {} cho Node: {}, Gán cho: {}",
                        task.getId(), node.getName(), approver != null ? approver.getFullName() : "Unassigned");

                recordChangeLog(instance.getWorkflow(), approver, "ASSIGN_TASK", Map.of(
                        "ticketId", instance.getId(),
                        "taskId", task.getId(),
                        "nodeName", node.getName(),
                        "assigneeName", approver != null ? approver.getFullName() : "N/A"
                ));
            }
            return;
        }

        if (type == NodeType.Review || type == NodeType.Assignment || type == NodeType.FormInput) {
            // Bước cần tương tác người dùng đơn: tạo TaskInstance
            User assignee = resolveAssignee(instance, node);
            LocalDateTime dueDate = calculateDueDate(node);

            TaskInstance task = TaskInstance.builder()
                    .workflowInstance(instance)
                    .node(node)
                    .assignedUser(assignee)
                    .status(TaskStatus.Pending)
                    .dueDate(dueDate)
                    .build();

            taskInstanceRepository.save(task);
            log.info("-> Đã tạo Task ID: {} cho Node: {}, Gán cho User: {}",
                    task.getId(), node.getName(), assignee != null ? assignee.getFullName() : "Unassigned");

            recordChangeLog(instance.getWorkflow(), assignee, "ASSIGN_TASK", Map.of(
                    "ticketId", instance.getId(),
                    "taskId", task.getId(),
                    "nodeName", node.getName(),
                    "assigneeName", assignee != null ? assignee.getFullName() : "N/A"
            ));
            return;
        }
    }

    private Transition findMatchingTransition(List<Transition> transitions, Map<String, Object> variables, String action) {
        boolean isReject = "REJECT".equalsIgnoreCase(action) || "REJECTED".equalsIgnoreCase(action);

        // 1. Nếu hành động là REJECT, ưu tiên tìm transition rẽ nhánh REJECT
        if (isReject) {
            for (Transition t : transitions) {
                Map<String, Object> condMap = parseJsonMap(t.getConditions());
                String branchType = (String) condMap.get("branchType");
                String label = t.getLabel() != null ? t.getLabel().toLowerCase() : "";
                if ("rejected".equalsIgnoreCase(branchType) || label.contains("từ chối") || label.contains("reject")) {
                    return t;
                }
            }
            // Nếu là REJECT nhưng không có transition rẽ nhánh REJECT cụ thể -> trả về null để dừng luồng và kết thúc với trạng thái Rejected
            return null;
        }

        // 2. Nếu hành động không phải REJECT, kiểm tra các transition rẽ nhánh APPROVE hoặc điều kiện biểu thức
        for (Transition t : transitions) {
            Map<String, Object> condMap = parseJsonMap(t.getConditions());
            String branchType = (String) condMap.get("branchType");
            if ("rejected".equalsIgnoreCase(branchType)) {
                continue; // Bỏ qua nhánh reject khi đang duyệt approve/tiếp theo
            }

            if (evaluateConditionMap(condMap, variables, action)) {
                return t;
            }
        }

        // 3. Fallback lấy transition đầu tiên (không phải nhánh reject) nếu không có transition nào khớp cụ thể
        return transitions.stream()
                .filter(t -> {
                    Map<String, Object> condMap = parseJsonMap(t.getConditions());
                    String branchType = (String) condMap.get("branchType");
                    return !"rejected".equalsIgnoreCase(branchType);
                })
                .findFirst()
                .orElse(transitions.get(0));
    }

    private boolean evaluateConditionMap(Map<String, Object> condMap, Map<String, Object> variables, String action) {
        String matchType = (String) condMap.getOrDefault("matchType", "ALWAYS");
        if ("ALWAYS".equalsIgnoreCase(matchType)) {
            return true;
        }

        Object rulesObj = condMap.containsKey("rules") ? condMap.get("rules") : condMap.get("conditions");
        if (!(rulesObj instanceof List<?> rulesList) || rulesList.isEmpty()) {
            return true;
        }

        boolean isOr = "OR".equalsIgnoreCase(matchType);
        boolean currentResult = !isOr;

        for (Object rObj : rulesList) {
            if (!(rObj instanceof Map<?, ?> rule)) {
                continue;
            }

            String fieldKey = (String) rule.get("fieldKey");
            String operator = (String) rule.get("operator");
            Object compareValue = rule.get("compareValue");

            boolean singleResult = evaluateSingleRule(fieldKey, operator, compareValue, variables);

            if (isOr) {
                if (singleResult) return true;
            } else {
                if (!singleResult) return false;
            }
        }

        return isOr ? false : true;
    }

    private boolean evaluateSingleRule(String fieldKey, String operator, Object compareValue, Map<String, Object> variables) {
        if (fieldKey == null || operator == null) {
            return true;
        }

        Object actualVal = variables.get(fieldKey);
        if (actualVal == null) {
            return false;
        }

        String actualStr = actualVal.toString().trim();
        String targetStr = compareValue != null ? compareValue.toString().trim() : "";

        // So sánh dạng số
        try {
            double actualNum = Double.parseDouble(actualStr);
            double targetNum = Double.parseDouble(targetStr);

            return switch (operator.toUpperCase()) {
                case "GREATER_THAN", ">" -> actualNum > targetNum;
                case "GREATER_THAN_OR_EQUAL", ">=" -> actualNum >= targetNum;
                case "LESS_THAN", "<" -> actualNum < targetNum;
                case "LESS_THAN_OR_EQUAL", "<=" -> actualNum <= targetNum;
                case "EQUALS", "==" -> Math.abs(actualNum - targetNum) < 0.00001;
                case "NOT_EQUALS", "!=" -> Math.abs(actualNum - targetNum) >= 0.00001;
                default -> false;
            };
        } catch (NumberFormatException ignored) {
            // Chuyển sang so sánh chuỗi
        }

        return switch (operator.toUpperCase()) {
            case "EQUALS", "==" -> actualStr.equalsIgnoreCase(targetStr);
            case "NOT_EQUALS", "!=" -> !actualStr.equalsIgnoreCase(targetStr);
            case "CONTAINS" -> actualStr.toLowerCase().contains(targetStr.toLowerCase());
            default -> false;
        };
    }

    private User resolveAssignee(WorkflowInstance instance, Node node) {
        Map<String, Object> config = parseJsonMap(node.getConfig());
        String approverType = (String) config.getOrDefault("approverType", config.getOrDefault("reviewerType", config.getOrDefault("assigneeType", "role")));

        // 1. Chỉ định User cụ thể theo ID
        Object userIdObj = config.get("approverUserId");
        if (userIdObj == null) userIdObj = config.get("reviewerUserId");
        if (userIdObj == null) userIdObj = config.get("assigneeUserId");

        if (userIdObj instanceof Number num) {
            User u = userRepository.findById(num.intValue()).orElse(null);
            if (u != null) return u;
        } else if (userIdObj instanceof String str && !str.trim().isEmpty()) {
            try {
                User u = userRepository.findById(Integer.parseInt(str.trim())).orElse(null);
                if (u != null) return u;
            } catch (NumberFormatException ignored) {
            }
        }

        // 2. Chỉ định động theo Manager của Creator
        if ("manager".equalsIgnoreCase(approverType) || "dynamic".equalsIgnoreCase(approverType)) {
            if (instance.getCreator() != null && instance.getCreator().getManager() != null) {
                return instance.getCreator().getManager();
            }
        }

        // 3. Chỉ định theo Role
        String roleStr = (String) config.get("approverRole");
        if (roleStr == null) roleStr = (String) config.get("reviewerRole");
        if (roleStr == null) roleStr = (String) config.get("assigneeRole");

        if (roleStr != null && !roleStr.trim().isEmpty()) {
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if (u.getRole() != null && u.getRole().name().equalsIgnoreCase(roleStr.trim())) {
                    return u;
                }
            }
        }

        // 4. Fallback: gán cho Workflow Owner hoặc user có vai trò Admin / Approver
        if (instance.getWorkflow().getOwner() != null) {
            return instance.getWorkflow().getOwner();
        }

        return userRepository.findAll().stream().findFirst().orElse(null);
    }

    private List<User> resolveApprovers(WorkflowInstance instance, Node node) {
        Map<String, Object> config = parseJsonMap(node.getConfig());
        String approvalMode = (String) config.getOrDefault("approvalMode", "single");

        if (!"multi".equalsIgnoreCase(approvalMode)) {
            User single = resolveAssignee(instance, node);
            return single != null ? List.of(single) : Collections.emptyList();
        }

        String multiAssigneeType = (String) config.getOrDefault("multiAssigneeType", "users");
        List<User> approvers = new ArrayList<>();

        if ("roles".equalsIgnoreCase(multiAssigneeType)) {
            Object rolesObj = config.get("approverRoles");
            List<String> roles = new ArrayList<>();
            if (rolesObj instanceof List<?> rList) {
                for (Object o : rList) {
                    if (o != null) roles.add(o.toString().trim());
                }
            }

            if (!roles.isEmpty()) {
                List<User> allUsers = userRepository.findAll();
                for (User u : allUsers) {
                    if (u.getRole() != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(u.getRole().name()))) {
                        if (!approvers.contains(u)) {
                            approvers.add(u);
                        }
                    }
                }
            }
        } else {
            // users
            Object usersObj = config.get("approverUserIds");
            List<Integer> userIds = new ArrayList<>();
            if (usersObj instanceof List<?> uList) {
                for (Object o : uList) {
                    if (o instanceof Number num) {
                        userIds.add(num.intValue());
                    } else if (o != null) {
                        try {
                            userIds.add(Integer.parseInt(o.toString().trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (!userIds.isEmpty()) {
                for (Integer uid : userIds) {
                    userRepository.findById(uid).ifPresent(u -> {
                        if (!approvers.contains(u)) approvers.add(u);
                    });
                }
            }
        }

        if (approvers.isEmpty()) {
            User fallback = resolveAssignee(instance, node);
            if (fallback != null) approvers.add(fallback);
        }

        return approvers;
    }

    private void evaluateMultiApprovalDecision(WorkflowInstance instance, Node currentNode, Map<String, Object> variables) {
        List<TaskInstance> nodeTasks = taskInstanceRepository.findByWorkflowInstanceIdOrderByIdAsc(instance.getId()).stream()
                .filter(t -> t.getNode().getId().equals(currentNode.getId()))
                .toList();

        int total = nodeTasks.size();
        if (total <= 1) {
            TaskInstance singleTask = nodeTasks.get(0);
            boolean isApp = singleTask.getStatus() == TaskStatus.Approved;
            advanceFromNode(instance, currentNode, variables, isApp ? "APPROVE" : "REJECT");
            return;
        }

        long approvedCount = nodeTasks.stream().filter(t -> t.getStatus() == TaskStatus.Approved).count();
        long rejectedCount = nodeTasks.stream().filter(t -> t.getStatus() == TaskStatus.Rejected).count();
        long pendingCount = nodeTasks.stream().filter(t -> t.getStatus() == TaskStatus.Pending).count();

        Map<String, Object> nodeConfig = parseJsonMap(currentNode.getConfig());
        String multiApprovalRule = (String) nodeConfig.getOrDefault("multiApprovalRule", "all");
        String multiRejectionRule = (String) nodeConfig.getOrDefault("multiRejectionRule", "any");

        int threshold = 1;
        Object threshObj = nodeConfig.get("approvalThreshold");
        if (threshObj instanceof Number num) {
            threshold = Math.max(1, num.intValue());
        }

        int requiredForApprove = switch (multiApprovalRule.toLowerCase()) {
            case "half" -> (int) Math.ceil(total / 2.0);
            case "majority" -> (int) Math.floor(total / 2.0) + 1;
            case "threshold" -> Math.min(threshold, total);
            case "any" -> 1;
            default -> total; // "all"
        };

        // 1. Kiểm tra điều kiện Từ chối (Rejection)
        boolean isCouncilRejected = false;
        if ("any".equalsIgnoreCase(multiRejectionRule) && rejectedCount >= 1) {
            isCouncilRejected = true;
        } else if ("majority".equalsIgnoreCase(multiRejectionRule) && rejectedCount > (total / 2.0)) {
            isCouncilRejected = true;
        } else if (approvedCount + pendingCount < requiredForApprove) {
            // Không còn khả năng đạt đủ số phiếu đồng thuận
            isCouncilRejected = true;
        }

        if (isCouncilRejected) {
            log.info("-> Hội đồng duyệt Node ID: {} quyết định: TỪ CHỐI (Rejected: {}/{}, Rule: {})",
                    currentNode.getId(), rejectedCount, total, multiRejectionRule);

            for (TaskInstance remaining : nodeTasks) {
                if (remaining.getStatus() == TaskStatus.Pending) {
                    remaining.setStatus(TaskStatus.Rejected);
                    remaining.setCompletedAt(LocalDateTime.now());
                    remaining.setStepSubmittedData("{\"action\":\"AUTO_CLOSE\",\"comment\":\"Đã đóng tự động do hội đồng đã có quyết định từ chối\"}");
                    taskInstanceRepository.save(remaining);
                }
            }

            recordChangeLog(instance.getWorkflow(), null, "COUNCIL_DECISION", Map.of(
                    "ticketId", instance.getId(),
                    "nodeName", currentNode.getName(),
                    "decision", "REJECT",
                    "approvedCount", approvedCount,
                    "rejectedCount", rejectedCount,
                    "total", total
            ));

            advanceFromNode(instance, currentNode, variables, "REJECT");
            return;
        }

        // 2. Kiểm tra điều kiện Phê duyệt (Approval)
        boolean isCouncilApproved = approvedCount >= requiredForApprove;

        if (isCouncilApproved) {
            log.info("-> Hội đồng duyệt Node ID: {} quyết định: THÔNG QUA (Approved: {}/{}, Rule: {})",
                    currentNode.getId(), approvedCount, total, multiApprovalRule);

            for (TaskInstance remaining : nodeTasks) {
                if (remaining.getStatus() == TaskStatus.Pending) {
                    remaining.setStatus(TaskStatus.Approved);
                    remaining.setCompletedAt(LocalDateTime.now());
                    remaining.setStepSubmittedData("{\"action\":\"AUTO_SKIP\",\"comment\":\"Tự động hoàn tất do hội đồng đã đạt đủ số phiếu phê duyệt\"}");
                    taskInstanceRepository.save(remaining);
                }
            }

            recordChangeLog(instance.getWorkflow(), null, "COUNCIL_DECISION", Map.of(
                    "ticketId", instance.getId(),
                    "nodeName", currentNode.getName(),
                    "decision", "APPROVE",
                    "approvedCount", approvedCount,
                    "rejectedCount", rejectedCount,
                    "total", total
            ));

            advanceFromNode(instance, currentNode, variables, "APPROVE");
            return;
        }

        // 3. Vẫn chưa đủ điều kiện: tiếp tục chờ các thành viên hội đồng còn lại
        log.info("-> Hội đồng duyệt Node ID: {} đang tiếp tục chờ biểu quyết (Approved: {}/{}, Cần: {}, Pending: {})",
                currentNode.getId(), approvedCount, total, requiredForApprove, pendingCount);
    }

    private LocalDateTime calculateDueDate(Node node) {
        Map<String, Object> config = parseJsonMap(node.getConfig());
        int slaHours = 24;
        Object slaObj = config.get("slaHours");
        if (slaObj instanceof Number num) {
            slaHours = num.intValue();
        }

        Object dueDaysObj = config.get("dueDays");
        if (dueDaysObj instanceof Number num) {
            slaHours = num.intValue() * 24;
        }

        return LocalDateTime.now().plusHours(slaHours);
    }

    private void completeInstance(WorkflowInstance instance, WorkflowInstanceStatus status) {
        instance.setStatus(status);
        instance.setEndTime(LocalDateTime.now());
        workflowInstanceRepository.save(instance);
        log.info("Ticket ID: {} đã hoàn thành với trạng thái: {}", instance.getId(), status);

        recordChangeLog(instance.getWorkflow(), instance.getCreator(), "COMPLETE_TICKET", Map.of(
                "ticketId", instance.getId(),
                "status", status.name()
        ));
    }

    private void recordChangeLog(Workflow workflow, User user, String action, Map<String, Object> details) {
        try {
            String detailsJson = objectMapper.writeValueAsString(details);
            ChangeLog log = ChangeLog.builder()
                    .workflow(workflow)
                    .user(user)
                    .action(action)
                    .details(detailsJson)
                    .build();
            changeLogRepository.save(log);
        } catch (Exception e) {
            log.error("Lỗi lưu ChangeLog: {}", e.getMessage());
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private TicketResponseDTO mapToTicketResponseDTO(WorkflowInstance instance) {
        Workflow workflow = instance.getWorkflow();

        Map<String, Object> variables = parseJsonMap(instance.getVariables());
        String ticketTitle = (String) variables.getOrDefault("__ticket_title", "Yêu cầu " + instance.getRequestCode());

        // Tìm node và người xử lý hiện tại
        String currentNodeName = null;
        String currentNodeType = null;
        UserSummaryDTO currentAssignee = null;

        if (instance.getStatus() == WorkflowInstanceStatus.Running) {
            Optional<TaskInstance> pendingTaskOpt = taskInstanceRepository
                    .findFirstByWorkflowInstanceIdAndStatusOrderByIdDesc(instance.getId(), TaskStatus.Pending);

            if (pendingTaskOpt.isPresent()) {
                TaskInstance pt = pendingTaskOpt.get();
                currentNodeName = pt.getNode().getName();
                currentNodeType = pt.getNode().getType().name();
                if (pt.getAssignedUser() != null) {
                    currentAssignee = mapToUserSummaryDTO(pt.getAssignedUser());
                }
            }
        } else {
            currentNodeName = instance.getStatus() == WorkflowInstanceStatus.Completed ? "Hoàn thành" : "Từ chối";
            currentNodeType = "End";
        }

        return TicketResponseDTO.builder()
                .id(instance.getId())
                .requestCode(instance.getRequestCode())
                .workflowId(workflow.getId())
                .workflowName(workflow.getName())
                .workflowCode(String.format("WF-%03d", workflow.getId()))
                .title(ticketTitle)
                .creator(mapToUserSummaryDTO(instance.getCreator()))
                .status(instance.getStatus())
                .currentNodeName(currentNodeName)
                .currentNodeType(currentNodeType)
                .currentAssignee(currentAssignee)
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .build();
    }

    private UserSummaryDTO mapToUserSummaryDTO(User user) {
        if (user == null) return null;
        return UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole() : UserRole.Viewer)
                .build();
    }
}
