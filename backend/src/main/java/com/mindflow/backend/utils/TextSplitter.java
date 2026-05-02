package com.mindflow.backend.utils;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {
    public static List<String> split(String content, int maxTokens) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) return chunks;
        int charsPerToken = 4; // Sensible approx for English
        int chunkSize = maxTokens * charsPerToken;
        for (int i = 0; i < content.length(); i += chunkSize) {
            chunks.add(content.substring(i, Math.min(content.length(), i + chunkSize)));
        }
        return chunks;
    }
}
