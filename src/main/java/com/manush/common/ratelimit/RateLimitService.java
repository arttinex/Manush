package com.manush.common.ratelimit;

import com.manush.common.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties props;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkLogin(String ip, String email) {
        consume("login:" + ip + ":" + email, props.getLogin());
    }

    public void checkRegister(String ip) {
        consume("register:" + ip, props.getRegister());
    }

    public void checkPost(String userId) {
        consume("post:" + userId, props.getPost());
    }

    public void checkComment(String userId) {
        consume("comment:" + userId, props.getComment());
    }

    public void checkMessage(String userId) {
        consume("message:" + userId, props.getMessage());
    }

    public void checkAi(String userId) {
        consume("ai:" + userId, props.getAi());
    }

    public void checkBloodRequest(String userId) {
        consume("blood:" + userId, props.getBloodRequest());
    }

    public void checkRateLimit(String key, int capacity, int refillMinutes) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> {
            Duration period = Duration.ofMinutes(refillMinutes);
            Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period));
            return Bucket.builder().addLimit(limit).build();
        });
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException();
        }
    }

    private void consume(String key, RateLimitProperties.BucketConfig config) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(config));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException();
        }
    }

    private Bucket buildBucket(RateLimitProperties.BucketConfig config) {
        Duration period = config.getRefillUnit() == RateLimitProperties.TimeUnit.HOURS
                ? Duration.ofHours(config.getRefillAmount())
                : Duration.ofMinutes(config.getRefillAmount());
        Bandwidth limit = Bandwidth.classic(
                config.getCapacity(),
                Refill.greedy(config.getCapacity(), period));
        return Bucket.builder().addLimit(limit).build();
    }
}
