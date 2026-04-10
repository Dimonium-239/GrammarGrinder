package com.dimonium239.grammargrinder.db;

import android.content.Context;

import com.dimonium239.grammargrinder.practice.Question;
import com.dimonium239.grammargrinder.practice.QuestionAssetStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestionRepository {
    private static final int MIN_COMPLEXITY = 1;
    private static final int MAX_COMPLEXITY = 5;

    private QuestionRepository() {
    }

    public static List<Question> loadQuestions(Context context, String section, List<String> topics) {
        if (section == null || section.isEmpty() || topics == null || topics.isEmpty()) {
            return new ArrayList<>();
        }
        List<Question> questions = QuestionAssetStore.loadQuestions(context, section, topics);
        applyProgress(context, questions);
        return questions;
    }

    public static List<Question> loadQuestionsMix(Context context, List<String> topicPaths) {
        if (topicPaths == null || topicPaths.isEmpty()) {
            return new ArrayList<>();
        }
        List<Question> questions = QuestionAssetStore.loadQuestionsMix(context, topicPaths);
        applyProgress(context, questions);
        return questions;
    }

    public static List<String> listTopicIds(Context context, String section) {
        if (section == null || section.isEmpty()) {
            return new ArrayList<>();
        }
        return QuestionAssetStore.listTopicIds(context, section);
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
    }

    public static int deriveComplexity(int mistakeCount) {
        int clampedMistakes = Math.max(0, mistakeCount);
        return Math.min(MAX_COMPLEXITY, MIN_COMPLEXITY + (clampedMistakes / 3));
    }

    private static void applyProgress(Context context, List<Question> questions) {
        if (questions.isEmpty()) {
            return;
        }

        QuestionDao dao = getDao(context);
        List<QuestionProgressEntity> progressRows = dao.loadAllProgress();
        Map<String, QuestionProgressEntity> progressByQuestion = new HashMap<>(progressRows.size());
        for (QuestionProgressEntity progressRow : progressRows) {
            progressByQuestion.put(progressRow.questionText, progressRow);
        }

        for (Question question : questions) {
            QuestionProgressEntity progress = progressByQuestion.get(question.question);
            if (progress == null) {
                question.lastSeen = 0L;
                question.mistakeCount = 0;
                continue;
            }

            question.lastSeen = progress.lastSeen;
            question.mistakeCount = progress.mistakeCount;
            if (progress.lastSeen > 0L || progress.mistakeCount > 0) {
                question.complexity = deriveComplexity(progress.mistakeCount);
            }
        }

        Collections.shuffle(questions);
        questions.sort((left, right) -> {
            int mistakesComparison = Integer.compare(right.mistakeCount, left.mistakeCount);
            if (mistakesComparison != 0) {
                return mistakesComparison;
            }

            int complexityComparison = Integer.compare(right.complexity, left.complexity);
            if (complexityComparison != 0) {
                return complexityComparison;
            }

            return Long.compare(left.lastSeen, right.lastSeen);
        });
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
        return db.questionDao();
    }
}
