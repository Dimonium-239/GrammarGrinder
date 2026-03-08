package com.dimonium239.grammargrinder.topics;

import android.content.Context;

import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.db.ProgressService;
import com.dimonium239.grammargrinder.db.TopicProgress;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;
import java.util.Map;

public final class TopicProgressLabelHelper {
    private TopicProgressLabelHelper() {
    }

    public static void refreshTopicProgressLabels(Context context, List<MaterialCheckBox> checkBoxes) {
        Map<String, TopicProgress> progressByTopic = ProgressService.getTopicProgressMap(context);
        for (MaterialCheckBox cb : checkBoxes) {
            Object rawTag = cb.getTag();
            if (!(rawTag instanceof String)) {
                continue;
            }
            String topicPath = (String) rawTag;
            TopicProgress progress = progressByTopic.get(topicPath);
            cb.setText(formatTopicLine(context, topicPath, progress));
        }
    }

    public static String formatTopicLine(Context context, String topicPath, TopicProgress progress) {
        String topicId = extractTopicId(topicPath);
        String title = formatTopicName(topicId);
        int seen = progress != null ? progress.getSeen() : 0;
        int accuracy = progress != null ? Math.round(progress.getAccuracyPercent()) : 0;
        String stats = context.getString(R.string.string_topic_progress_accuracy_seen, accuracy, seen);
        return context.getString(R.string.string_topic_progress_two_line, title, stats);
    }

    private static String extractTopicId(String topicPath) {
        if (topicPath == null || topicPath.isEmpty()) {
            return "";
        }
        int slashIndex = topicPath.indexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < topicPath.length()) {
            return topicPath.substring(slashIndex + 1);
        }
        return topicPath;
    }

    private static String formatTopicName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
