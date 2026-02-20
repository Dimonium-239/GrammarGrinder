package com.dimonium239.grammargrinder.db;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private Converters() {
    }

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) {
            return "[]";
        }
        return GSON.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = GSON.fromJson(value, LIST_TYPE);
        return result == null ? new ArrayList<>() : result;
    }
}
