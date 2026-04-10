package com.dimonium239.grammargrinder.practice;

import android.content.Context;

import com.dimonium239.grammargrinder.db.QuestionRepository;

import java.util.List;

public class QuestionLoader {

    public static List<Question> loadQuestions(Context context, String section, List<String> topics) {
        return QuestionRepository.loadQuestions(context, section, topics);
    }

    public static List<Question> loadQuestionsMix(Context context, List<String> selectedTopics) {
        return QuestionRepository.loadQuestionsMix(context, selectedTopics);
    }

    public static List<String> listTopicIds(Context context, String section) {
        return QuestionRepository.listTopicIds(context, section);
    }

    public static void recordQuestionShown(Context context, String questionText) {
        QuestionRepository.recordQuestionShown(context, questionText);
    }

    public static void recordAnswerResult(Context context, String questionText, boolean isCorrect) {
        QuestionRepository.recordAnswerResult(context, questionText, isCorrect);
    }
}
