package com.spark.campusday.study.assistant.dto;

import com.spark.campusday.study.assistant.model.TaskbotAction;
import com.spark.campusday.study.assistant.model.TaskbotIntent;

import java.util.List;

public record TaskbotChatResponse(
        String reply,
        TaskbotIntent intent,
        TaskbotAction proposedAction,
        TaskbotTaskDraft taskDraft,
        List<TaskbotPlanItem> plan
) {
}
