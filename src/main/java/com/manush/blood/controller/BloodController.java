package com.manush.blood.controller;

import com.manush.auth.model.User;
import com.manush.blood.model.BloodDonor;
import com.manush.blood.model.BloodRequest;
import com.manush.blood.service.BloodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Blood Bank", description = "Blood donation and request management")
@RestController
@RequestMapping("/api/v1/blood")
@RequiredArgsConstructor
public class BloodController {

    private final BloodService bloodService;

    @Operation(summary = "Find nearby blood donors by blood group")
    @GetMapping("/donors/nearby")
    public ResponseEntity<List<BloodDonor>> findNearbyDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "10") double radius,
            Pageable pageable) {
        return ResponseEntity.ok(bloodService.findNearbyDonors(bloodGroup, lat, lng, radius, pageable));
    }

    @Operation(summary = "Create blood donation record")
    @PostMapping("/donations")
    public ResponseEntity<BloodDonor> registerDonation(
            @AuthenticationPrincipal User user,
            @RequestBody BloodDonor donor) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bloodService.registerDonation(user, donor));
    }

    @Operation(summary = "Get my donation history")
    @GetMapping("/donations/my")
    public ResponseEntity<List<BloodDonor>> getMyDonations(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bloodService.getMyDonations(user));
    }

    @Operation(summary = "Create blood request")
    @PostMapping("/requests")
    public ResponseEntity<BloodRequest> createRequest(
            @AuthenticationPrincipal User user,
            @RequestBody BloodRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bloodService.createRequest(user, request));
    }

    @Operation(summary = "Get all blood requests")
    @GetMapping("/requests")
    public ResponseEntity<List<BloodRequest>> getRequests(Pageable pageable) {
        return ResponseEntity.ok(bloodService.getRequests(pageable));
    }

    @Operation(summary = "Get my blood requests")
    @GetMapping("/requests/my")
    public ResponseEntity<List<BloodRequest>> getMyRequests(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bloodService.getMyRequests(user));
    }

    @Operation(summary = "Update blood request status")
    @PatchMapping("/requests/{id}/status")
    public ResponseEntity<BloodRequest> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        try {
            return ResponseEntity.ok(bloodService.updateStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
