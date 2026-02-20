package com.dimonium239.grammargrinder.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(
        tableName = "questions",
        indices = {
                @Index(value = {"question_text"}, unique = true),
                @Index(value = {"section_id", "topic_id"}),
                @Index(value = {"topic_id"}),
                @Index(value = {"is_active"})
        }
)
public class QuestionEntity {
    @PrimaryKey
    @ColumnInfo(name = "question_text")
    @NonNull
    public String question;

    @ColumnInfo(name = "section_id")
    @NonNull
    public String sectionId;

    @ColumnInfo(name = "topic_id")
    @NonNull
    public String topicId;

    @NonNull
    public String category;

    public int complexity;

    @ColumnInfo(name = "options_json")
    @NonNull
    public List<String> options;

    @ColumnInfo(name = "correct_answer")
    @NonNull
    public String correctAnswer;

    @ColumnInfo(name = "is_active")
    public int isActive;
}
