package com.manush.auth.controller;

import com.manush.admin.service.AdminModerationService;
import com.manush.auth.model.User;
import com.manush.auth.model.VerificationState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final AdminModerationService adminModerationService;

    public record SubmitVerificationRequest(
            String documentType,
            String documentNumber,
            String documentFileId
    ) {}

    @PostMapping("/submit")
    public ResponseEntity<VerificationState> submitVerification(
            @RequestBody SubmitVerificationRequest request,
            @AuthenticationPrincipal User user) {
        VerificationState state = adminModerationService.submitVerification(
                user,
                request.documentType(),
                request.documentNumber(),
                request.documentFileId()
        );
        return ResponseEntity.ok(state);
    }
}
