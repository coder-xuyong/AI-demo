package com.study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.model.DetectRequest;
import com.study.model.DetectResponse;
import com.study.model.HallucinationType;
import org.springframework.ai.chat.client.ChatClient; // 注意这个包路径
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DetectService {

    // 1. 注入 ChatClient.Builder，而不是 ChatClient
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DetectResponse detect(DetectRequest request) {
        // 2. 通过 Builder 构建 ChatClient 实例
        ChatClient chatClient = chatClientBuilder.build();

        // 3. 构建 prompt
        String promptText = buildPrompt(request);

        // 4. 使用 Fluent API 调用 LLM
        String llmOutput = chatClient.prompt()
                .user(promptText)      // 设置用户消息
                .call()                // 发送请求
                .content();            // 获取响应内容 (String)

        // 5. 解析 JSON 返回
        try {
            return objectMapper.readValue(llmOutput, DetectResponse.class);
        } catch (Exception e) {
            DetectResponse fallback = new DetectResponse();
            fallback.setHallucination(false);
            fallback.setType(HallucinationType.NONE);
            fallback.setReason("LLM 输出解析失败: " + llmOutput);
            return fallback;
        }
    }

    // buildPrompt 方法保持不变
    private String buildPrompt(DetectRequest request) {
        return """
                你是一个客服回复质检专家。请根据以下信息判断系统回复是否与知识库一致，是否存在幻觉。
                用户问题: %s
                系统回复: %s
                知识库: %s
                判断要求:
                - 若回复内容与知识库明显矛盾，或声称不存在的能力，或遗漏关键安全信息，则视为幻觉。
                - 若完全一致或仅有关无痛痒的措辞差异，则视为正常。
                输出格式（严格 JSON）:
                {"hallucination": true/false, "type": "类型（若无则 NONE）", "reason": "简要理由"}
                类型可选: POLICY_FABRICATION, PRODUCT_PARAM_FABRICATION, CAPABILITY_OVERSTEP, SAFETY_MISLEADING, INFORMATION_MISSING, NONE
                """.formatted(request.getUserQuestion(), request.getSystemReply(), request.getKnowledgeBase());
    }
}