package com.dimonium239.grammargrinder.topics;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dimonium239.grammargrinder.options.OptionsActivity;
import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.core.settings.AppSettings;
import com.dimonium239.grammargrinder.db.ProgressService;
import com.dimonium239.grammargrinder.db.TopicProgress;
import com.dimonium239.grammargrinder.practice.PracticeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopicSelectActivity extends AppCompatActivity {
    private static final String EXTRA_SECTION = "SECTION";
    private static final String EXTRA_TITLE = "TITLE";
    private static final String TAG = "TopicSelectActivity";
    private String section;
    private LinearLayout containerTopics;
    private MaterialButton btnStart;
    private LayoutInflater inflater;
    private final List<MaterialCheckBox> checkBoxes = new ArrayList<>();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private MaterialCheckBox sectionTitleView;
    private String screenTitle;
    private boolean sectionLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_select);

        TextView tvTitle = findViewById(R.id.tv_sub_title);
        MaterialCheckBox cbSectionTitle = findViewById(R.id.cb_section_title);
        containerTopics = findViewById(R.id.container_topics);
        btnStart = findViewById(R.id.btn_start);
        inflater = LayoutInflater.from(this);
        sectionTitleView = cbSectionTitle;

        Intent intent = getIntent();
        section = intent.getStringExtra(EXTRA_SECTION);
        String title = intent.getStringExtra(EXTRA_TITLE);

        screenTitle = title != null ? title : getString(R.string.string_select_topics);
        tvTitle.setText(screenTitle);

        setupTopBar();
        setupStartButton();
        tvTitle.setVisibility(View.GONE);
        cbSectionTitle.setVisibility(View.VISIBLE);
        cbSectionTitle.setText(screenTitle);
        showLoadingState();
        loadSectionAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sectionLoaded) {
            refreshProgressAsync();
        }
    }

    @Override
    protected void onDestroy() {
        backgroundExecutor.shutdownNow();
        super.onDestroy();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        ImageButton btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnHelp = topBar.findViewById(R.id.btn_help);
        ImageButton btnOptions = topBar.findViewById(R.id.btn_options);
        TextView tvTitle = topBar.findViewById(R.id.tv_title);
        btnBack.setOnClickListener(v -> finish());
        btnHelp.setVisibility(View.GONE);
        btnOptions.setOnClickListener(v -> startActivity(new Intent(this, OptionsActivity.class)));
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        tvTitle.setText(title != null ? title : getString(R.string.app_name));
    }

    private void loadSectionAsync() {
        if (section == null || section.isEmpty()) {
            renderSection(new ArrayList<>(), new java.util.HashMap<>());
            return;
        }

        backgroundExecutor.execute(() -> {
            Map<String, TopicProgress> progressByTopic = ProgressService.getTopicProgressMap(this);
            List<String> topicPaths = new ArrayList<>();

            try {
                String[] files = getAssets().list(section);
                if (files != null) {
                    for (String file : files) {
                        if (file.endsWith(".json")) {
                            topicPaths.add(section + "/" + file.replace(".json", ""));
                        }
                    }
                    topicPaths.sort(String::compareTo);
                }
            } catch (IOException e) {
                Log.d(TAG, "Error listing assets in section: " + section, e);
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                renderSection(topicPaths, progressByTopic);
            });
        });
    }

    private void renderSection(List<String> topicPaths, Map<String, TopicProgress> progressByTopic) {
        sectionLoaded = true;
        sectionTitleView.setText(screenTitle);
        containerTopics.removeAllViews();
        checkBoxes.clear();
        for (String topicPath : topicPaths) {
            TopicProgress progress = progressByTopic.get(topicPath);
            addCheckbox(TopicProgressLabelHelper.formatTopicLine(this, topicPath, progress), topicPath);
        }
        updateStartState();
    }

    private void refreshProgressAsync() {
        backgroundExecutor.execute(() -> {
            Map<String, TopicProgress> progressByTopic = ProgressService.getTopicProgressMap(this);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !sectionLoaded) {
                    return;
                }
                for (MaterialCheckBox checkBox : checkBoxes) {
                    Object rawTag = checkBox.getTag();
                    if (!(rawTag instanceof String)) {
                        continue;
                    }
                    String topicPath = (String) rawTag;
                    checkBox.setText(TopicProgressLabelHelper.formatTopicLine(this, topicPath, progressByTopic.get(topicPath)));
                }
            });
        });
    }

    private void showLoadingState() {
        sectionLoaded = false;
        sectionTitleView.setText(getString(R.string.string_loading_content));
        containerTopics.removeAllViews();
        btnStart.setEnabled(false);
        btnStart.setAlpha(0.5f);
    }

    private void addCheckbox(String title, String tag) {
        MaterialCheckBox cb = createCheckbox(title, tag);
        containerTopics.addView(cb);
    }

    private MaterialCheckBox createCheckbox(String title, String tag) {
        return createCheckbox(title, tag, null);
    }

    private MaterialCheckBox createCheckbox(String title, String tag, Runnable onChanged) {
        MaterialCheckBox cb = (MaterialCheckBox) inflater.inflate(R.layout.item_topic_checkbox, containerTopics, false);
        cb.setText(title);
        cb.setTag(tag);
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStartState();
            if (onChanged != null) {
                onChanged.run();
            }
        });
        checkBoxes.add(cb);
        return cb;
    }

    private void setupStartButton() {
        btnStart.setOnClickListener(v -> {
            ArrayList<String> selected = getSelectedTopics();
            if (selected.isEmpty()) return;
            Intent intent = new Intent(this, PracticeActivity.class);
            intent.putStringArrayListExtra("TOPICS", selected);
            startActivity(intent);
        });
    }

    private ArrayList<String> getSelectedTopics() {
        ArrayList<String> result = new ArrayList<>();
        for (MaterialCheckBox cb : checkBoxes) {
            if (cb.isChecked()) {
                result.add((String) cb.getTag());
            }
        }
        return result;
    }

    private void updateStartState() {
        boolean any = false;
        for (MaterialCheckBox cb : checkBoxes) {
            if (cb.isChecked()) {
                any = true;
                break;
            }
        }

        btnStart.setEnabled(any);
        btnStart.setAlpha(any ? 1f : 0.5f);
    }

}
