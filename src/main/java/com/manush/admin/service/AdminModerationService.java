package com.manush.admin.service;

import com.manush.admin.model.AuditLog;
import com.manush.admin.repository.AuditLogRepository;
import com.manush.auth.model.User;
import com.manush.auth.model.VerificationState;
import com.manush.auth.repository.UserRepository;
import com.manush.auth.repository.VerificationStateRepository;
import com.manush.common.exception.ResourceNotFoundException;
import com.manush.common.provider.IdentityVerificationProvider;
import com.manush.help.model.Report;
import com.manush.help.repository.ReportRepository;
import com.manush.job.model.Professional;
import com.manush.job.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private final VerificationStateRepository verificationStateRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final IdentityVerificationProvider identityVerificationProvider;

    // --- Verification Queue ---

    public VerificationState submitVerification(User user, String documentType, String documentNumber, String documentFileId) {
        IdentityVerificationProvider.VerificationRequest req = new IdentityVerificationProvider.VerificationRequest(
                user.getId(),
                user.getName(),
                documentType,
                documentNumber,
                documentFileId
        );

        IdentityVerificationProvider.VerificationResult res = identityVerificationProvider.verify(req);

        VerificationState state = verificationStateRepository.findByUserId(user.getId())
                .orElse(VerificationState.builder().userId(user.getId()).build());

        state.setDocumentType(documentType);
        state.setDocumentFileId(documentFileId);
        state.setStatus(VerificationState.VerificationStatus.PENDING);
        state.setSubmittedAt(Instant.now());
        state.setAdminNote(res.message());

        VerificationState saved = verificationStateRepository.save(state);

        // Update professional status if exists
        professionalRepository.findByUserId(user.getId()).ifPresent(prof -> {
            prof.setVerificationStatus(Professional.VerificationStatus.PENDING);
            professionalRepository.save(prof);
        });

        return saved;
    }

    public Page<VerificationState> getVerificationQueue(VerificationState.VerificationStatus status, Pageable pageable) {
        if (status != null) {
            return verificationStateRepository.findByStatus(status, pageable);
        }
        return verificationStateRepository.findAll(pageable);
    }

    public VerificationState reviewVerification(String verificationId, User admin, boolean approved, String note, String ipAddress) {
        VerificationState state = verificationStateRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification record not found: " + verificationId));

        VerificationState.VerificationStatus newStatus = approved ?
                VerificationState.VerificationStatus.VERIFIED :
                VerificationState.VerificationStatus.REJECTED;

        state.setStatus(newStatus);
        state.setAdminNote(note);
        state.setReviewedBy(admin.getId());
        state.setReviewedAt(Instant.now());
        if (approved) {
            state.setExpiresAt(Instant.now().plus(730, ChronoUnit.DAYS)); // 2 years expiry
        }

        VerificationState updated = verificationStateRepository.save(state);

        // Update professional verification if user is professional
        professionalRepository.findByUserId(state.getUserId()).ifPresent(prof -> {
            prof.setVerificationStatus(approved ?
                    Professional.VerificationStatus.VERIFIED :
                    Professional.VerificationStatus.REJECTED);
            professionalRepository.save(prof);
        });

        // Audit Log
        AuditLog audit = AuditLog.builder()
                .actorId(admin.getId())
                .action(approved ? "APPROVE_VERIFICATION" : "REJECT_VERIFICATION")
                .resourceType("VERIFICATION")
                .resourceId(state.getId())
                .ipAddress(ipAddress)
                .notes("User: " + state.getUserId() + ". Note: " + note)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(audit);

        return updated;
    }

    // --- Reports Moderation Queue ---

    public Page<Report> getReportQueue(Report.ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return reportRepository.findAll(pageable);
    }

    public Report reviewReport(String reportId, User admin, Report.ReportStatus nextStatus, String note, String ipAddress) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));

        report.setStatus(nextStatus);
        report.setAdminNote(note);
        report.setResolvedBy(admin.getId());
        report.setUpdatedAt(Instant.now());

        Report updated = reportRepository.save(report);

        AuditLog audit = AuditLog.builder()
                .actorId(admin.getId())
                .action("MODERATE_REPORT_" + nextStatus.name())
                .resourceType(report.getTargetType() != null ? report.getTargetType().name() : "REPORT")
                .resourceId(report.getTargetId())
                .ipAddress(ipAddress)
                .notes("Report: " + report.getId() + ". Note: " + note)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(audit);

        return updated;
    }

    // --- User Ban Moderation ---

    public void banUser(String targetUserId, User admin, String banReason, String ipAddress) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));

        target.setBanned(true);
        target.setActive(false);
        target.setBanReason(banReason);
        userRepository.save(target);

        AuditLog audit = AuditLog.builder()
                .actorId(admin.getId())
                .action("BAN_USER")
                .resourceType("USER")
                .resourceId(targetUserId)
                .ipAddress(ipAddress)
                .notes(banReason)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(audit);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
