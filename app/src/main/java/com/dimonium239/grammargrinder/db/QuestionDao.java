package com.dimonium239.grammargrinder.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.dimonium239.grammargrinder.practice.Question;

import java.util.List;

@Dao
public interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertQuestion(QuestionEntity question);

    @Query("UPDATE questions SET " +
            "section_id = :sectionId, " +
            "topic_id = :topicId, " +
            "category = :category, " +
            "complexity = :complexity, " +
            "options_json = :optionsJson, " +
            "correct_answer = :correctAnswer, " +
            "is_active = 1 " +
            "WHERE question_text = :questionText")
    int updateQuestionByText(
            String questionText,
            String sectionId,
            String topicId,
            String category,
            int complexity,
            String optionsJson,
            String correctAnswer
    );

    @Query("UPDATE questions SET is_active = 0")
    void markAllQuestionsInactive();

    @Query("SELECT COUNT(*) FROM questions")
    int countQuestions();

    @Query("SELECT q.question_text AS id, q.category AS category, q.section_id AS sectionId, q.topic_id AS topicId, " +
            "q.complexity AS complexity, q.question_text AS question, q.options_json AS options, " +
            "q.correct_answer AS correctAnswer, COALESCE(p.last_seen, 0) AS lastSeen, " +
            "COALESCE(p.mistake_count, 0) AS mistakeCount " +
            "FROM questions q " +
            "LEFT JOIN question_progress p ON p.question_text = q.question_text " +
            "WHERE q.is_active = 1 AND q.section_id = :sectionId AND q.topic_id IN (:topicIds) " +
            "ORDER BY COALESCE(p.mistake_count, 0) DESC, q.complexity DESC, COALESCE(p.last_seen, 0) ASC, RANDOM()")
    List<Question> loadForSectionTopics(String sectionId, List<String> topicIds);

    @Query("SELECT q.question_text AS id, q.category AS category, q.section_id AS sectionId, q.topic_id AS topicId, " +
            "q.complexity AS complexity, q.question_text AS question, q.options_json AS options, " +
            "q.correct_answer AS correctAnswer, COALESCE(p.last_seen, 0) AS lastSeen, " +
            "COALESCE(p.mistake_count, 0) AS mistakeCount " +
            "FROM questions q " +
            "LEFT JOIN question_progress p ON p.question_text = q.question_text " +
            "WHERE q.is_active = 1 AND (q.section_id || '/' || q.topic_id) IN (:topicPaths) " +
            "ORDER BY COALESCE(p.mistake_count, 0) DESC, q.complexity DESC, COALESCE(p.last_seen, 0) ASC, RANDOM()")
    List<Question> loadForTopicPaths(List<String> topicPaths);

    @Query("SELECT DISTINCT topic_id FROM questions WHERE section_id = :sectionId AND is_active = 1 ORDER BY topic_id")
    List<String> listTopicIdsBySection(String sectionId);

    @Query("SELECT * FROM question_progress WHERE question_text = :questionText LIMIT 1")
    QuestionProgressEntity getProgress(String questionText);

    @Query("UPDATE questions SET complexity = :complexity WHERE question_text = :questionText")
    void updateComplexity(String questionText, int complexity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertProgress(QuestionProgressEntity progress);
}
