package com.dimonium239.grammargrinder.db;

import android.content.Context;

import androidx.annotation.NonNull;

import com.dimonium239.grammargrinder.practice.QuestionAssetStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProgressService {
    private ProgressService() {
    }

    @NonNull
    public static TopicProgress getGlobalProgress(Context context) {
        Map<String, List<String>> questionTextsByTopic = QuestionAssetStore.loadQuestionTextsByTopicPath(context);
        Map<String, QuestionProgressEntity> progressByQuestion = loadProgressMap(context);

        int seen = 0;
        int successful = 0;
        int unsuccessful = 0;

        for (List<String> questionTexts : questionTextsByTopic.values()) {
            Counts counts = countProgress(questionTexts, progressByQuestion);
            seen += counts.seen;
            successful += counts.successful;
            unsuccessful += counts.unsuccessful;
        }

        return fromCounts(seen, successful, unsuccessful);
    }

    @NonNull
    public static Map<String, TopicProgress> getTopicProgressMap(Context context) {
        Map<String, List<String>> questionTextsByTopic = QuestionAssetStore.loadQuestionTextsByTopicPath(context);
        Map<String, QuestionProgressEntity> progressByQuestion = loadProgressMap(context);
        Map<String, TopicProgress> result = new HashMap<>(questionTextsByTopic.size());

        for (Map.Entry<String, List<String>> entry : questionTextsByTopic.entrySet()) {
            Counts counts = countProgress(entry.getValue(), progressByQuestion);
            result.put(entry.getKey(), fromCounts(counts.seen, counts.successful, counts.unsuccessful));
        }

        return result;
    }

    private static Counts countProgress(
            Collection<String> questionTexts,
            Map<String, QuestionProgressEntity> progressByQuestion
    ) {
        Counts counts = new Counts();
        for (String questionText : questionTexts) {
            QuestionProgressEntity progress = progressByQuestion.get(questionText);
            if (progress == null || progress.lastSeen <= 0) {
                continue;
            }

            counts.seen++;
            if (progress.mistakeCount > 0) {
                counts.unsuccessful++;
            } else {
                counts.successful++;
            }
        }
        return counts;
    }

    private static Map<String, QuestionProgressEntity> loadProgressMap(Context context) {
        List<QuestionProgressEntity> rows = getDao(context).loadAllProgress();
        Map<String, QuestionProgressEntity> result = new HashMap<>(rows.size());
        for (QuestionProgressEntity row : rows) {
            result.put(row.questionText, row);
        }
        return result;
    }

    private static QuestionDao getDao(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        return db.questionDao();
    }

    @NonNull
    private static TopicProgress fromCounts(int seen, int successful, int unsuccessful) {
        int safeSeen = Math.max(0, seen);
        int safeSuccessful = Math.max(0, successful);
        int safeUnsuccessful = Math.max(0, unsuccessful);
        float accuracy = safeSeen > 0 ? (safeSuccessful * 100f) / safeSeen : 0f;
        return new TopicProgress(safeSeen, safeSuccessful, safeUnsuccessful, accuracy);
    }

    private static final class Counts {
        int seen;
        int successful;
        int unsuccessful;
    }
}
