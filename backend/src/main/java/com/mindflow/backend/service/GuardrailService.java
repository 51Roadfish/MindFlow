package com.mindflow.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GuardrailService {

    private static final List<Pattern> BLOCKED_PATTERNS = List.of(
            // 中文注入
            Pattern.compile("忽略.*(?:指令|提示|规则|设定|命令|system|之前的)"),
            Pattern.compile("无视.*(?:指令|提示|规则|设定|命令|system|之前的)"),
            Pattern.compile("假装你是|扮演|角色扮演"),
            Pattern.compile("越狱|突破限制|解除限制"),
            Pattern.compile("请你(?:忘记|忽略|不要管).*(?:指令|规则|身份)"),
            Pattern.compile("你被(?:解放|释放|解除了)"),
            Pattern.compile("现在你是|从现在起你是"),
            Pattern.compile("请(?:输出|显示|告诉我).*(?:密码|密钥|token|secret)"),
            Pattern.compile("帮我(?:攻击|入侵|破解|钓鱼|诈骗)"),
            Pattern.compile("如何(?:制作|制造|合成)(?:毒品|炸弹|武器|病毒)"),

            // English injection
            Pattern.compile("ignore all (?:previous )?instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget (?:all )?(?:previous )?instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore (?:above|prior|everything)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system override", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are (?:now |no longer )?(?:free|released|jailbroken)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("DAN|do anything now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bypass (?:safety|security|restriction)", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 检查用户消息是否命中 guardrail 规则
     * @return true = 消息合法，false = 消息被拦截
     */
    public boolean check(String message) {
        if (message == null || message.isBlank()) {
            return true;
        }
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(message).find()) {
                log.warn("Guardrail 拦截命中: pattern={}, message={}", pattern.pattern(), message);
                return false;
            }
        }
        return true;
    }
}
