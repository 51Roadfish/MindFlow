package com.mindflow.backend.service;

import com.mindflow.backend.dto.IntentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRouterService {

    private final IntentClassifier intentClassifier;
    private final GuardrailService guardrailService;

    public static final String INTENT_CHAT = "CHAT";
    public static final String INTENT_SEARCH = "SEARCH";
    public static final String INTENT_WRITE = "WRITE";
    public static final String INTENT_REFUSE = "REFUSE";

    public IntentResult analyze(String userMessage) {
        // 第一步: Guardrail 检测 — 正则匹配注入/越狱模式
        if (!guardrailService.check(userMessage)) {
            log.warn("Guardrail triggered for message: {}", userMessage);
            return new IntentResult(INTENT_REFUSE, userMessage);
        }

        // 第二步: Embedding + kNN 分类
        return intentClassifier.classify(userMessage);
    }
}
