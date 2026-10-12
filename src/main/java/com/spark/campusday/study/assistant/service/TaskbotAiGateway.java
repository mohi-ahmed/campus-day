package com.spark.campusday.study.assistant.service;

import com.spark.campusday.study.assistant.dto.TaskbotChatRequest;
import com.spark.campusday.study.assistant.dto.TaskbotChatResponse;

public interface TaskbotAiGateway {
    boolean available();

    String providerSummary();

    String modelSummary();

    TaskbotChatResponse chat(String studyContext, TaskbotChatRequest request);
}
