package com.spark.campusday.auth.mail;

public interface TransactionalEmailSender {
    void sendHtml(String recipientEmail, String subject, String htmlContent);
}
