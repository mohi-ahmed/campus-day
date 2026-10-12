package com.spark.campusday.study.assistant.service;

import org.springframework.ai.chat.client.ChatClient;

interface TaskbotChatProvider {
    boolean available();

    String displayName();

    String model();

    ChatClient client();
}
