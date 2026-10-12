package com.spark.campusday.study.assistant.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.assistant.dto.TaskbotChatRequest;
import com.spark.campusday.study.assistant.dto.TaskbotChatResponse;
import com.spark.campusday.study.assistant.dto.TaskbotStatusResponse;
import com.spark.campusday.study.assistant.dto.TaskbotPlanItem;
import com.spark.campusday.study.assistant.dto.TaskbotTaskDraft;
import com.spark.campusday.study.assistant.model.TaskbotAction;
import com.spark.campusday.study.assistant.model.TaskbotIntent;
import com.spark.campusday.study.task.dto.StudyTaskRequest;
import com.spark.campusday.study.task.dto.StudyTaskResponse;
import com.spark.campusday.study.task.service.StudyTaskService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskbotService {
    private static final int MAX_PLAN_ITEMS = 8;

    private final TaskbotAiGateway aiGateway;
    private final TaskbotStudyContextService studyContext;
    private final TaskbotRateLimit rateLimit;
    private final StudyTaskService tasks;
    private final StudentService students;
    private final Validator validator;

    public TaskbotStatusResponse status() {
        return new TaskbotStatusResponse(
                aiGateway.available(), aiGateway.providerSummary(),
                aiGateway.modelSummary());
    }

    public TaskbotChatResponse chat(String studentId, TaskbotChatRequest request) {
        validateStudentId(studentId);
        validate(request, "Taskbot chat request is required");
        String ownerId = students.getByStudentId(studentId).getId();
        rateLimit.check(ownerId);

        TaskbotChatResponse response = aiGateway.chat(
                studyContext.context(studentId), request);
        TaskbotIntent intent = response.intent() == null
                ? TaskbotIntent.GENERAL_STUDY_HELP : response.intent();
        TaskbotAction action = response.proposedAction() == null
                ? TaskbotAction.NONE : response.proposedAction();
        TaskbotTaskDraft draft = response.taskDraft();
        if (action != TaskbotAction.CREATE_TASK_DRAFT
                || draft == null || !validator.validate(draft).isEmpty()) {
            action = TaskbotAction.NONE;
            draft = null;
        }
        return new TaskbotChatResponse(
                response.reply().trim(), intent, action, draft,
                validPlanItems(response.plan()));
    }

    public StudyTaskResponse confirmTask(String studentId, TaskbotTaskDraft draft) {
        validateStudentId(studentId);
        validate(draft, "Taskbot task draft is required");
        return tasks.create(studentId, new StudyTaskRequest(
                draft.title(),
                draft.subject(),
                draft.taskType(),
                draft.priority(),
                draft.dueDate(),
                draft.dueTime(),
                draft.estimatedMinutes(),
                draft.repeat(),
                draft.note()));
    }

    private void validateStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new InvalidRequestException("Student id is required");
        }
    }

    private void validate(Object value, String missingMessage) {
        if (value == null) {
            throw new InvalidRequestException(missingMessage);
        }
        var violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private List<TaskbotPlanItem> validPlanItems(List<TaskbotPlanItem> plan) {
        if (plan == null) {
            return List.of();
        }
        return plan.stream()
                .filter(item -> item != null
                        && item.type() != null
                        && item.title() != null
                        && !item.title().isBlank()
                        && item.durationMinutes() != null
                        && item.durationMinutes() > 0
                        && item.durationMinutes() <= 1440)
                .limit(MAX_PLAN_ITEMS)
                .toList();
    }
}
