package com.dimonium239.grammargrinder.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "question_progress",
        foreignKeys = @ForeignKey(
                entity = QuestionEntity.class,
                parentColumns = "question_text",
                childColumns = "question_text",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"question_text"})
        }
)
public class QuestionProgressEntity {
    @PrimaryKey
    @ColumnInfo(name = "question_text")
    @NonNull
    public String questionText;

    @ColumnInfo(name = "last_seen")
    public long lastSeen;

    @ColumnInfo(name = "mistake_count")
    public int mistakeCount;
}
