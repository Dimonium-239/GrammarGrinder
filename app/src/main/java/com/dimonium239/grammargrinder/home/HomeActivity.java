package com.dimonium239.grammargrinder.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonium239.grammargrinder.guides.GuidesSheetsActivity;
import com.dimonium239.grammargrinder.options.OptionsActivity;
import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.topics.TopicSelectActivity;
import com.dimonium239.grammargrinder.core.settings.AppSettings;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_category);

        RecyclerView rv = findViewById(R.id.rv_sections);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<SectionMeta> sections = SectionLoader.loadSections(this);

        SectionAdapter adapter = new SectionAdapter(sections, section -> {
            switch (section.type) {
                case "section":
                    Intent i = new Intent(this, TopicSelectActivity.class);
                    i.putExtra("SECTION", section.id);
                    i.putExtra("TITLE", section.title);
                    startActivity(i);
                    break;
                case "mix":
                    Intent mix = new Intent(this, TopicSelectActivity.class);
                    mix.putExtra("MODE", "mix");
                    mix.putExtra("TITLE", section.title);
                    startActivity(mix);
                    break;
                case "guides":
                    startActivity(new Intent(this, GuidesSheetsActivity.class));
                    break;
                case "options":
                    startActivity(new Intent(this, OptionsActivity.class));
                    break;
            }
        });

        rv.setAdapter(adapter);
    }
}
