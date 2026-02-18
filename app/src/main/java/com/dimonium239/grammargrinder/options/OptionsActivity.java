package com.dimonium239.grammargrinder.options;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.core.settings.AppSettings;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        setupTopBar();
        setupControls();
    }

    private void setupTopBar() {
        View topBar = findViewById(R.id.top_bar);
        ImageButton btnBack = topBar.findViewById(R.id.btn_back);
        TextView tvTitle = topBar.findViewById(R.id.tv_title);
        ImageButton btnHelp = topBar.findViewById(R.id.btn_help);
        ImageButton btnOptions = topBar.findViewById(R.id.btn_options);

        tvTitle.setText(getString(R.string.string_options));
        btnBack.setOnClickListener(v -> finish());
        btnHelp.setVisibility(View.GONE);
        btnOptions.setVisibility(View.GONE);
    }

    private void setupControls() {
        SwitchMaterial swVibration = findViewById(R.id.sw_vibration_wrong);
        SwitchMaterial swNightTheme = findViewById(R.id.sw_night_theme);
        SwitchMaterial swFastTimeout = findViewById(R.id.sw_fast_timeout);
        Spinner spinnerLanguage = findViewById(R.id.spinner_language);

        swVibration.setChecked(AppSettings.isVibrateWrongEnabled(this));
        swNightTheme.setChecked(AppSettings.isNightThemeEnabled(this));
        swFastTimeout.setChecked(AppSettings.isFastAnswerTimeoutEnabled(this));

        swVibration.setOnCheckedChangeListener(
                (buttonView, isChecked) -> AppSettings.setVibrateWrongEnabled(this, isChecked)
        );

        swNightTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSettings.setNightThemeEnabled(this, isChecked);
            AppSettings.applyTheme(this);
        });
        swFastTimeout.setOnCheckedChangeListener(
                (buttonView, isChecked) -> AppSettings.setFastAnswerTimeoutEnabled(this, isChecked)
        );

        String[] languages = getResources().getStringArray(R.array.array_languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        spinnerLanguage.setSelection(0, false);
        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                AppSettings.setLanguage(OptionsActivity.this, "en");
                AppSettings.applyLanguage(OptionsActivity.this);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }
}
