package com.spark.campusday.auth.mail;

import com.spark.campusday.auth.config.AuthProperties;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class BrevoTransactionalEmailSender implements TransactionalEmailSender {
    private final AuthProperties properties;
    private final RestClient restClient;

    public BrevoTransactionalEmailSender(AuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBrevo().getBaseUrl())
                .build();
    }

    @Override
    public void sendHtml(String recipientEmail, String subject, String htmlContent) {
        ensureConfigured();
        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", properties.getBrevo().getApiKey())
                    .body(new BrevoMessage(
                            new Sender(properties.getBrevo().getSenderEmail(),
                                    properties.getBrevo().getSenderName()),
                            List.of(new Recipient(recipientEmail)),
                            subject,
                            htmlContent))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new InvalidBusinessStateException("Email could not be delivered. Please try again.");
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.getBrevo().getApiKey())
                || !StringUtils.hasText(properties.getBrevo().getSenderEmail())) {
            throw new InvalidBusinessStateException(
                    "Brevo email delivery is not configured. Set BREVO_API_KEY and BREVO_SENDER_EMAIL.");
        }
    }

    private record Sender(String email, String name) {
    }

    private record Recipient(String email) {
    }

    private record BrevoMessage(Sender sender, List<Recipient> to, String subject, String htmlContent) {
    }
}
