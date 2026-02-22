package com.dimonium239.grammargrinder.guides;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
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

import java.util.List;

public class GuidesSheetsActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC_ID = "TOPIC_ID";

    private static final int LEVEL_SECTIONS = 0;
    private static final int LEVEL_TOPICS = 1;
    private static final int LEVEL_DETAIL = 2;

    private LinearLayout container;
    private TextView subtitle;
    private int currentLevel = LEVEL_SECTIONS;
    private String currentSectionId = "";
    private String currentSectionTitle = "";
    private boolean openedFromTopicDeepLink = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        container = findViewById(R.id.container_guides);
        subtitle = findViewById(R.id.tv_guide_subtitle);

        setupTopBar();
        renderGuides();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        ImageButton btnBack = topBar.findViewById(R.id.btn_back);
        ImageButton btnHelp = topBar.findViewById(R.id.btn_help);
        ImageButton btnOptions = topBar.findViewById(R.id.btn_options);
        TextView tvTitle = topBar.findViewById(R.id.tv_title);

        btnBack.setOnClickListener(v -> navigateBack());
        btnHelp.setVisibility(View.GONE);
        btnOptions.setOnClickListener(v -> startActivity(new Intent(this, OptionsActivity.class)));
        tvTitle.setText(getString(R.string.string_grammar_guide));
    }

    private void renderGuides() {
        String topicId = getIntent().getStringExtra(EXTRA_TOPIC_ID);
        if (topicId != null && !topicId.isEmpty()) {
            GuideEntry entry = GuideLoader.findById(this, topicId);
            if (entry != null) {
                openedFromTopicDeepLink = true;
                renderGuideDetail(entry);
                return;
            }
        }
        renderSections();
    }

    private void navigateBack() {
        if (openedFromTopicDeepLink) {
            finish();
            return;
        }
        if (currentLevel == LEVEL_DETAIL) {
            renderTopics(currentSectionId, currentSectionTitle);
            return;
        }
        if (currentLevel == LEVEL_TOPICS) {
            renderSections();
            return;
        }
        finish();
    }

    private void renderSections() {
        currentLevel = LEVEL_SECTIONS;
        currentSectionId = "";
        currentSectionTitle = "";

        container.removeAllViews();
        subtitle.setText(getString(R.string.string_guide_choose_section));

        List<SectionMeta> sections = SectionLoader.loadSections(this);
        for (SectionMeta section : sections) {
            if (!"section".equals(section.type)) {
                continue;
            }
            if (!GuideLoader.hasGuidesForSection(this, section.id)) {
                continue;
            }
            addNavigationCard(
                    section.title,
                    section.subtitle,
                    v -> renderTopics(section.id, section.title)
            );
        }
    }

    private void renderTopics(String sectionId, String sectionTitle) {
        currentLevel = LEVEL_TOPICS;
        currentSectionId = sectionId;
        currentSectionTitle = sectionTitle;

        container.removeAllViews();
        subtitle.setText(getString(R.string.string_guide_section_subtitle, sectionTitle));

        List<GuideEntry> entries = GuideLoader.loadGuidesForSection(this, sectionId);
        for (GuideEntry entry : entries) {
            addNavigationCard(
                    entry.title,
                    getString(R.string.string_guide_topic_card_subtitle),
                    v -> renderGuideDetail(entry)
            );
        }
    }

    private void renderGuideDetail(GuideEntry entry) {
        currentLevel = LEVEL_DETAIL;
        container.removeAllViews();
        subtitle.setText(getString(R.string.string_guide_single_subtitle, entry.title));
        addGuideCard(entry);
    }

    private void addNavigationCard(String title, String subtitleText, View.OnClickListener onClick) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_guide_navigation_card, container, false);
        TextView tvTitle = card.findViewById(R.id.tv_title);
        TextView tvSubtitle = card.findViewById(R.id.tv_subtitle);
        tvTitle.setText(title);
        tvSubtitle.setText(subtitleText);
        card.setOnClickListener(onClick);
        container.addView(card);
    }

    private void addGuideCard(GuideEntry entry) {
        if (entry == null) {
            return;
        }
        View card = LayoutInflater.from(this).inflate(R.layout.item_guide_detail_card, container, false);
        TextView tvTitle = card.findViewById(R.id.tv_title);
        TextView tvUse = card.findViewById(R.id.tv_use);
        TextView tvFormula = card.findViewById(R.id.tv_formula);
        TextView tvExamples = card.findViewById(R.id.tv_examples);
        TextView tvKeywords = card.findViewById(R.id.tv_keywords);
        TextView tvMistakes = card.findViewById(R.id.tv_mistakes);

        tvTitle.setText(entry.title);
        tvUse.setText(getString(R.string.string_guide_when_to_use, entry.whenToUse));
        tvFormula.setText(getString(R.string.string_guide_formula, entry.formula));
        tvExamples.setText(getString(R.string.string_guide_examples, bulletList(entry.examples)));
        tvKeywords.setText(getString(R.string.string_guide_keywords, joinComma(entry.keywords)));
        tvMistakes.setText(getString(R.string.string_guide_common_mistakes, bulletList(entry.commonMistakes)));
        container.addView(card);
    }

    private String bulletList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append("• ").append(items.get(i));
            if (i < items.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String joinComma(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i));
            if (i < items.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
