package queuectl.model;

import java.time.OffsetDateTime;

public class Job {
    public String id;
    public String command;
    public String state = "pending";
    public int attempts = 0;
    public int maxRetries = 3;
    public OffsetDateTime createdAt = OffsetDateTime.now();
    public OffsetDateTime updatedAt = OffsetDateTime.now();
    public OffsetDateTime nextRunAt = OffsetDateTime.now();
    public String lastError;
    public String workerId;

    @Override
    public String toString() {
        return String.format("Job[id=%s, command=%s, state=%s]", id, command, state);
    }
}
