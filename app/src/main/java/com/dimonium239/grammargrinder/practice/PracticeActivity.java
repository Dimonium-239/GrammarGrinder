package com.dimonium239.grammargrinder.practice;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.dimonium239.grammargrinder.db.QuestionRepository;
import com.dimonium239.grammargrinder.guides.GuidesSheetsActivity;
import com.dimonium239.grammargrinder.options.OptionsActivity;
import com.dimonium239.grammargrinder.R;
import com.dimonium239.grammargrinder.core.settings.AppSettings;
import com.dimonium239.grammargrinder.databinding.ActivityQuizBinding;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PracticeActivity extends AppCompatActivity {
    private static final String TAG = "PracticeActivity";

    private ActivityQuizBinding binding;
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private String selectedSection;
    private ArrayList<String> selectedTopics = new ArrayList<>();

    private final Handler handler = new Handler();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private int goodCount = 0;
    private int badCount = 0;
    private boolean loadingQuestions = false;
    private static final int BASE_REVIEW_DISTANCE = 7;
    private static final int MIN_REVIEW_DISTANCE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applySavedUiSettings(this);
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        selectedSection = getIntent().getStringExtra("SECTION");
        ArrayList<String> topics = getIntent().getStringArrayListExtra("TOPICS");

        if (topics == null) {
            topics = new ArrayList<>();
        }
        selectedTopics = topics;

        setupOptionButtons();
        setupTopBar();
        updateCounters();
        showLoadingState();
        reloadQuestionsAsync(true);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        backgroundExecutor.shutdownNow();
        super.onDestroy();
    }

    private void setupOptionButtons() {
        binding.btnOption1.setOnClickListener(v -> onAnswerClicked(0));
        binding.btnOption2.setOnClickListener(v -> onAnswerClicked(1));
        binding.btnOption3.setOnClickListener(v -> onAnswerClicked(2));
        binding.btnOption4.setOnClickListener(v -> onAnswerClicked(3));
    }

    private void showQuestion() {
        if (loadingQuestions) {
            return;
        }

        if (questions.isEmpty()) {
            showEmptyState();
            return;
        }

        if (currentIndex >= questions.size()) {
            reloadQuestionsAsync(true);
            return;
        }

        Question q = questions.get(currentIndex);
        String questionText = questionText(q);
        QuestionRepository.recordQuestionShown(this, questionText);
        q.lastSeen = System.currentTimeMillis();
        Log.d(TAG, formatQuestionLog(q));
        binding.tvCategory.setText(formatTopicName(q.topic));
        binding.tvSentence.setText(q.question);
        binding.topBar.btnHelp.setEnabled(true);
        resetButtons();
        setAnswerButtonsEnabled(true);

        List<String> opts = new ArrayList<>(q.options);
        Collections.shuffle(opts);
        MaterialButton[] buttons = {
                binding.btnOption1,
                binding.btnOption2,
                binding.btnOption3,
                binding.btnOption4
        };

        for (int i = 0; i < buttons.length; i++) {
            if (i < opts.size()) {
                buttons[i].setText(opts.get(i));
                buttons[i].setTag(opts.get(i));
                buttons[i].setEnabled(true);
                buttons[i].setVisibility(MaterialButton.VISIBLE);
            } else {
                buttons[i].setVisibility(MaterialButton.INVISIBLE);
            }
        }
    }

    private void onAnswerClicked(int index) {
        if (!hasCurrentQuestion()) {
            showEmptyState();
            return;
        }

        Question q = questions.get(currentIndex);
        MaterialButton[] buttons = {
                binding.btnOption1,
                binding.btnOption2,
                binding.btnOption3,
                binding.btnOption4
        };

        MaterialButton clicked = buttons[index];
        String answer = (String) clicked.getTag();

        boolean correct = answer.equals(q.correctAnswer);
        int correctColor = ContextCompat.getColor(this, R.color.quiz_answer_correct);
        int wrongColor = ContextCompat.getColor(this, R.color.quiz_answer_wrong);

        for (MaterialButton b : buttons) {
            b.setEnabled(false);
        }

        if (correct) {
            clicked.setBackgroundColor(correctColor);
        } else {
            clicked.setBackgroundColor(wrongColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrateOnWrongAnswer(clicked);
            }
            for (MaterialButton b : buttons) {
                String a = (String) b.getTag();
                if (q.correctAnswer.equals(a)) {
                    b.setBackgroundColor(correctColor);
                }
            }
        }
        if (correct) {
            goodCount++;
            if (q.mistakeCount > 0) {
                q.mistakeCount -= 1;
            }
            q.complexity = QuestionRepository.deriveComplexity(q.mistakeCount);
        } else {
            badCount++;
            q.mistakeCount += 1;
            q.complexity = QuestionRepository.deriveComplexity(q.mistakeCount);
            scheduleFailedQuestion(q);
        }
        QuestionRepository.recordAnswerResult(this, questionText(q), correct);
        updateCounters();

        handler.postDelayed(() -> {
            currentIndex++;
            showQuestion();
        }, AppSettings.getAnswerDelayMs());
    }

    private void resetButtons() {
        MaterialButton[] buttons = {
                binding.btnOption1,
                binding.btnOption2,
                binding.btnOption3,
                binding.btnOption4
        };

        for (MaterialButton b : buttons) {
            b.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            b.setEnabled(true);
        }
    }

    private void setupTopBar() {

        binding.topBar.btnBack.setOnClickListener(v -> finish());

        binding.topBar.btnHelp.setOnClickListener(v -> {
            if (!hasCurrentQuestion()) {
                return;
            }
            Question q = questions.get(currentIndex);
            openGuideForQuestion(q);
        });
        binding.topBar.btnOptions.setOnClickListener(
                v -> startActivity(new android.content.Intent(this, OptionsActivity.class))
        );

        binding.topBar.tvTitle.setText(getString(R.string.string_practice));
    }

    private void updateCounters() {
        binding.tvCounterGood.setText(getString(R.string.string_counter_good, goodCount));
        binding.tvCounterBad.setText(getString(R.string.string_counter_bad, badCount));
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void vibrateOnWrongAnswer(View sourceView) {
        if (!AppSettings.isVibrateWrongEnabled(this)) {
            return;
        }

        // Fallback haptic feedback for devices/configurations where app vibration is blocked.
        sourceView.performHapticFeedback(
                HapticFeedbackConstants.REJECT,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        );

        Vibrator vibrator = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            VibratorManager manager = getSystemService(VibratorManager.class);
            if (manager != null) {
                vibrator = manager.getDefaultVibrator();
            }
        } else {
            vibrator = getSystemService(Vibrator.class);
        }

        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        long[] pattern = new long[]{0, 130, 40, 130, 40, 220};
        try {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } catch (SecurityException ignored) {
            // Keep gameplay smooth; haptic feedback fallback above already executed.
        }
    }

    private void openGuideForQuestion(Question q) {
        android.content.Intent intent = new android.content.Intent(this, GuidesSheetsActivity.class);
        String topicId = q.topic == null ? "" : q.topic;
        if (!topicId.isEmpty()) {
            intent.putExtra(GuidesSheetsActivity.EXTRA_TOPIC_ID, topicId);
        }
        startActivity(intent);
    }

    private void scheduleFailedQuestion(Question failedQuestion) {
        String failedKey = questionText(failedQuestion);
        for (int i = questions.size() - 1; i > currentIndex; i--) {
            if (failedKey.equals(questionText(questions.get(i)))) {
                questions.remove(i);
            }
        }

        int complexityPenalty = Math.max(0, failedQuestion.complexity - 1);
        int mistakePenalty = Math.min(5, Math.max(0, failedQuestion.mistakeCount - 1));
        int distance = Math.max(MIN_REVIEW_DISTANCE, BASE_REVIEW_DISTANCE - complexityPenalty - mistakePenalty);
        int targetIndex = Math.min(currentIndex + distance, questions.size());
        questions.add(targetIndex, failedQuestion);
    }

    private String questionText(Question q) {
        return q.question == null ? "" : q.question;
    }

    private void reloadQuestionsAsync(boolean resetIndex) {
        if (loadingQuestions) {
            return;
        }

        loadingQuestions = true;
        showLoadingState();
        backgroundExecutor.execute(() -> {
            List<Question> loadedQuestions;
            if (selectedSection == null || selectedSection.isEmpty()) {
                loadedQuestions = QuestionRepository.loadQuestionsMix(this, selectedTopics);
            } else {
                loadedQuestions = QuestionRepository.loadQuestions(this, selectedSection, selectedTopics);
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                questions = loadedQuestions;
                if (resetIndex) {
                    currentIndex = 0;
                }
                loadingQuestions = false;
                showQuestion();
            });
        });
    }

    private boolean hasCurrentQuestion() {
        return currentIndex >= 0 && currentIndex < questions.size();
    }

    private void showLoadingState() {
        binding.tvCategory.setText("");
        binding.tvSentence.setText(getString(R.string.string_loading_content));
        setAnswerButtonsEnabled(false);
        binding.topBar.btnHelp.setEnabled(false);
    }

    private void showEmptyState() {
        currentIndex = 0;
        binding.tvCategory.setText("");
        binding.tvSentence.setText(getString(R.string.string_no_questions_found));
        setAnswerButtonsEnabled(false);
        binding.topBar.btnHelp.setEnabled(false);
    }

    private void setAnswerButtonsEnabled(boolean enabled) {
        MaterialButton[] buttons = {
                binding.btnOption1,
                binding.btnOption2,
                binding.btnOption3,
                binding.btnOption4
        };

        for (MaterialButton button : buttons) {
            button.setEnabled(enabled);
            button.setVisibility(enabled ? MaterialButton.VISIBLE : MaterialButton.INVISIBLE);
            if (!enabled) {
                button.setTag(null);
            }
        }
    }

    private String formatQuestionLog(Question q) {
        return "[QUESTION] question=\"" + q.question + "\"" +
                ", section=\"" + q.section + "\"" +
                ", topic=\"" + q.topic + "\"" +
                ", complexity=" + q.complexity +
                ", lastSeen=" + q.lastSeen +
                ", mistakeCount=" + q.mistakeCount;
    }

    private String formatTopicName(String rawTopic) {
        if (rawTopic == null || rawTopic.isEmpty()) {
            return "";
        }
        String[] parts = rawTopic.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return builder.toString().trim();
    }

}
