package com.dimonium239.grammargrinder.practice;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QuestionLoader {

    private static final String TAG = "QuestionLoader";

    public static List<Question> loadQuestions(Context context, String section, List<String> topics) {
        List<Question> result = new ArrayList<>();

        for (String topic : topics) {
            String path = section + "/" + topic + ".json";
            List<Question> fromFile = loadFromFile(context, path);
            result.addAll(fromFile);
        }

        return result;
    }

    private static List<Question> loadFromFile(Context context, String assetPath) {
        List<Question> list = new ArrayList<>();
        try {
            String json = readAsset(context, assetPath);
            JSONArray arr = new JSONArray(json);
            String[] source = parseSource(assetPath);
            String sectionId = source[0];
            String topicId = source[1];

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Question q = parseQuestion(o);
                q.sectionId = sectionId;
                q.topicId = topicId;
                list.add(q);
            }
        } catch (Exception e) {
            Log.d(TAG, "Error loading questions from JSON: " + assetPath, e);
        }

        return list;
    }

    private static Question parseQuestion(JSONObject o) throws Exception {

        Question q = new Question();

        q.id = o.getString("id");
        q.category = o.optString("category", "");
        q.complexity = o.optInt("complexity", 1);

        q.question = o.getString("question");

        JSONArray opts = o.getJSONArray("options");
        List<String> options = new ArrayList<>();

        for (int i = 0; i < opts.length(); i++) {
            options.add(opts.getString(i));
        }
        q.options = options;

        q.correctAnswer = o.getString("correctAnswer");
        q.explanation = o.optString("explanation", "");

        q.priority = o.optInt("priority", 10);
        q.lastSeen = o.optLong("lastSeen", 0);
        q.mistakeCount = o.optInt("mistakeCount", 0);

        return q;
    }

    public static List<Question> loadQuestionsMix(Context context, List<String> selectedTopics) {
        List<Question> all = new ArrayList<>();
        if (selectedTopics == null) {
            return all;
        }

        for (String topicPath : selectedTopics) {
            String assetPath = topicPath + ".json";
            List<Question> questions = loadFromFile(context, assetPath);
            all.addAll(questions);
        }
        return all;
    }

    private static String readAsset(Context context, String path) throws Exception {
        AssetManager am = context.getAssets();
        InputStream is = am.open(path);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    private static String[] parseSource(String assetPath) {
        String normalized = assetPath.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        int dot = normalized.lastIndexOf('.');
        if (slash <= 0 || dot <= slash) {
            return new String[]{"", ""};
        }
        String sectionId = normalized.substring(0, slash);
        String topicId = normalized.substring(slash + 1, dot);
        return new String[]{sectionId, topicId};
    }
}
