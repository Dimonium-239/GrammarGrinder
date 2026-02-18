package com.dimonium239.grammargrinder.practice;

import java.util.List;

public class Question {
    public String id;
    public String category;
    public String sectionId;
    public String topicId;
    public int complexity;

    public String question;
    public List<String> options;
    public String correctAnswer;
    public String explanation;

    public int priority;
    public long lastSeen;
    public int mistakeCount;
}
