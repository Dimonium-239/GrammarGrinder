package com.dimonium239.grammargrinder.db;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProgressService {
    private ProgressService() {
    }

    @NonNull
    public static TopicProgress getGlobalProgress(Context context) {
        ProgressStatsRow row = getDao(context).loadGlobalProgress();
        return fromStats(row);
    }

    @NonNull
    public static Map<String, TopicProgress> getTopicProgressMap(Context context) {
        List<TopicProgressRow> rows = getDao(context).loadAllTopicProgressRows();
        Map<String, TopicProgress> result = new HashMap<>();
        for (TopicProgressRow row : rows) {
            String key = row.sectionId + "/" + row.topicId;
            result.put(key, fromCounts(row.seen, row.successful, row.unsuccessful));
        }
        return result;
    }

    private static QuestionDao getDao(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        QuestionDao dao = db.questionDao();
        DatabaseSeeder.ensureSeeded(context.getApplicationContext(), dao);
        return dao;
    }

    @NonNull
    private static TopicProgress fromStats(ProgressStatsRow row) {
        if (row == null) {
            return empty();
        }
        return fromCounts(row.seen, row.successful, row.unsuccessful);
    }

    @NonNull
    private static TopicProgress fromCounts(int seen, int successful, int unsuccessful) {
        int safeSeen = Math.max(0, seen);
        int safeSuccessful = Math.max(0, successful);
        int safeUnsuccessful = Math.max(0, unsuccessful);
        float accuracy = safeSeen > 0 ? (safeSuccessful * 100f) / safeSeen : 0f;
        return new TopicProgress(safeSeen, safeSuccessful, safeUnsuccessful, accuracy);
    }

    @NonNull
    private static TopicProgress empty() {
        return new TopicProgress(0, 0, 0, 0f);
    }
}
