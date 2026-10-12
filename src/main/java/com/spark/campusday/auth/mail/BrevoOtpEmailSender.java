package com.spark.campusday.auth.mail;

import com.spark.campusday.auth.model.OtpPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrevoOtpEmailSender implements OtpEmailSender {
    private final TransactionalEmailSender emailSender;

    @Override
    public void send(String recipientEmail, String otp, OtpPurpose purpose) {
        String action = purpose == OtpPurpose.EMAIL_VERIFICATION
                ? "verify your Campus Day email"
                : "reset your Campus Day password";
        String subject = purpose == OtpPurpose.EMAIL_VERIFICATION
                ? "Verify your Campus Day email"
                : "Reset your Campus Day password";
        String html = """
                <div style="font-family:Arial,sans-serif;color:#10251b;line-height:1.6">
                  <h2 style="margin-bottom:8px">Campus Day</h2>
                  <p>Use this one-time code to %s:</p>
                  <p style="font-size:32px;font-weight:700;letter-spacing:8px;margin:24px 0">%s</p>
                  <p>This code expires soon. If you did not request it, you can ignore this email.</p>
                </div>
                """.formatted(action, otp);

        emailSender.sendHtml(recipientEmail, subject, html);
    }
}
