package dev.urls.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShortUrl {
    private long id;
    private String originalUrl;
    private String shortPath;
    private UUID userUuid;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int clicksLimit;
    private int clicksCounter;
    private boolean isActive;
}
