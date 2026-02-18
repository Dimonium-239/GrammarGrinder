package com.dimonium239.grammargrinder.home;

import android.util.Log;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SectionLoader {

    private static final String TAG = "SectionLoader";

    public static List<SectionMeta> loadSections(Context context) {
        List<SectionMeta> result = new ArrayList<>();

        try {
            InputStream is = context.getAssets().open("sections.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                result.add(new SectionMeta(
                        o.getString("id"),
                        o.getString("title"),
                        o.optString("subtitle", ""),
                        o.optString("type")
                ));
            }
            is.close();
        } catch (Exception e) {
            Log.d(TAG, "Error loading sections from sections.json", e);
        }

        return result;
    }
}
