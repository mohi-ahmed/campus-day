package com.spark.campusday.study.assistant.service;

import com.spark.campusday.study.assistant.config.TaskbotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
class GroqTaskbotProvider implements TaskbotChatProvider {
    private final TaskbotProperties properties;
    private volatile ChatClient chatClient;

    @Override
    public boolean available() {
        return properties.groqConfigured();
    }

    @Override
    public String displayName() {
        return "Groq Cloud";
    }

    @Override
    public String model() {
        return properties.groqModel();
    }

    @Override
    public ChatClient client() {
        ChatClient current = chatClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (chatClient == null) {
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .baseUrl(properties.groqBaseUrl())
                        .apiKey(properties.groqApiKey().trim())
                        .model(properties.groqModel())
                        .temperature(properties.temperature())
                        .maxTokens(Math.max(1024, properties.maxOutputTokens()))
                        .build();
                OpenAiChatModel chatModel = OpenAiChatModel.builder()
                        .options(options)
                        .build();
                chatClient = ChatClient.builder(chatModel).build();
            }
            return chatClient;
        }
    }
}
