package com.dimonium239.grammargrinder.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dimonium239.grammargrinder.guides.GuidesSheetsActivity;
import com.dimonium239.grammargrinder.options.OptionsActivity;
import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.practice.PracticeActivity;
import com.dimonium239.grammargrinder.core.settings.AppSettings;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private final List<MaterialCheckBox> checkBoxes = new ArrayList<>();
    private LinearLayout containerTopics;
    private MaterialButton btnStart;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_select);

        containerTopics = findViewById(R.id.container_topics);
        btnStart = findViewById(R.id.btn_start);
        inflater = LayoutInflater.from(this);

        setupTopBar();
        setupStartButton();
        buildMixUI();
        updateStartState();
    }

    private void buildMixUI() {
        List<SectionMeta> sections = SectionLoader.loadSections(this);
        for (SectionMeta section : sections) {
            if (!"section".equals(section.type)) {
                continue;
            }
            try {
                String[] files = getAssets().list(section.id);
                if (files == null) {
                    continue;
                }
                addDropdownSection(section, files);
            } catch (IOException e) {
                Log.d(TAG, "Error listing assets in section: " + section.id, e);
            }
        }
    }

    private void addDropdownSection(SectionMeta sectionMeta, String[] files) {
        View card = inflater.inflate(R.layout.item_topic_dropdown_section, containerTopics, false);
        LinearLayout headerRow = card.findViewById(R.id.section_header_row);
        TextView header = card.findViewById(R.id.tv_section_title);
        ImageView expandToggle = card.findViewById(R.id.iv_expand_toggle);
        MaterialCheckBox cbSection = card.findViewById(R.id.cb_section_select);
        LinearLayout content = card.findViewById(R.id.container_section_topics);
        List<MaterialCheckBox> sectionCheckBoxes = new ArrayList<>();
        AtomicBoolean syncing = new AtomicBoolean(false);
        header.setText(sectionMeta.title);

        for (String file : files) {
            if (!file.endsWith(".json")) {
                continue;
            }
            String topicId = file.replace(".json", "");
            MaterialCheckBox cb = createCheckbox(
                    formatName(topicId),
                    sectionMeta.id + "/" + topicId,
                    () -> syncSectionCheckState(syncing, cbSection, sectionCheckBoxes)
            );
            sectionCheckBoxes.add(cb);
            content.addView(cb);
        }

        cbSection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (syncing.get()) {
                return;
            }
            syncing.set(true);
            boolean selectAll = cbSection.getCheckedState() == MaterialCheckBox.STATE_CHECKED;
            for (MaterialCheckBox cb : sectionCheckBoxes) {
                cb.setChecked(selectAll);
            }
            syncing.set(false);
        });

        syncSectionCheckState(syncing, cbSection, sectionCheckBoxes);

        View.OnClickListener expandCollapse = v -> {
            boolean visible = content.getVisibility() == View.VISIBLE;
            boolean expanded = !visible;
            content.setVisibility(expanded ? View.VISIBLE : View.GONE);
            expandToggle.animate().rotation(expanded ? 180f : 0f).setDuration(120).start();
        };
        headerRow.setOnClickListener(expandCollapse);
        header.setOnClickListener(expandCollapse);
        containerTopics.addView(card);
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

    private void syncSectionCheckState(
            AtomicBoolean syncing,
            MaterialCheckBox sectionCheckBox,
            List<MaterialCheckBox> sectionCheckBoxes
    ) {
        if (syncing.get()) {
            return;
        }
        syncing.set(true);
        sectionCheckBox.setCheckedState(resolveSectionCheckedState(sectionCheckBoxes));
        syncing.set(false);
    }

    private int resolveSectionCheckedState(List<MaterialCheckBox> sectionCheckBoxes) {
        if (sectionCheckBoxes.isEmpty()) {
            return MaterialCheckBox.STATE_UNCHECKED;
        }

        int checkedCount = 0;
        for (MaterialCheckBox child : sectionCheckBoxes) {
            if (child.isChecked()) {
                checkedCount++;
            }
        }

        if (checkedCount == 0) {
            return MaterialCheckBox.STATE_UNCHECKED;
        }
        if (checkedCount == sectionCheckBoxes.size()) {
            return MaterialCheckBox.STATE_CHECKED;
        }
        return MaterialCheckBox.STATE_INDETERMINATE;
    }

    private void setupStartButton() {
        btnStart.setOnClickListener(v -> {
            ArrayList<String> selected = getSelectedTopics();
            if (selected.isEmpty()) {
                return;
            }
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
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        ImageButton btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnHelp = topBar.findViewById(R.id.btn_help);
        ImageButton btnOptions = topBar.findViewById(R.id.btn_options);
        TextView tvTitle = topBar.findViewById(R.id.tv_title);

        tvTitle.setText(getString(R.string.string_home_title));
        btnBack.setVisibility(View.INVISIBLE);
        btnHelp.setVisibility(View.VISIBLE);
        btnHelp.setOnClickListener(v -> startActivity(new Intent(this, GuidesSheetsActivity.class)));
        btnOptions.setOnClickListener(v -> startActivity(new Intent(this, OptionsActivity.class)));
    }
}
