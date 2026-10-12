package com.manush.blood.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.blood.model.BloodDonor;
import com.manush.blood.model.BloodRequest;
import com.manush.blood.repository.BloodDonorRepository;
import com.manush.blood.repository.BloodRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloodService {

    private final BloodDonorRepository donorRepository;
    private final BloodRequestRepository requestRepository;
    private final UserRepository userRepository;

    public List<BloodDonor> findNearbyDonors(String bloodGroup, Double lat, Double lng, double radius, Pageable pageable) {
        if (lat != null && lng != null) {
            Point point = new Point(lng, lat);
            if (bloodGroup != null && !bloodGroup.isEmpty()) {
                return donorRepository.findByBloodGroupAndLocationNear(
                    bloodGroup, point, new Distance(radius, Metrics.KILOMETERS), pageable
                ).getContent();
            }
            return donorRepository.findByLocationNear(
                point, new Distance(radius, Metrics.KILOMETERS), pageable
            ).getContent();
        }
        
        if (bloodGroup != null && !bloodGroup.isEmpty()) {
            return donorRepository.findByBloodGroup(bloodGroup, pageable).getContent();
        }
        
        return donorRepository.findAll(pageable).getContent();
    }

    public BloodDonor registerDonation(User user, BloodDonor donor) {
        donor.setUserId(user.getId());
        donor.setDonorName(user.getName());
        donor.setBloodGroup(user.getBloodGroup());
        if (user.getLocation() != null) {
            donor.setLocation(new BloodDonor.Location(
                "Point", user.getLocation().getCoordinates(), user.getLocation().getArea(), user.getLocation().getCity()
            ));
        }
        BloodDonor saved = donorRepository.save(donor);
        
        // Award points
        user.setHelpingPoints(user.getHelpingPoints() + 50);
        user.getBadges().add("blood-donor");
        userRepository.save(user);
        
        return saved;
    }

    public List<BloodDonor> getMyDonations(User user) {
        return donorRepository.findByUserId(user.getId()).map(List::of).orElse(List.of());
    }

    public BloodRequest createRequest(User user, BloodRequest request) {
        request.setRequesterId(user.getId());
        request.setRequesterName(user.getName());
        request.setRequesterPhone(user.getPhone());
        if (user.getLocation() != null) {
            request.setLocation(new BloodRequest.Location(
                "Point", user.getLocation().getCoordinates(), user.getLocation().getArea(), user.getLocation().getCity()
            ));
        }
        return requestRepository.save(request);
    }

    public List<BloodRequest> getRequests(Pageable pageable) {
        return requestRepository.findAll(pageable).getContent();
    }

    public List<BloodRequest> getMyRequests(User user) {
        return requestRepository.findByRequesterId(user.getId());
    }

    public BloodRequest updateStatus(String id, String status) {
        BloodRequest request = requestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus(BloodRequest.RequestStatus.valueOf(status));
        return requestRepository.save(request);
    }
}
