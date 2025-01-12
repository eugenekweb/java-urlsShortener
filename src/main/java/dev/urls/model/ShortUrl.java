package dev.urls.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShortUrl {
    @NonNull private long id;
    @NonNull private String originalUrl;
    @NonNull @Setter(AccessLevel.NONE) private String shortPath;
    @NonNull private UUID userUuid;
    @NonNull @Setter(AccessLevel.NONE) private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int clicksLimit;
    private int clicksCounter;
    private boolean isActive = true;
}
