package com.dimonium239.grammargrinder.topics;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
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
import com.dimonium239.grammargrinder.practice.QuestionLoader;
import com.dimonium239.grammargrinder.practice.PracticeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class TopicSelectActivity extends AppCompatActivity {
    private static final String EXTRA_SECTION = "SECTION";
    private static final String EXTRA_MODE = "MODE";
    private static final String EXTRA_TITLE = "TITLE";
    private static final String EXTRA_TOPICS = "TOPICS";
    private static final String MODE_MIX = "mix";
    private String section;
    private LinearLayout containerTopics;
    private MaterialButton btnStart;
    private MaterialCheckBox cbSectionTitle;
    private boolean sectionBulkChangeInProgress = false;
    private final List<MaterialCheckBox> checkBoxes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_select);

        TextView tvTitle = findViewById(R.id.tv_sub_title);
        cbSectionTitle = findViewById(R.id.cb_section_title);
        containerTopics = findViewById(R.id.container_topics);
        btnStart = findViewById(R.id.btn_start);

        Intent intent = getIntent();
        section = intent.getStringExtra(EXTRA_SECTION);
        String mode = intent.getStringExtra(EXTRA_MODE);
        String title = intent.getStringExtra(EXTRA_TITLE);

        String screenTitle = title != null ? title : getString(R.string.string_select_topics);
        tvTitle.setText(screenTitle);

        setupTopBar();
        setupStartButton();

        if (MODE_MIX.equals(mode)) {
            cbSectionTitle.setVisibility(View.GONE);
            buildMixUI();
        } else {
            tvTitle.setVisibility(View.GONE);
            cbSectionTitle.setVisibility(View.VISIBLE);
            cbSectionTitle.setText(screenTitle);
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
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        tvTitle.setText(title != null ? title : getString(R.string.app_name));
    }

    private void buildSingleSectionUI() {
        if (section == null) {
            return;
        }
        List<String> topicIds = QuestionLoader.listTopicIds(this, section);
        for (String topicId : topicIds) {
            addCheckbox(formatName(topicId), section + "/" + topicId);
        }
        cbSectionTitle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (sectionBulkChangeInProgress) return;
            sectionBulkChangeInProgress = true;
            setChecked(checkBoxes, isChecked);
            sectionBulkChangeInProgress = false;
            updateStartState();
        });
        syncStandaloneSectionHeaderState();
    }

    private void buildMixUI() {
        List<SectionMeta> sections = SectionLoader.loadSections(this);
        for (SectionMeta s : sections) {
            if (!"section".equals(s.type)) continue;
            List<String> topicIds = QuestionLoader.listTopicIds(this, s.id);
            if (!topicIds.isEmpty()) {
                addDropdownSection(s, topicIds);
            }
        }
    }

    private void addDropdownSection(SectionMeta sectionMeta, List<String> topicIds) {
        DropdownSectionViews views = createDropdownSectionViews(sectionMeta.title);
        List<MaterialCheckBox> sectionCheckBoxes = populateDropdownTopics(views.content, sectionMeta.id, topicIds);
        wireDropdownInteractions(views, sectionCheckBoxes);
        attachDropdownSection(views.card);
    }

    private DropdownSectionViews createDropdownSectionViews(String title) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(20f);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(32, 24, 32, 24);

        MaterialCheckBox headerCheckBox = new MaterialCheckBox(this);
        headerCheckBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView headerTitle = new TextView(this);
        headerTitle.setText(title);
        headerTitle.setTextSize(18);
        headerTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        ImageButton expandButton = new ImageButton(this);
        int expandButtonSize = dpToPx(40);
        expandButton.setLayoutParams(new LinearLayout.LayoutParams(expandButtonSize, expandButtonSize));
        expandButton.setBackgroundResource(android.R.color.transparent);
        expandButton.setImageResource(android.R.drawable.arrow_down_float);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setVisibility(View.GONE);
        content.setPadding(32, 0, 32, 24);

        headerRow.addView(headerCheckBox);
        headerRow.addView(headerTitle);
        headerRow.addView(expandButton);
        root.addView(headerRow);
        root.addView(content);
        card.addView(root);

        return new DropdownSectionViews(card, headerCheckBox, headerTitle, expandButton, content);
    }

    private List<MaterialCheckBox> populateDropdownTopics(LinearLayout content, String sectionId, List<String> topicIds) {
        List<MaterialCheckBox> sectionCheckBoxes = new ArrayList<>();
        for (String topicId : topicIds) {
            MaterialCheckBox cb = createCheckbox(formatName(topicId), sectionId + "/" + topicId);
            sectionCheckBoxes.add(cb);
            content.addView(cb);
        }
        return sectionCheckBoxes;
    }

    private void wireDropdownInteractions(DropdownSectionViews views, List<MaterialCheckBox> sectionCheckBoxes) {
        final boolean[] bulkUpdateInProgress = {false};

        Runnable syncHeaderState = () -> {
            if (bulkUpdateInProgress[0]) return;
            views.headerCheckBox.setChecked(areAllChecked(sectionCheckBoxes));
            updateStartState();
        };
        Runnable toggleExpanded = () -> {
            boolean visible = views.content.getVisibility() == View.VISIBLE;
            views.content.setVisibility(visible ? View.GONE : View.VISIBLE);
            views.expandButton.setImageResource(
                    visible ? android.R.drawable.arrow_down_float : android.R.drawable.arrow_up_float
            );
        };

        for (MaterialCheckBox cb : sectionCheckBoxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> syncHeaderState.run());
        }

        views.headerTitle.setOnClickListener(v -> toggleExpanded.run());
        views.expandButton.setOnClickListener(v -> toggleExpanded.run());
        views.headerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bulkUpdateInProgress[0]) return;
            bulkUpdateInProgress[0] = true;
            setChecked(sectionCheckBoxes, isChecked);
            bulkUpdateInProgress[0] = false;
            updateStartState();
        });
        syncHeaderState.run();
    }

    private void attachDropdownSection(MaterialCardView card) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
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
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStartState();
            syncStandaloneSectionHeaderState();
        });
        checkBoxes.add(cb);
        return cb;
    }

    private void setupStartButton() {
        btnStart.setOnClickListener(v -> {
            ArrayList<String> selected = getSelectedTopics();
            if (selected.isEmpty()) return;
            Intent intent = new Intent(this, PracticeActivity.class);
            intent.putStringArrayListExtra(EXTRA_TOPICS, selected);
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
        boolean any = hasChecked(checkBoxes);
        btnStart.setEnabled(any);
        btnStart.setAlpha(any ? 1f : 0.5f);
    }

    private void syncStandaloneSectionHeaderState() {
        if (cbSectionTitle == null || cbSectionTitle.getVisibility() != View.VISIBLE || sectionBulkChangeInProgress) {
            return;
        }
        cbSectionTitle.setChecked(areAllChecked(checkBoxes));
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private boolean hasChecked(List<MaterialCheckBox> boxes) {
        for (MaterialCheckBox cb : boxes) {
            if (cb.isChecked()) return true;
        }
        return false;
    }

    private boolean areAllChecked(List<MaterialCheckBox> boxes) {
        if (boxes.isEmpty()) return false;
        for (MaterialCheckBox cb : boxes) {
            if (!cb.isChecked()) return false;
        }
        return true;
    }

    private void setChecked(List<MaterialCheckBox> boxes, boolean checked) {
        for (MaterialCheckBox cb : boxes) {
            cb.setChecked(checked);
        }
    }

    private static class DropdownSectionViews {
        private final MaterialCardView card;
        private final MaterialCheckBox headerCheckBox;
        private final TextView headerTitle;
        private final ImageButton expandButton;
        private final LinearLayout content;

        private DropdownSectionViews(
                MaterialCardView card,
                MaterialCheckBox headerCheckBox,
                TextView headerTitle,
                ImageButton expandButton,
                LinearLayout content
        ) {
            this.card = card;
            this.headerCheckBox = headerCheckBox;
            this.headerTitle = headerTitle;
            this.expandButton = expandButton;
            this.content = content;
        }
    }
}
