package com.dimonium239.grammargrinder.db;

import androidx.annotation.NonNull;

public class TopicProgressRow {
    @NonNull
    public String sectionId;
    @NonNull
    public String topicId;
    public int seen;
    public int successful;
    public int unsuccessful;
}
