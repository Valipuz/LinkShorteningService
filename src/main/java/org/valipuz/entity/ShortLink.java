package org.valipuz.entity;

import java.util.UUID;

public class ShortLink {
    private final String code;
    private final String originalUrl;
    private final UUID ownerId;
    private final long expiresAtMillis;
    private int limitClicks;
    private int clicks;

    public ShortLink(String code, String originalUrl, UUID ownerId, long expiresAtMillis, int limitClicks) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.ownerId = ownerId;
        this.expiresAtMillis = expiresAtMillis;
        this.limitClicks = limitClicks;
        this.clicks = 0;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }

    public boolean isClickLimitsReached() {
        return clicks >= limitClicks;
    }

    public void registerClick() {
        clicks++;
    }

    public String getCode() {
        return code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public void setLimitClicks(int limitClicks) {
        this.limitClicks = limitClicks;
    }

    public int getLimitClicks() {
        return limitClicks;
    }

    public int getClicks() {
        return clicks;
    }
}
