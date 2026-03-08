package com.dimonium239.grammargrinder.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
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
import com.dimonium239.grammargrinder.db.ProgressService;
import com.dimonium239.grammargrinder.db.TopicProgress;
import com.dimonium239.grammargrinder.topics.TopicProgressLabelHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private final List<MaterialCheckBox> checkBoxes = new ArrayList<>();
    private final List<SingleTopicHeaderBinding> singleTopicHeaderBindings = new ArrayList<>();
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

    @Override
    protected void onResume() {
        super.onResume();
        TopicProgressLabelHelper.refreshTopicProgressLabels(this, checkBoxes);
        refreshSingleTopicHeaders();
    }

    private void buildMixUI() {
        List<SectionMeta> sections = SectionLoader.loadSections(this);
        Map<String, TopicProgress> progressByTopic = ProgressService.getTopicProgressMap(this);
        for (SectionMeta section : sections) {
            if (!"section".equals(section.type)) {
                continue;
            }
            try {
                String[] files = getAssets().list(section.id);
                if (files == null) {
                    continue;
                }
                List<String> topicFiles = listJsonTopicFiles(files);
                if (topicFiles.size() == 1) {
                    addSingleTopicSectionCard(section, topicFiles.get(0), progressByTopic);
                    continue;
                }
                if (topicFiles.size() >= 2) {
                    addDropdownSection(section, topicFiles, progressByTopic);
                }
            } catch (IOException e) {
                Log.d(TAG, "Error listing assets in section: " + section.id, e);
            }
        }
    }

    private List<String> listJsonTopicFiles(String[] files) {
        List<String> topicFiles = new ArrayList<>();
        for (String file : files) {
            if (file.endsWith(".json")) {
                topicFiles.add(file);
            }
        }
        topicFiles.sort(String::compareTo);
        return topicFiles;
    }

    private void addSingleTopicSectionCard(SectionMeta sectionMeta, String topicFile, Map<String, TopicProgress> progressByTopic) {
        View card = inflater.inflate(R.layout.item_topic_dropdown_section, containerTopics, false);
        LinearLayout headerRow = card.findViewById(R.id.section_header_row);
        TextView header = card.findViewById(R.id.tv_section_title);
        ImageView expandToggle = card.findViewById(R.id.iv_expand_toggle);
        MaterialCheckBox cbSection = card.findViewById(R.id.cb_section_select);
        LinearLayout content = card.findViewById(R.id.container_section_topics);

        String topicId = topicFile.replace(".json", "");
        String topicPath = sectionMeta.id + "/" + topicId;
        TopicProgress progress = progressByTopic.get(topicPath);
        cbSection.setText(null);
        cbSection.setTag(new TopicSelectionTag(topicPath));
        cbSection.setOnCheckedChangeListener((buttonView, isChecked) -> updateStartState());
        checkBoxes.add(cbSection);
        singleTopicHeaderBindings.add(new SingleTopicHeaderBinding(header, topicPath, sectionMeta.title));

        int childIndent = getResources().getDimensionPixelSize(R.dimen.spacing_subsection_header_padding);
        headerRow.setPaddingRelative(
                childIndent,
                headerRow.getPaddingTop(),
                headerRow.getPaddingEnd(),
                headerRow.getPaddingBottom()
        );
        header.setText(buildSingleTopicHeaderText(sectionMeta.title, progress));

        expandToggle.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        headerRow.setOnClickListener(null);

        containerTopics.addView(card);
    }

    private void addDropdownSection(SectionMeta sectionMeta, List<String> topicFiles, Map<String, TopicProgress> progressByTopic) {
        View card = inflater.inflate(R.layout.item_topic_dropdown_section, containerTopics, false);
        LinearLayout headerRow = card.findViewById(R.id.section_header_row);
        TextView header = card.findViewById(R.id.tv_section_title);
        ImageView expandToggle = card.findViewById(R.id.iv_expand_toggle);
        MaterialCheckBox cbSection = card.findViewById(R.id.cb_section_select);
        LinearLayout content = card.findViewById(R.id.container_section_topics);
        List<MaterialCheckBox> sectionCheckBoxes = new ArrayList<>();
        AtomicBoolean syncing = new AtomicBoolean(false);
        header.setText(sectionMeta.title);

        for (String file : topicFiles) {
            String topicId = file.replace(".json", "");
            String topicPath = sectionMeta.id + "/" + topicId;
            TopicProgress progress = progressByTopic.get(topicPath);
            MaterialCheckBox cb = createCheckbox(
                    TopicProgressLabelHelper.formatTopicLine(this, topicPath, progress),
                    topicPath,
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
                Object tag = cb.getTag();
                if (tag instanceof String) {
                    result.add((String) tag);
                } else if (tag instanceof TopicSelectionTag) {
                    result.add(((TopicSelectionTag) tag).path);
                }
            }
        }
        return result;
    }

    private void refreshSingleTopicHeaders() {
        if (singleTopicHeaderBindings.isEmpty()) {
            return;
        }
        Map<String, TopicProgress> progressByTopic = ProgressService.getTopicProgressMap(this);
        for (SingleTopicHeaderBinding binding : singleTopicHeaderBindings) {
            TopicProgress progress = progressByTopic.get(binding.topicPath);
            binding.header.setText(buildSingleTopicHeaderText(binding.sectionTitle, progress));
        }
    }

    private CharSequence buildSingleTopicHeaderText(String title, TopicProgress progress) {
        int seen = progress != null ? progress.getSeen() : 0;
        int accuracy = progress != null ? Math.round(progress.getAccuracyPercent()) : 0;
        String stats = getString(R.string.string_topic_progress_accuracy_seen, accuracy, seen);
        String full = title + "\n" + stats;
        SpannableString styled = new SpannableString(full);
        int secondLineStart = title.length() + 1;
        styled.setSpan(new RelativeSizeSpan(0.72f), secondLineStart, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }

    private static final class TopicSelectionTag {
        final String path;

        TopicSelectionTag(String path) {
            this.path = path;
        }
    }

    private static final class SingleTopicHeaderBinding {
        final TextView header;
        final String topicPath;
        final String sectionTitle;

        SingleTopicHeaderBinding(TextView header, String topicPath, String sectionTitle) {
            this.header = header;
            this.topicPath = topicPath;
            this.sectionTitle = sectionTitle;
        }
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
