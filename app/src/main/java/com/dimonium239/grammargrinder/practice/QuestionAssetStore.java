package com.dimonium239.grammargrinder.practice;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class QuestionAssetStore {
    private static final String TAG = "QuestionAssetStore";

    private static volatile Map<String, List<String>> questionTextsByTopicPathCache;

    private QuestionAssetStore() {
    }

    public static List<Question> loadQuestions(Context context, String section, List<String> topics) {
        List<String> topicPaths = new ArrayList<>(topics.size());
        for (String topic : topics) {
            if (topic == null || topic.isEmpty()) {
                continue;
            }
            topicPaths.add(section + "/" + topic);
        }
        return loadQuestionsMix(context, topicPaths);
    }

    public static List<Question> loadQuestionsMix(Context context, List<String> topicPaths) {
        if (topicPaths == null || topicPaths.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Question> questionsByText = new LinkedHashMap<>();
        AssetManager assetManager = context.getAssets();
        List<String> sortedTopicPaths = new ArrayList<>(topicPaths);
        sortedTopicPaths.sort(String::compareTo);

        for (String topicPath : sortedTopicPaths) {
            TopicPath parsedPath = TopicPath.parse(topicPath);
            if (parsedPath == null) {
                continue;
            }
            readQuestionFile(
                    assetManager,
                    parsedPath.assetPath(),
                    parsedPath.section,
                    parsedPath.topic,
                    questionsByText
            );
        }
        return new ArrayList<>(questionsByText.values());
    }

    public static List<String> listTopicIds(Context context, String section) {
        List<String> topics = new ArrayList<>();
        try {
            String[] files = context.getAssets().list(section);
            if (files == null) {
                return topics;
            }
            Arrays.sort(files);
            for (String file : files) {
                if (file.endsWith(".json")) {
                    topics.add(file.substring(0, file.length() - 5));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to list topics for section: " + section, e);
        }
        return topics;
    }

    public static Map<String, List<String>> loadQuestionTextsByTopicPath(Context context) {
        Map<String, List<String>> cached = questionTextsByTopicPathCache;
        if (cached != null) {
            return cached;
        }

        synchronized (QuestionAssetStore.class) {
            if (questionTextsByTopicPathCache != null) {
                return questionTextsByTopicPathCache;
            }
            questionTextsByTopicPathCache = buildTopicIndex(context);
            return questionTextsByTopicPathCache;
        }
    }

    private static Map<String, List<String>> buildTopicIndex(Context context) {
        Map<String, LinkedHashSet<String>> questionTextsByTopicPath = new LinkedHashMap<>();
        Map<String, String> topicPathByQuestionText = new LinkedHashMap<>();

        try {
            AssetManager assetManager = context.getAssets();
            String[] topLevelEntries = assetManager.list("");
            if (topLevelEntries == null) {
                return new LinkedHashMap<>();
            }
            Arrays.sort(topLevelEntries);

            for (String section : topLevelEntries) {
                String[] files = assetManager.list(section);
                if (files == null || files.length == 0) {
                    continue;
                }
                Arrays.sort(files);

                for (String file : files) {
                    if (!file.endsWith(".json")) {
                        continue;
                    }

                    String topic = file.substring(0, file.length() - 5);
                    String topicPath = section + "/" + topic;
                    readQuestionTexts(
                            assetManager,
                            section + "/" + file,
                            topicPath,
                            questionTextsByTopicPath,
                            topicPathByQuestionText
                    );
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to build question index from assets", e);
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : questionTextsByTopicPath.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        return result;
    }

    private static void readQuestionFile(
            AssetManager assetManager,
            String assetPath,
            String section,
            String topic,
            Map<String, Question> out
    ) {
        try {
            JSONArray array = new JSONArray(readAsset(assetManager, assetPath));
            for (int i = 0; i < array.length(); i++) {
                JSONObject rawQuestion = array.getJSONObject(i);
                Question question = parseQuestion(rawQuestion, section, topic);
                if (question == null) {
                    continue;
                }
                upsertQuestion(out, question, assetPath);
            }
        } catch (Exception e) {
            Log.w(TAG, "Skipping malformed question asset: " + assetPath, e);
        }
    }

    private static void readQuestionTexts(
            AssetManager assetManager,
            String assetPath,
            String topicPath,
            Map<String, LinkedHashSet<String>> out,
            Map<String, String> topicPathByQuestionText
    ) {
        try {
            JSONArray array = new JSONArray(readAsset(assetManager, assetPath));
            for (int i = 0; i < array.length(); i++) {
                JSONObject rawQuestion = array.getJSONObject(i);
                String questionText = extractQuestionText(rawQuestion);
                if (questionText == null) {
                    continue;
                }
                upsertQuestionText(out, topicPathByQuestionText, topicPath, questionText);
            }
        } catch (Exception e) {
            Log.w(TAG, "Skipping malformed question asset: " + assetPath, e);
        }
    }

    static void upsertQuestion(Map<String, Question> questionsByText, Question question, String assetPath) {
        Question existing = questionsByText.remove(question.question);
        if (existing != null) {
            Log.w(TAG, "Replacing duplicate question text in assets: " + question.question + " (" + assetPath + ")");
        }
        questionsByText.put(question.question, question);
    }

    static void upsertQuestionText(
            Map<String, LinkedHashSet<String>> questionTextsByTopicPath,
            Map<String, String> topicPathByQuestionText,
            String topicPath,
            String questionText
    ) {
        String previousTopicPath = topicPathByQuestionText.put(questionText, topicPath);
        if (previousTopicPath != null) {
            LinkedHashSet<String> previousQuestions = questionTextsByTopicPath.get(previousTopicPath);
            if (previousQuestions != null) {
                previousQuestions.remove(questionText);
            }
            Log.w(TAG, "Replacing duplicate question text in assets: " + questionText + " (" + topicPath + ")");
        }

        LinkedHashSet<String> questionTexts =
                questionTextsByTopicPath.computeIfAbsent(topicPath, key -> new LinkedHashSet<>());
        questionTexts.remove(questionText);
        questionTexts.add(questionText);
    }

    private static Question parseQuestion(JSONObject rawQuestion, String section, String topic) throws Exception {
        String questionText = extractQuestionText(rawQuestion);
        if (questionText == null || !rawQuestion.has("options") || !rawQuestion.has("correctAnswer")) {
            return null;
        }

        List<String> options = toOptions(rawQuestion.getJSONArray("options"));
        String correctAnswer = rawQuestion.getString("correctAnswer").trim();
        if (correctAnswer.isEmpty() || options.isEmpty()) {
            return null;
        }

        Question question = new Question();
        question.section = section;
        question.topic = topic;
        question.complexity = rawQuestion.optInt("complexity", 1);
        question.question = questionText;
        question.options = options;
        question.correctAnswer = correctAnswer;
        return question;
    }

    private static String extractQuestionText(JSONObject rawQuestion) throws Exception {
        if (!rawQuestion.has("question")) {
            return null;
        }
        String questionText = rawQuestion.getString("question").trim();
        return questionText.isEmpty() ? null : questionText;
    }

    private static List<String> toOptions(JSONArray array) throws Exception {
        List<String> options = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            String option = array.getString(i).trim();
            if (!option.isEmpty()) {
                options.add(option);
            }
        }
        return options;
    }

    private static String readAsset(AssetManager assetManager, String assetPath) throws Exception {
        try (InputStream inputStream = assetManager.open(assetPath)) {
            byte[] buffer = new byte[inputStream.available()];
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return "";
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        }
    }

    private static final class TopicPath {
        final String section;
        final String topic;

        private TopicPath(String section, String topic) {
            this.section = section;
            this.topic = topic;
        }

        static TopicPath parse(String topicPath) {
            if (topicPath == null || topicPath.isEmpty()) {
                return null;
            }
            int slashIndex = topicPath.indexOf('/');
            if (slashIndex <= 0 || slashIndex >= topicPath.length() - 1) {
                return null;
            }
            return new TopicPath(
                    topicPath.substring(0, slashIndex),
                    topicPath.substring(slashIndex + 1)
            );
        }

        String assetPath() {
            return section + "/" + topic + ".json";
        }
    }
}
