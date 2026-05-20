package com.mindflow.backend.service;

import com.mindflow.backend.dto.IntentResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {

    private final EmbeddingModel embeddingModel;

    private List<float[]> templateEmbeddings;
    private List<String> templateIntents;

    @PostConstruct
    public void init() {
        List<IntentTemplates.IntentTemplate> templates = IntentTemplates.ALL;
        templateIntents = new ArrayList<>(templates.size());
        List<String> texts = new ArrayList<>(templates.size());
        for (IntentTemplates.IntentTemplate t : templates) {
            templateIntents.add(t.intent());
            texts.add(t.text());
        }

        // API batch limit is 32, split into chunks
        templateEmbeddings = new ArrayList<>();
        int batchSize = 32;
        for (int i = 0; i < texts.size(); i += batchSize) {
            templateEmbeddings.addAll(embeddingModel.embed(texts.subList(i, Math.min(i + batchSize, texts.size()))));
        }

        log.info("IntentClassifier initialized with {} templates, dimension={}",
                templateEmbeddings.size(), templateEmbeddings.isEmpty() ? 0 : templateEmbeddings.get(0).length);
    }

    public IntentResult classify(String userMessage) {
        try {
            float[] queryArr = embeddingModel.embed(userMessage);

            int bestIdx = -1;
            double bestScore = -1;

            for (int i = 0; i < templateEmbeddings.size(); i++) {
                double sim = cosineSimilarity(queryArr, templateEmbeddings.get(i));
                if (sim > bestScore) {
                    bestScore = sim;
                    bestIdx = i;
                }
            }

            if (bestIdx < 0 || bestScore < IntentTemplates.SIMILARITY_THRESHOLD) {
                log.info("IntentClassifier: query='{}' bestScore={}, below threshold={} → REFUSE",
                        userMessage, String.format("%.4f", bestScore), IntentTemplates.SIMILARITY_THRESHOLD);
                return new IntentResult(IntentRouterService.INTENT_REFUSE, userMessage);
            }

            String matchedIntent = templateIntents.get(bestIdx);
            String query = IntentRouterService.INTENT_SEARCH.equals(matchedIntent) ? userMessage : userMessage;

            log.info("IntentClassifier: query='{}' → intent={}, score={}",
                    userMessage, matchedIntent, String.format("%.4f", bestScore));
            return new IntentResult(matchedIntent, query);
        } catch (Exception e) {
            log.error("IntentClassifier: embedding failed, fallback to CHAT", e);
            return new IntentResult(IntentRouterService.INTENT_CHAT, userMessage);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }
}
