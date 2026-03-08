package com.dimonium239.grammargrinder.db;

public final class TopicProgress {
    private final int seen;
    private final int successful;
    private final int unsuccessful;
    private final float accuracyPercent;

    public TopicProgress(int seen, int successful, int unsuccessful, float accuracyPercent) {
        this.seen = seen;
        this.successful = successful;
        this.unsuccessful = unsuccessful;
        this.accuracyPercent = accuracyPercent;
    }

    public int getSeen() {
        return seen;
    }

    public float getAccuracyPercent() {
        return accuracyPercent;
    }
}
