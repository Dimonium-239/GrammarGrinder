package com.dimonium239.grammargrinder.db;

import android.content.Context;

import com.dimonium239.grammargrinder.practice.Question;

import java.util.ArrayList;
import java.util.List;

public final class QuestionRepository {
    private static final int MIN_COMPLEXITY = 1;
    private static final int MAX_COMPLEXITY = 5;

    private QuestionRepository() {
    }

    public static List<Question> loadQuestions(Context context, String sectionId, List<String> topicIds) {
        if (sectionId == null || sectionId.isEmpty() || topicIds == null || topicIds.isEmpty()) {
            return new ArrayList<>();
        }
        QuestionDao dao = getDao(context);
        return dao.loadForSectionTopics(sectionId, topicIds);
    }

    public static List<Question> loadQuestionsMix(Context context, List<String> topicPaths) {
        if (topicPaths == null || topicPaths.isEmpty()) {
            return new ArrayList<>();
        }
        QuestionDao dao = getDao(context);
        return dao.loadForTopicPaths(topicPaths);
    }

    public static List<String> listTopicIds(Context context, String sectionId) {
        if (sectionId == null || sectionId.isEmpty()) {
            return new ArrayList<>();
        }
        QuestionDao dao = getDao(context);
        return dao.listTopicIdsBySection(sectionId);
    }

    public static void recordQuestionShown(Context context, String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return;
        }
        QuestionDao dao = getDao(context);
        long now = System.currentTimeMillis();
        QuestionProgressEntity progress = getOrCreateProgress(dao, questionText, now);
        progress.lastSeen = now;
        dao.upsertProgress(progress);
    }

    public static void recordAnswerResult(Context context, String questionText, boolean isCorrect) {
        if (questionText == null || questionText.isEmpty()) {
            return;
        }
        QuestionDao dao = getDao(context);
        long now = System.currentTimeMillis();
        QuestionProgressEntity progress = getOrCreateProgress(dao, questionText, now);

        if (isCorrect) {
            if (progress.mistakeCount > 0) {
                progress.mistakeCount -= 1;
            }
        } else {
            progress.mistakeCount += 1;
        }

        progress.lastSeen = now;
        dao.upsertProgress(progress);
        dao.updateComplexity(questionText, deriveComplexity(progress.mistakeCount));
    }

    public static int deriveComplexity(int mistakeCount) {
        int clampedMistakes = Math.max(0, mistakeCount);
        return Math.min(MAX_COMPLEXITY, MIN_COMPLEXITY + (clampedMistakes / 3));
    }

    private static QuestionProgressEntity getOrCreateProgress(QuestionDao dao, String questionText, long now) {
        QuestionProgressEntity progress = dao.getProgress(questionText);
        if (progress != null) {
            return progress;
        }

        QuestionProgressEntity created = new QuestionProgressEntity();
        created.questionText = questionText;
        created.lastSeen = now;
        created.mistakeCount = 0;
        return created;
    }

    private static QuestionDao getDao(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        QuestionDao dao = db.questionDao();
        DatabaseSeeder.ensureSeeded(context.getApplicationContext(), dao);
        return dao;
    }
}
