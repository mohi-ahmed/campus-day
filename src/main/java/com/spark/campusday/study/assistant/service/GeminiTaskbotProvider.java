package com.spark.campusday.study.assistant.service;

import com.google.genai.Client;
import com.spark.campusday.study.assistant.config.TaskbotProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
class GeminiTaskbotProvider implements TaskbotChatProvider {
    private final TaskbotProperties properties;
    private volatile ChatClient chatClient;
    private volatile Client genAiClient;

    @Override
    public boolean available() {
        return properties.geminiConfigured();
    }

    @Override
    public String displayName() {
        return "Google Gemini";
    }

    @Override
    public String model() {
        return properties.model();
    }

    @Override
    public ChatClient client() {
        ChatClient current = chatClient;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (chatClient == null) {
                Client providerClient = Client.builder()
                        .apiKey(properties.apiKey().trim())
                        .build();
                GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                        .model(properties.model())
                        .temperature(properties.temperature())
                        .maxOutputTokens(Math.max(1024, properties.maxOutputTokens()))
                        .build();
                try {
                    GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                            .genAiClient(providerClient)
                            .options(options)
                            .build();
                    chatClient = ChatClient.builder(chatModel).build();
                    genAiClient = providerClient;
                } catch (RuntimeException exception) {
                    providerClient.close();
                    throw exception;
                }
            }
            return chatClient;
        }
    }

    @PreDestroy
    void closeProviderClient() {
        Client current = genAiClient;
        if (current != null) {
            current.close();
        }
    }
}
