package com.mindflow.backend.service;

import java.util.List;

public class IntentTemplates {

    public static final double SIMILARITY_THRESHOLD = 0.45d;

    public record IntentTemplate(String intent, String text) {}

    public static final List<IntentTemplate> ALL = List.of(
            // CHAT — 与学习/知识管理/技术讨论相关的对话 (8条)
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "你好"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "你是谁"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "你能做什么"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "什么是人工智能"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "帮我推荐一本书"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "你有什么功能"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "解释一下什么是微服务架构"),
            new IntentTemplate(IntentRouterService.INTENT_CHAT, "推荐一些学习Java的资源"),

            // SEARCH — 知识库检索 (12条)
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "我笔记里关于微服务的内容"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "查找Spring Boot配置"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "我的笔记中提到过哪些设计模式"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "搜索关于Docker的笔记"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "我记过的关于JVM调优的内容"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "查找数据库索引相关的笔记"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "我的知识库里有关于Redis的内容吗"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "帮我找一下之前记录的算法笔记"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "搜索我笔记中的Kubernetes相关"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "查看关于网络协议的学习笔记"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "找一下我记的关于消息队列的内容"),
            new IntentTemplate(IntentRouterService.INTENT_SEARCH, "我的笔记中关于系统架构的记录"),

            // WRITE — 写作任务 (12条)
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "帮我写一篇文章"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "润色这段话"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "总结一下这篇文章"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "帮我写一封邮件"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "续写这段内容"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "帮我写一份报告"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "扩写这段文字"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "帮我整理一下这些要点"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "写一篇日记"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "帮我起草一份方案"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "改写这段文字的风格"),
            new IntentTemplate(IntentRouterService.INTENT_WRITE, "帮我写一段代码注释")
    );

    private IntentTemplates() {}
}
