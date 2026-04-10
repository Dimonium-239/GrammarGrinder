package com.dimonium239.grammargrinder.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                QuestionProgressEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract QuestionDao questionDao();

    private static volatile AppDatabase INSTANCE;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `questions_new` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`section_id` TEXT NOT NULL, " +
                    "`topic_id` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`complexity` INTEGER NOT NULL, " +
                    "`options_json` TEXT NOT NULL, " +
                    "`correct_answer` TEXT NOT NULL, " +
                    "`explanation` TEXT NOT NULL, " +
                    "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                    "PRIMARY KEY(`question_text`))");

            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress_backup` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`priority` INTEGER NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_text`))");

            db.execSQL("INSERT OR REPLACE INTO questions_new(" +
                    "question_text, section_id, topic_id, category, complexity, options_json, correct_answer, explanation, is_active) " +
                    "SELECT question_text, section_id, topic_id, category, complexity, options_json, correct_answer, explanation, 1 " +
                    "FROM questions");

            db.execSQL("INSERT OR REPLACE INTO question_progress_backup(question_text, priority, last_seen, mistake_count) " +
                    "SELECT q.question_text, p.priority, p.last_seen, p.mistake_count " +
                    "FROM question_progress p " +
                    "INNER JOIN questions q ON q.id = p.question_id");

            db.execSQL("DROP TABLE question_progress");
            db.execSQL("DROP TABLE questions");
            db.execSQL("ALTER TABLE questions_new RENAME TO questions");
            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`priority` INTEGER NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_text`), " +
                    "FOREIGN KEY(`question_text`) REFERENCES `questions`(`question_text`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT OR REPLACE INTO question_progress(question_text, priority, last_seen, mistake_count) " +
                    "SELECT b.question_text, b.priority, b.last_seen, b.mistake_count " +
                    "FROM question_progress_backup b " +
                    "INNER JOIN questions q ON q.question_text = b.question_text");
            db.execSQL("DROP TABLE question_progress_backup");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_questions_question_text` ON `questions` (`question_text`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_section_id_topic_id` ON `questions` (`section_id`, `topic_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_topic_id` ON `questions` (`topic_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_is_active` ON `questions` (`is_active`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_question_text` ON `question_progress` (`question_text`)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `questions_new` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`section_id` TEXT NOT NULL, " +
                    "`topic_id` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`complexity` INTEGER NOT NULL, " +
                    "`options_json` TEXT NOT NULL, " +
                    "`correct_answer` TEXT NOT NULL, " +
                    "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                    "PRIMARY KEY(`question_text`))");

            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress_backup` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_text`))");

            db.execSQL("INSERT OR REPLACE INTO questions_new(" +
                    "question_text, section_id, topic_id, category, complexity, options_json, correct_answer, is_active) " +
                    "SELECT question_text, section_id, topic_id, category, complexity, options_json, correct_answer, is_active " +
                    "FROM questions");

            db.execSQL("INSERT OR REPLACE INTO question_progress_backup(question_text, last_seen, mistake_count) " +
                    "SELECT question_text, COALESCE(last_seen, 0), COALESCE(mistake_count, 0) FROM question_progress");

            db.execSQL("DROP TABLE question_progress");
            db.execSQL("DROP TABLE questions");
            db.execSQL("ALTER TABLE questions_new RENAME TO questions");
            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_text`), " +
                    "FOREIGN KEY(`question_text`) REFERENCES `questions`(`question_text`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("INSERT OR REPLACE INTO question_progress(question_text, last_seen, mistake_count) " +
                    "SELECT b.question_text, b.last_seen, b.mistake_count " +
                    "FROM question_progress_backup b " +
                    "INNER JOIN questions q ON q.question_text = b.question_text");
            db.execSQL("DROP TABLE question_progress_backup");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_questions_question_text` ON `questions` (`question_text`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_section_id_topic_id` ON `questions` (`section_id`, `topic_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_topic_id` ON `questions` (`topic_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_is_active` ON `questions` (`is_active`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_question_text` ON `question_progress` (`question_text`)");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `questions_new` (" +
                    "`id` TEXT NOT NULL, " +
                    "`question_text` TEXT NOT NULL, " +
                    "`section_id` TEXT NOT NULL, " +
                    "`topic_id` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, " +
                    "`complexity` INTEGER NOT NULL, " +
                    "`options_json` TEXT NOT NULL, " +
                    "`correct_answer` TEXT NOT NULL, " +
                    "`is_active` INTEGER NOT NULL DEFAULT 1, " +
                    "PRIMARY KEY(`id`))");

            db.execSQL("INSERT OR REPLACE INTO questions_new(" +
                    "id, question_text, section_id, topic_id, category, complexity, options_json, correct_answer, is_active) " +
                    "SELECT section_id || '/' || topic_id || '/' || question_text, " +
                    "question_text, section_id, topic_id, category, complexity, options_json, correct_answer, is_active " +
                    "FROM questions");

            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress_new` (" +
                    "`question_id` TEXT NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_id`), " +
                    "FOREIGN KEY(`question_id`) REFERENCES `questions_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");

            db.execSQL("INSERT OR REPLACE INTO question_progress_new(question_id, last_seen, mistake_count) " +
                    "SELECT q.id, p.last_seen, p.mistake_count " +
                    "FROM question_progress p " +
                    "INNER JOIN questions_new q ON q.question_text = p.question_text");

            db.execSQL("DROP TABLE question_progress");
            db.execSQL("DROP TABLE questions");
            db.execSQL("ALTER TABLE questions_new RENAME TO questions");
            db.execSQL("ALTER TABLE question_progress_new RENAME TO question_progress");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_question_text` ON `questions` (`question_text`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_section_id_topic_id` ON `questions` (`section_id`, `topic_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_topic_id` ON `questions` (`topic_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_is_active` ON `questions` (`is_active`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_question_id` ON `question_progress` (`question_id`)");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `questions_new` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`section` TEXT NOT NULL, " +
                    "`topic` TEXT NOT NULL, " +
                    "`complexity` INTEGER NOT NULL, " +
                    "`options_json` TEXT NOT NULL, " +
                    "`correct_answer` TEXT NOT NULL, " +
                    "PRIMARY KEY(`question_text`))");

            db.execSQL("INSERT OR REPLACE INTO questions_new(" +
                    "question_text, section, topic, complexity, options_json, correct_answer) " +
                    "SELECT question_text, section_id, topic_id, complexity, options_json, correct_answer " +
                    "FROM questions " +
                    "ORDER BY rowid");

            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress_new` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_text`), " +
                    "FOREIGN KEY(`question_text`) REFERENCES `questions_new`(`question_text`) ON UPDATE NO ACTION ON DELETE CASCADE)");

            db.execSQL("INSERT OR REPLACE INTO question_progress_new(question_text, last_seen, mistake_count) " +
                    "SELECT q.question_text, MAX(p.last_seen), MAX(p.mistake_count) " +
                    "FROM question_progress p " +
                    "INNER JOIN questions q ON q.id = p.question_id " +
                    "GROUP BY q.question_text");

            db.execSQL("DROP TABLE question_progress");
            db.execSQL("DROP TABLE questions");
            db.execSQL("ALTER TABLE questions_new RENAME TO questions");
            db.execSQL("ALTER TABLE question_progress_new RENAME TO question_progress");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_section_topic` ON `questions` (`section`, `topic`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_topic` ON `questions` (`topic`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_question_text` ON `question_progress` (`question_text`)");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `question_progress_new` (" +
                    "`question_text` TEXT NOT NULL, " +
                    "`last_seen` INTEGER NOT NULL, " +
                    "`mistake_count` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`question_text`))");

            db.execSQL("INSERT OR REPLACE INTO question_progress_new(question_text, last_seen, mistake_count) " +
                    "SELECT question_text, COALESCE(last_seen, 0), COALESCE(mistake_count, 0) " +
                    "FROM question_progress");

            db.execSQL("DROP TABLE question_progress");
            db.execSQL("DROP TABLE IF EXISTS questions");
            db.execSQL("ALTER TABLE question_progress_new RENAME TO question_progress");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_progress_question_text` ON `question_progress` (`question_text`)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "grammar_grinder.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
