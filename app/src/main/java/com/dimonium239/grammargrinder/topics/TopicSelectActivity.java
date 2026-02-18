package com.dimonium239.grammargrinder.topics;

import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dimonium239.grammargrinder.options.OptionsActivity;
import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.core.settings.AppSettings;
import com.dimonium239.grammargrinder.home.SectionLoader;
import com.dimonium239.grammargrinder.home.SectionMeta;
import com.dimonium239.grammargrinder.practice.PracticeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TopicSelectActivity extends AppCompatActivity {

    private static final String TAG = "TopicSelectActivity";
    private String section;
    private LinearLayout containerTopics;
    private MaterialButton btnStart;
    private final List<MaterialCheckBox> checkBoxes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_select);

        TextView tvTitle = findViewById(R.id.tv_sub_title);
        containerTopics = findViewById(R.id.container_topics);
        btnStart = findViewById(R.id.btn_start);

        section = getIntent().getStringExtra("SECTION");
        String mode = getIntent().getStringExtra("MODE");
        String title = getIntent().getStringExtra("TITLE");

        tvTitle.setText(title != null ? title : getString(R.string.string_select_topics));

        setupTopBar();
        setupStartButton();

        if ("mix".equals(mode)) {
            buildMixUI();
        } else {
            buildSingleSectionUI();
        }

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
        String title = getIntent().getStringExtra("TITLE");
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

    private void buildMixUI() {
        List<SectionMeta> sections = SectionLoader.loadSections(this);
        for (SectionMeta s : sections) {
            if (!"section".equals(s.type)) continue;
            try {
                String[] files = getAssets().list(s.id);
                if (files == null) continue;
                addDropdownSection(s, files);
            } catch (IOException e) {
                Log.d(TAG, "Error listing assets in section: " + s.id, e);
            }
        }
    }

    private void addDropdownSection(SectionMeta sectionMeta, String[] files) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(20f);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(this);
        header.setText(sectionMeta.title);
        header.setPadding(32, 24, 32, 24);
        header.setTextSize(18);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setVisibility(View.GONE);
        content.setPadding(32, 0, 32, 24);

        for (String file : files) {
            if (!file.endsWith(".json")) continue;
            String topicId = file.replace(".json", "");
            MaterialCheckBox cb = createCheckbox(formatName(topicId), sectionMeta.id + "/" + topicId);
            content.addView(cb);
        }

        header.setOnClickListener(v -> {
            boolean visible = content.getVisibility() == View.VISIBLE;
            content.setVisibility(visible ? View.GONE : View.VISIBLE);
        });

        root.addView(header);
        root.addView(content);
        card.addView(root);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        params.bottomMargin = 12;

        containerTopics.addView(card, params);
    }

    private void addCheckbox(String title, String tag) {
        MaterialCheckBox cb = createCheckbox(title, tag);
        containerTopics.addView(cb);
    }

    private MaterialCheckBox createCheckbox(String title, String tag) {
        MaterialCheckBox cb = new MaterialCheckBox(this);
        cb.setText(title);
        cb.setTag(tag);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        params.bottomMargin = 12;
        cb.setLayoutParams(params);
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> updateStartState());
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
