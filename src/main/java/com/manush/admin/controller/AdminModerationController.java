package com.manush.admin.controller;

import com.manush.admin.model.AuditLog;
import com.manush.admin.service.AdminModerationService;
import com.manush.auth.model.User;
import com.manush.auth.model.VerificationState;
import com.manush.help.model.Report;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    // --- Verification Queue ---

    @GetMapping("/verifications")
    public ResponseEntity<Page<VerificationState>> getVerifications(
            @RequestParam(required = false) VerificationState.VerificationStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(adminModerationService.getVerificationQueue(status, pageable));
    }

    @PostMapping("/verifications/{id}/review")
    public ResponseEntity<VerificationState> reviewVerification(
            @PathVariable String id,
            @RequestParam boolean approved,
            @RequestParam(required = false, defaultValue = "") String note,
            @AuthenticationPrincipal User admin,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ResponseEntity.ok(adminModerationService.reviewVerification(id, admin, approved, note, ip));
    }

    // --- Reports Moderation Queue ---

    @GetMapping("/reports")
    public ResponseEntity<Page<Report>> getReports(
            @RequestParam(required = false) Report.ReportStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(adminModerationService.getReportQueue(status, pageable));
    }

    @PostMapping("/reports/{id}/moderate")
    public ResponseEntity<Report> moderateReport(
            @PathVariable String id,
            @RequestParam Report.ReportStatus status,
            @RequestParam(required = false, defaultValue = "") String note,
            @AuthenticationPrincipal User admin,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ResponseEntity.ok(adminModerationService.reviewReport(id, admin, status, note, ip));
    }

    // --- User Ban ---

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable String userId,
            @RequestParam String reason,
            @AuthenticationPrincipal User admin,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        adminModerationService.banUser(userId, admin, reason, ip);
        return ResponseEntity.noContent().build();
    }

    // --- Audit Logs ---

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(adminModerationService.getAuditLogs(pageable));
    }
}
