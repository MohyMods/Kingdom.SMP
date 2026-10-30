package com.mohymods.kingdomsmp.client;

/**
 * Single-flight request lifecycle with no Minecraft dependencies.
 *
 * <p>Use on the client thread and retain one instance across connections. Time
 * arguments use the same monotonic nanosecond clock (normally System.nanoTime).
 * Connection and screen values are compared by reference identity, not equals.
 * A null origin screen is valid; a null connection is not.</p>
 */
public final class OverviewRequestTracker {
    public enum Status { IDLE, WAITING, CANCELLED, TIMED_OUT }

    private final long timeoutNanos;
    private long sequence;
    private long pendingId;
    private long startedAt;
    private Object connection;
    private Object originScreen;

    public OverviewRequestTracker(long timeoutNanos) {
        if (timeoutNanos <= 0) {
            throw new IllegalArgumentException("Request timeout must be positive");
        }
        this.timeoutNanos = timeoutNanos;
    }

    /**
     * Starts a request and returns its positive ID, or zero if already waiting.
     * Clear or tick a stale request before beginning another. IDs are never reset
     * by clear; exhaustion fails rather than wrapping into an old session's IDs.
     */
    public long begin(long now, Object connectionIdentity, Object originScreen) {
        if (connectionIdentity == null) {
            throw new IllegalArgumentException("A request requires a connection");
        }
        if (waiting()) return 0;
        if (sequence == Long.MAX_VALUE) {
            throw new IllegalStateException("Overview request ID space exhausted");
        }
        pendingId = ++sequence;
        startedAt = now;
        connection = connectionIdentity;
        this.originScreen = originScreen;
        return pendingId;
    }

    /**
     * Consumes only the current request's reply, in its original connection and
     * screen, strictly before timeout. An old ID cannot cancel a newer request.
     * A matching but late/context-invalid reply clears its obsolete request.
     */
    public boolean complete(long id, Object connectionIdentity, Object currentScreen, long now) {
        if (!waiting() || id != pendingId) return false;
        if (tick(now, connectionIdentity, currentScreen) != Status.WAITING) return false;
        clear();
        return true;
    }

    /**
     * Cancels on disconnect/connection replacement/navigation, expires requests,
     * or reports WAITING/IDLE. Cancellation takes precedence over timeout so
     * navigation and disconnect do not produce a misleading timeout message.
     */
    public Status tick(long now, Object connectionIdentity, Object currentScreen) {
        if (!waiting()) return Status.IDLE;
        if (connectionIdentity == null || connectionIdentity != connection || currentScreen != originScreen) {
            clear();
            return Status.CANCELLED;
        }
        // Subtraction handles nanoTime's signed wrap-around for elapsed durations
        // shorter than 2^63 ns; comparing a computed deadline would not.
        if (now - startedAt >= timeoutNanos) {
            clear();
            return Status.TIMED_OUT;
        }
        return Status.WAITING;
    }

    public boolean waiting() {
        return pendingId != 0;
    }

    /** Clears lifecycle references but deliberately retains the ID sequence. */
    public void clear() {
        pendingId = 0;
        startedAt = 0;
        connection = null;
        originScreen = null;
    }
}
