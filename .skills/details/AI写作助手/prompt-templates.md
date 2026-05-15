# AI 写作助手 — Prompt 模板

## 续写

> 位置: `service/AIWriteService.java:18`

```java
"Please continue writing the following markdown note: " + content
```

## 润色

> 位置: `service/AIWriteService.java:19`

```java
"Polish the following markdown content to be more fluent and professional: " + content
```

## 摘要

> 位置: `service/AIWriteService.java:20`

```java
"Summarize the following markdown content in under 100 words: " + content
```

> **待优化：** 当前 Prompt 仅为简单的字符串拼接，建议后续重构为 SystemMessage + UserMessage 结构以提升可控性。
