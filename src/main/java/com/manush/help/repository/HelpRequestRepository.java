package com.manush.help.repository;

import com.manush.help.model.HelpRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpRequestRepository extends MongoRepository<HelpRequest, String> {
    List<HelpRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);
    List<HelpRequest> findByRequesterId(String requesterId);
    List<HelpRequest> findByHelperIdAndStatusOrderByCreatedAtDesc(
            String helperId, HelpRequest.HelpStatus status);
    
    Page<HelpRequest> findByCategory(String category, Pageable pageable);
    Page<HelpRequest> findByUrgency(HelpRequest.Urgency urgency, Pageable pageable);
    Page<HelpRequest> findByStatus(HelpRequest.HelpStatus status, Pageable pageable);
    Page<HelpRequest> findByCategoryAndUrgency(String category, HelpRequest.Urgency urgency, Pageable pageable);
    Page<HelpRequest> findByLocationNear(Point location, Distance distance, Pageable pageable);
    Page<HelpRequest> findByCategoryAndLocationNear(String category, Point location, Distance distance, Pageable pageable);
}
