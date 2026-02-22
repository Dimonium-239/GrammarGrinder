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
import com.dimonium239.grammargrinder.practice.PracticeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TopicSelectActivity extends AppCompatActivity {
    private static final String EXTRA_SECTION = "SECTION";
    private static final String EXTRA_TITLE = "TITLE";
    private static final String TAG = "TopicSelectActivity";
    private String section;
    private LinearLayout containerTopics;
    private MaterialButton btnStart;
    private LayoutInflater inflater;
    private final List<MaterialCheckBox> checkBoxes = new ArrayList<>();


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

        Intent intent = getIntent();
        section = intent.getStringExtra(EXTRA_SECTION);
        String title = intent.getStringExtra(EXTRA_TITLE);

        String screenTitle = title != null ? title : getString(R.string.string_select_topics);
        tvTitle.setText(screenTitle);

        setupTopBar();
        setupStartButton();
        tvTitle.setVisibility(View.GONE);
        cbSectionTitle.setVisibility(View.VISIBLE);
        cbSectionTitle.setText(screenTitle);
        buildSingleSectionUI();

        updateStartState();
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

    private void buildSingleSectionUI() {
        if (section == null) return;

        try {
            String[] files = getAssets().list(section);
            if (files == null) return;

            for (String file : files) {
                if (!file.endsWith(".json")) continue;
                String topicId = file.replace(".json", "");
                addCheckbox(formatName(topicId), section + "/" + topicId);
            }
        } catch (IOException e) {
            Log.d(TAG, "Error listing assets in section: " + section, e);
        }
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

    private String formatName(String raw) {
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }

        return sb.toString().trim();
    }
}
