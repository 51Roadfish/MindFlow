package com.mindflow.backend.utils;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {
    public static List<String> split(String content, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        for (int i = 0; i < content.length(); i += maxChars) {
            chunks.add(content.substring(i, Math.min(content.length(), i + maxChars)));
        }
        return chunks;
    }
}
