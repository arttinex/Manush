package com.manush.common.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "manush.ratelimit")
public class RateLimitProperties {
    private BucketConfig login = new BucketConfig(5, 1, TimeUnit.MINUTES);
    private BucketConfig register = new BucketConfig(5, 10, TimeUnit.MINUTES);
    private BucketConfig post = new BucketConfig(10, 1, TimeUnit.MINUTES);
    private BucketConfig comment = new BucketConfig(30, 1, TimeUnit.MINUTES);
    private BucketConfig message = new BucketConfig(60, 1, TimeUnit.MINUTES);
    private BucketConfig ai = new BucketConfig(20, 1, TimeUnit.MINUTES);
    private BucketConfig bloodRequest = new BucketConfig(5, 1, TimeUnit.HOURS);

    @Data
    public static class BucketConfig {
        private int capacity;
        private int refillAmount;
        private TimeUnit refillUnit;

        public BucketConfig() {}
        public BucketConfig(int capacity, int refillAmount, TimeUnit refillUnit) {
            this.capacity = capacity;
            this.refillAmount = refillAmount;
            this.refillUnit = refillUnit;
        }
    }

    public enum TimeUnit { MINUTES, HOURS }
}
