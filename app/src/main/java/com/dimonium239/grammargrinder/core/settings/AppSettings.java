package com.dimonium239.grammargrinder.core.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class AppSettings {
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_VIBRATE_WRONG = "vibrate_wrong";
    private static final String KEY_NIGHT_THEME = "night_theme";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_FAST_ANSWER_TIMEOUT = "fast_answer_timeout";

    public static final int ANSWER_DELAY_FAST_MS = 900;
    public static final int ANSWER_DELAY_NORMAL_MS = 1500;

    private AppSettings() {
    }

    public static void applySavedUiSettings(Context context) {
        applyTheme(context);
        applyLanguage(context);
    }

    public static void applyTheme(Context context) {
        int mode = isNightThemeEnabled(context)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static void applyLanguage(Context context) {
        String language = getLanguage(context);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }

    public static boolean isVibrateWrongEnabled(Context context) {
        return prefs(context).getBoolean(KEY_VIBRATE_WRONG, true);
    }

    public static void setVibrateWrongEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_VIBRATE_WRONG, enabled).apply();
    }

    public static boolean isNightThemeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NIGHT_THEME, false);
    }

    public static void setNightThemeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NIGHT_THEME, enabled).apply();
    }

    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, "en");
    }

    public static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply();
    }

    public static boolean isFastAnswerTimeoutEnabled(Context context) {
        return prefs(context).getBoolean(KEY_FAST_ANSWER_TIMEOUT, true);
    }

    public static void setFastAnswerTimeoutEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_FAST_ANSWER_TIMEOUT, enabled).apply();
    }

    public static int getAnswerDelayMs(Context context) {
        return isFastAnswerTimeoutEnabled(context) ? ANSWER_DELAY_FAST_MS : ANSWER_DELAY_NORMAL_MS;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
