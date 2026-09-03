package com.alfa.device_ctrl;

/** Evidence-backed resource alert state; UNKNOWN never authorizes a process operation. */
public final class ResourceAlertState {
    public enum Level { UNKNOWN, INFO, WARNING, CRITICAL }

    private final Level level;
    private final String evidenceId;
    private final String source;
    private final long sampledAtEpochMs;
    private final Double cpuPercent;

    private ResourceAlertState(Level level, String evidenceId, String source, long sampledAtEpochMs, Double cpuPercent) {
        this.level = level;
        this.evidenceId = evidenceId;
        this.source = source;
        this.sampledAtEpochMs = sampledAtEpochMs;
        this.cpuPercent = cpuPercent;
    }

    public static ResourceAlertState unknown() {
        return new ResourceAlertState(Level.UNKNOWN, "", "", 0L, null);
    }

    public static ResourceAlertState verifiedCritical(String evidenceId, String source, long sampledAtEpochMs, double cpuPercent) {
        if (evidenceId == null || evidenceId.trim().isEmpty() || source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("critical alert requires evidence identity and source");
        }
        if (cpuPercent < 0.0 || cpuPercent > 100.0 || sampledAtEpochMs <= 0L) {
            throw new IllegalArgumentException("critical alert sample is invalid");
        }
        return new ResourceAlertState(Level.CRITICAL, evidenceId, source, sampledAtEpochMs, cpuPercent);
    }

    public Level level() { return level; }
    public String evidenceId() { return evidenceId; }
    public String source() { return source; }
    public long sampledAtEpochMs() { return sampledAtEpochMs; }
    public Double cpuPercent() { return cpuPercent; }

    public boolean canOfferKillProcess() {
        return level == Level.CRITICAL && !evidenceId.isEmpty() && !source.isEmpty() && sampledAtEpochMs > 0L && cpuPercent != null;
    }

    public String displayMessage() {
        if (level == Level.UNKNOWN) return "Data resource belum tersedia (UNKNOWN).";
        if (cpuPercent == null) return "Data resource tersedia tanpa CPU sample.";
        return String.format("CPU %.1f%% — sumber %s", cpuPercent, source);
    }
}
