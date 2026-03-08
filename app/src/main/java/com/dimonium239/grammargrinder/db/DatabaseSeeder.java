package com.dimonium239.grammargrinder.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.security.MessageDigest;

public final class DatabaseSeeder {
    private static final String TAG = "DatabaseSeeder";
    private static final String PREFS_NAME = "db_seed_prefs";
    private static final String KEY_CONTENT_VERSION = "content_version";
    private static final String KEY_CONTENT_FINGERPRINT = "content_fingerprint";

    // Bump when bundled question content changes.
    private static final int CONTENT_VERSION = 3;

    private DatabaseSeeder() {
    }

    public static synchronized void ensureSeeded(Context context, QuestionDao dao) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int seededVersion = prefs.getInt(KEY_CONTENT_VERSION, 0);
        boolean hasData = dao.countQuestions() > 0;

        // Avoid parsing and hashing the full asset corpus on routine screen loads.
        // CONTENT_VERSION is the authoritative invalidation key for bundled content.
        if (hasData && seededVersion >= CONTENT_VERSION) {
            return;
        }

        List<QuestionEntity> questions = readQuestionsFromAssets(context);
        if (questions.isEmpty()) {
            return;
        }
        String fingerprint = computeFingerprint(questions);

        dao.markAllQuestionsInactive();
        for (QuestionEntity question : questions) {
            int updated = dao.updateQuestionByText(
                    question.question,
                    question.sectionId,
                    question.topicId,
                    question.category,
                    question.complexity,
                    Converters.fromStringList(question.options),
                    question.correctAnswer
            );
            if (updated == 0) {
                long inserted = dao.insertQuestion(question);
                if (inserted == -1L) {
                    // Row was inserted by another path/thread; update to current seed values.
                    dao.updateQuestionByText(
                            question.question,
                            question.sectionId,
                            question.topicId,
                            question.category,
                            question.complexity,
                            Converters.fromStringList(question.options),
                            question.correctAnswer
                    );
                }
            }
        }
        prefs.edit()
                .putInt(KEY_CONTENT_VERSION, CONTENT_VERSION)
                .putString(KEY_CONTENT_FINGERPRINT, fingerprint)
                .apply();
    }

    private static List<QuestionEntity> readQuestionsFromAssets(Context context) {
        List<QuestionEntity> result = new ArrayList<>();
        try {
            AssetManager am = context.getAssets();
            String[] topLevel = am.list("");
            if (topLevel == null) {
                return result;
            }
            Arrays.sort(topLevel);

            for (String sectionId : topLevel) {
                String[] files = am.list(sectionId);
                if (files == null) {
                    continue;
                }
                Arrays.sort(files);

                for (String file : files) {
                    if (!file.endsWith(".json")) {
                        continue;
                    }

                    String topicId = file.substring(0, file.length() - 5);
                    String path = sectionId + "/" + file;
                    parseQuestionFile(am, path, sectionId, topicId, result);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error reading bundled question assets", e);
        }
        return result;
    }

    private static void parseQuestionFile(
            AssetManager am,
            String path,
            String sectionId,
            String topicId,
            List<QuestionEntity> out
    ) {
        try {
            String json = readAsset(am, path);
            JSONArray arr = new JSONArray(json);
            String category = formatTopic(topicId);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!o.has("question") || !o.has("options") || !o.has("correctAnswer")) {
                    continue;
                }

                String question = o.getString("question");
                String correctAnswer = o.getString("correctAnswer");
                List<String> options = toList(o.getJSONArray("options"));
                if (options.isEmpty()) {
                    continue;
                }

                QuestionEntity entity = new QuestionEntity();
                entity.sectionId = sectionId;
                entity.topicId = topicId;
                entity.category = o.optString("category", category);
                entity.complexity = o.optInt("complexity", 1);
                entity.question = question;
                entity.options = options;
                entity.correctAnswer = correctAnswer;
                entity.isActive = 1;
                out.add(entity);
            }
        } catch (Exception ignored) {
            // Ignore non-question JSON files (for example guides metadata).
        }
    }

    private static String readAsset(AssetManager am, String path) throws Exception {
        InputStream is = am.open(path);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    private static List<String> toList(JSONArray arr) throws Exception {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(arr.getString(i));
        }
        return list;
    }

    private static String formatTopic(String raw) {
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String computeFingerprint(List<QuestionEntity> questions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (QuestionEntity question : questions) {
                updateDigest(digest, question.sectionId);
                updateDigest(digest, question.topicId);
                updateDigest(digest, question.category);
                updateDigest(digest, String.valueOf(question.complexity));
                updateDigest(digest, question.question);
                updateDigest(digest, question.correctAnswer);
                if (question.options != null) {
                    for (String option : question.options) {
                        updateDigest(digest, option);
                    }
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // Safe fallback: if hashing fails, force reseed by returning non-persisted marker.
            return String.valueOf(System.currentTimeMillis());
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        String safeValue = value == null ? "" : value;
        digest.update(safeValue.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

}
