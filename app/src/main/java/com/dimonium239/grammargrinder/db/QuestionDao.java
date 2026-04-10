package com.dimonium239.grammargrinder.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuestionDao {

    @Query("SELECT * FROM question_progress WHERE question_text = :questionText LIMIT 1")
    QuestionProgressEntity getProgress(String questionText);

    @Query("SELECT * FROM question_progress")
    List<QuestionProgressEntity> loadAllProgress();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertProgress(QuestionProgressEntity progress);
}
