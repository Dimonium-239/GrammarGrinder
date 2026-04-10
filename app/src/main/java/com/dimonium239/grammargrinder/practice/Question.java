package com.dimonium239.grammargrinder.practice;

import java.util.List;

public class Question {
    public String section;
    public String topic;
    public int complexity;

    public String question;
    public List<String> options;
    public String correctAnswer;

    public long lastSeen;
    public int mistakeCount;
}
