package com.spark.campusday.auth.mail;

import com.spark.campusday.auth.model.OtpPurpose;

public interface OtpEmailSender {
    void send(String recipientEmail, String otp, OtpPurpose purpose);
}
