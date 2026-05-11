package org.finflow.commons.event;

import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all system events to ensure consistency in event auditing.
 */
@Getter
public abstract class BaseEvent {
    private final UUID eventId = UUID.randomUUID();
    private final Instant timestamp = Instant.now();
}
