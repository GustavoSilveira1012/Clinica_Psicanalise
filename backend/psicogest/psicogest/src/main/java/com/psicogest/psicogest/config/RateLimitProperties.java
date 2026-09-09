package com.psicogest.psicogest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.security.rate-limit")
public class RateLimitProperties {

    private Bucket loginIp;
    private Bucket loginAccount;
    private Bucket refreshIp;
    private Bucket mfaIp;
    private Bucket mfaChallenge;

    public static class Bucket {
        private int capacity;
        private Duration refillPeriod;

        public Bucket() {}

        public Bucket(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }
    }

    public Bucket getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(Bucket loginIp) {
        this.loginIp = loginIp;
    }

    public Bucket getLoginAccount() {
        return loginAccount;
    }

    public void setLoginAccount(Bucket loginAccount) {
        this.loginAccount = loginAccount;
    }

    public Bucket getRefreshIp() {
        return refreshIp;
    }

    public void setRefreshIp(Bucket refreshIp) {
        this.refreshIp = refreshIp;
    }

    public Bucket getMfaIp() {
        return mfaIp;
    }

    public void setMfaIp(Bucket mfaIp) {
        this.mfaIp = mfaIp;
    }

    public Bucket getMfaChallenge() {
        return mfaChallenge;
    }

    public void setMfaChallenge(Bucket mfaChallenge) {
        this.mfaChallenge = mfaChallenge;
    }
}
