package com.dimonium239.grammargrinder.guides;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GuideLoader {
    private static final String TAG = "GuideLoader";
    private static final String GUIDES_DIR = "guides";

    private GuideLoader() {
    }

    public static List<GuideEntry> loadGuidesForSection(Context context, String sectionId) {
        List<GuideEntry> result = new ArrayList<>();
        if (sectionId == null || sectionId.isEmpty()) {
            return result;
        }
        try {
            String json = readAsset(context, GUIDES_DIR + "/" + sectionId + ".json");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                result.add(parseEntry(o));
            }
        } catch (Exception e) {
            Log.d(TAG, "Error loading guide data", e);
        }
        return result;
    }

    public static GuideEntry findById(Context context, String topicId) {
        if (topicId == null || topicId.isEmpty()) {
            return null;
        }
        for (String sectionId : listGuideSectionIds(context)) {
            List<GuideEntry> entries = loadGuidesForSection(context, sectionId);
            for (GuideEntry entry : entries) {
                if (entry.id != null && entry.id.equals(topicId)) {
                    return entry;
                }
            }
        }
        return null;
    }

    public static boolean hasGuidesForSection(Context context, String sectionId) {
        return !loadGuidesForSection(context, sectionId).isEmpty();
    }

    public static List<String> listGuideSectionIds(Context context) {
        List<String> result = new ArrayList<>();
        try {
            String[] files = context.getAssets().list(GUIDES_DIR);
            if (files == null) {
                return result;
            }
            for (String file : files) {
                if (!file.endsWith(".json")) {
                    continue;
                }
                result.add(file.substring(0, file.length() - 5));
            }
        } catch (Exception e) {
            Log.d(TAG, "Error listing guide sections", e);
        }
        return result;
    }

    private static GuideEntry parseEntry(JSONObject o) throws Exception {
        GuideEntry entry = new GuideEntry();
        entry.id = o.getString("id");
        entry.title = o.getString("title");
        entry.whenToUse = o.optString("whenToUse", "");
        entry.formula = o.optString("formula", "");
        entry.examples = toList(o.optJSONArray("examples"));
        entry.keywords = toList(o.optJSONArray("keywords"));
        entry.commonMistakes = toList(o.optJSONArray("commonMistakes"));
        return entry;
    }

    private static List<String> toList(JSONArray arr) throws Exception {
        List<String> result = new ArrayList<>();
        if (arr == null) {
            return result;
        }
        for (int i = 0; i < arr.length(); i++) {
            result.add(arr.getString(i));
        }
        return result;
    }

    private static String readAsset(Context context, String path) throws Exception {
        AssetManager am = context.getAssets();
        InputStream is = am.open(path);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
}
