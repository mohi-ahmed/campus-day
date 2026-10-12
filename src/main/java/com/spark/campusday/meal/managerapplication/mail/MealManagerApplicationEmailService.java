package com.spark.campusday.meal.managerapplication.mail;

import com.spark.campusday.auth.mail.TransactionalEmailSender;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.managerapplication.config.MealManagerApplicationProperties;
import com.spark.campusday.meal.managerapplication.model.MealManagerApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class MealManagerApplicationEmailService {
    private final TransactionalEmailSender emailSender;
    private final MealManagerApplicationProperties properties;

    public void sendApplicationSubmitted(MealManagerApplication application, String rawToken) {
        if (!StringUtils.hasText(properties.getAdminEmail())) {
            throw new InvalidBusinessStateException(
                    "Meal Manager approval email is not configured. Set MEAL_MANAGER_ADMIN_EMAIL.");
        }
        String approveUrl = reviewUrl(rawToken, "APPROVE");
        String rejectUrl = reviewUrl(rawToken, "REJECT");
        String studentName = HtmlUtils.htmlEscape(application.getStudentName());
        String studentId = HtmlUtils.htmlEscape(application.getStudentId());
        String studentEmail = HtmlUtils.htmlEscape(application.getStudentEmail());
        String periodRow = application.getPeriodStartDate() == null ? "" : """
                    <tr><td style="padding:8px 0;color:#738078">Management period</td><td style="padding:8px 0">%s</td></tr>
                """.formatted(HtmlUtils.htmlEscape(
                application.getPeriodStartDate() + " to " + application.getPeriodEndDate()));
        String html = """
                <div style="font-family:Arial,sans-serif;color:#14251c;line-height:1.6;max-width:620px;margin:auto">
                  <div style="border-bottom:1px solid #d9e2dd;padding-bottom:16px;margin-bottom:24px">
                    <strong style="font-size:20px">Campus Day</strong>
                    <div style="color:#607069;font-size:13px">Meal Manager access request</div>
                  </div>
                  <h2 style="font-size:24px;margin:0 0 12px">Review %s's application</h2>
                  <p style="color:#52625a;margin:0 0 22px">The decision link is valid for 72 hours. After that, the application is automatically rejected.</p>
                  <table style="width:100%%;border-collapse:collapse;margin-bottom:26px">
                    <tr><td style="padding:8px 0;color:#738078">Student ID</td><td style="padding:8px 0;font-weight:700">%s</td></tr>
                    <tr><td style="padding:8px 0;color:#738078">Email</td><td style="padding:8px 0">%s</td></tr>
                    %s
                  </table>
                  <div style="display:flex;gap:12px;flex-wrap:wrap">
                    <a href="%s" style="display:inline-block;padding:12px 20px;border-radius:8px;background:#0b865e;color:#fff;text-decoration:none;font-weight:700">Review and approve</a>
                    <a href="%s" style="display:inline-block;padding:11px 20px;border:1px solid #bd5555;border-radius:8px;color:#9b3939;text-decoration:none;font-weight:700">Review and reject</a>
                  </div>
                  <p style="margin-top:24px;color:#77847e;font-size:12px">For security, the email button opens a confirmation page. No decision is made until you confirm there.</p>
                </div>
                """.formatted(studentName, studentId, studentEmail, periodRow, approveUrl, rejectUrl);
        emailSender.sendHtml(
                properties.getAdminEmail(),
                "Meal Manager application: " + application.getStudentName(),
                html);
    }

    private String reviewUrl(String rawToken, String decision) {
        return UriComponentsBuilder.fromUri(properties.getFrontendBaseUrl())
                .replacePath("/meal-manager/review")
                .queryParam("token", rawToken)
                .queryParam("decision", decision)
                .build()
                .encode()
                .toUriString();
    }
}
