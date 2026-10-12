package com.manush.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** TTL-indexed – auto-expires in 300 s (replaces Redis EX key). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_codes")
public class OtpCode {

    @Id
    private String id;

    private String identifier; // email or phone
    private String codeHash;   // BCrypt of 6-digit code
    private OtpPurpose purpose;
    private int attempts;

    @Indexed(expireAfter = "300s")
    private Instant createdAt;

    public enum OtpPurpose { EMAIL_VERIFY, PHONE_VERIFY, PASSWORD_RESET }
}
