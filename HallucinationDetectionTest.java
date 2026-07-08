package com.study;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.model.DetectRequest;
import com.study.model.DetectResponse;
import com.study.model.HallucinationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(classes = DemoApplication.class)
public class HallucinationDetectionTest {

    @Autowired
    private DetectService detectService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void detectAllReplies() throws Exception {
        // 1. 读取 resources/task4_replies.json
        ClassPathResource resource = new ClassPathResource("task4_replies.json");
        InputStream inputStream = resource.getInputStream();
        List<Map<String, String>> records = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Map<String, String>>>() {
                }
        );

        System.out.println("===== start =====");
        int total = records.size();
        int hallucinationCount = 0;
/// 用于收集结果的对象列表（将写入文件）
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Map<String, String> record : records) {
            String id = record.get("id");
            String userQuestion = record.get("user_question");
            String systemReply = record.get("system_reply");
            String knowledgeBase = record.get("knowledge_base");

            // 2. 构造请求
            DetectRequest request = new DetectRequest();
            request.setUserQuestion(userQuestion);
            request.setSystemReply(systemReply);
            request.setKnowledgeBase(knowledgeBase);

            // 3. 调用检测服务（会调用真实 LLM）
            DetectResponse response = detectService.detect(request);

            // 4. 统计幻觉数量
            if (response.isHallucination()) {
                hallucinationCount++;
            }

            // 5. 打印结果
            System.out.printf("ID: %s | 是否幻觉: %s | 类型: %s | 原因: %s%n",
                    id,
                    response.isHallucination() ? "✅是" : "❌否",
                    response.getType(),
                    response.getReason());

            Map<String, Object> resultEntry = new LinkedHashMap<>();
            resultEntry.put("id", id);
            resultEntry.put("user_question", userQuestion);
            resultEntry.put("system_reply", systemReply);
            resultEntry.put("knowledge_base", knowledgeBase);
            resultEntry.put("is_hallucination", response.isHallucination());
            String typeChinese = convertTypeToChinese(response.getType());
            resultEntry.put("hallucination_type", typeChinese);
            resultEntry.put("reason", response.getReason());

            resultList.add(resultEntry);
        }
// 统计信息
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("hallucination_count", hallucinationCount);
        summary.put("normal_count", total - hallucinationCount);

        // 构造最终输出对象（包含结果列表和摘要）
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", summary);
        output.put("results", resultList);

        // 将结果写入文件（项目根目录下的 detection_results.json）
        Path outputPath = Paths.get("detection_results.json");
        objectMapper.writeValue(outputPath.toFile(), output);

        System.out.println("===== end =====");
        System.out.printf("总计: %d 条，幻觉: %d 条，正常: %d 条%n",
                total, hallucinationCount, total - hallucinationCount);
        System.out.printf("结果已写入文件: %s%n", outputPath.toAbsolutePath());
    }

    @Test
    void evaluateAndWriteToFile() throws Exception {
        // 1. 加载 ground_truth
        ClassPathResource gtResource = new ClassPathResource("task4_ground_truth.json");
        List<Map<String, Object>> groundTruthList = objectMapper.readValue(
                gtResource.getInputStream(),
                new TypeReference<List<Map<String, Object>>>() {}
        );
        Map<String, Boolean> expectedMap = new LinkedHashMap<>();
        Map<String, String> expectedTypeMap = new LinkedHashMap<>();
        for (Map<String, Object> gt : groundTruthList) {
            String id = (String) gt.get("id");
            Boolean isHallucination = (Boolean) gt.get("is_hallucination");
            expectedMap.put(id, isHallucination);
            // 如果存在hallucination_type，也记录下来（仅用于展示）
            if (gt.containsKey("hallucination_type") && gt.get("hallucination_type") != null) {
                expectedTypeMap.put(id, (String) gt.get("hallucination_type"));
            }
        }

        // 2. 加载 replies
        ClassPathResource repliesResource = new ClassPathResource("task4_replies.json");
        List<Map<String, String>> replies = objectMapper.readValue(
                repliesResource.getInputStream(),
                new TypeReference<List<Map<String, String>>>() {}
        );

        System.out.println("===== 开始评估检测（与Ground Truth对比） =====");

        // 用于存储每个case的对比结果
        List<Map<String, Object>> caseResults = new ArrayList<>();
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (Map<String, String> record : replies) {
            String id = record.get("id");
            DetectRequest request = new DetectRequest();
            request.setUserQuestion(record.get("user_question"));
            request.setSystemReply(record.get("system_reply"));
            request.setKnowledgeBase(record.get("knowledge_base"));

            // 调用检测
            DetectResponse response = detectService.detect(request);
            boolean predicted = response.isHallucination();
            boolean expected = expectedMap.getOrDefault(id, false);

            // 统计
            if (predicted && expected) tp++;
            else if (predicted && !expected) fp++;
            else if (!predicted && !expected) tn++;
            else if (!predicted && expected) fn++;

            // 构建单个case结果（包含中文类型）
            Map<String, Object> caseResult = new LinkedHashMap<>();
            caseResult.put("id", id);
            caseResult.put("user_question", record.get("user_question"));
            caseResult.put("system_reply", record.get("system_reply"));
            caseResult.put("knowledge_base", record.get("knowledge_base"));
            caseResult.put("predicted_hallucination", predicted);
            caseResult.put("predicted_type", convertTypeToChinese(response.getType()));
            caseResult.put("predicted_reason", response.getReason());
            caseResult.put("expected_hallucination", expected);
            caseResult.put("expected_type", expectedTypeMap.get(id));  // 可能为null（部分没有类型）
            caseResult.put("match", (predicted == expected));

            caseResults.add(caseResult);

            // 控制台打印简要信息
            System.out.printf("ID: %s | 预测: %s | 真实: %s | %s%n",
                    id,
                    predicted ? "幻觉" : "正常",
                    expected ? "幻觉" : "正常",
                    (predicted == expected) ? "✅" : "❌");
        }

        // 3. 计算指标
        double precision = (tp + fp) == 0 ? 0 : (double) tp / (tp + fp);
        double recall = (tp + fn) == 0 ? 0 : (double) tp / (tp + fn);
        double accuracy = (double) (tp + tn) / (tp + fp + tn + fn);
        double f1 = (precision + recall) == 0 ? 0 : 2 * precision * recall / (precision + recall);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("true_positive", tp);
        metrics.put("false_positive", fp);
        metrics.put("true_negative", tn);
        metrics.put("false_negative", fn);
        metrics.put("precision", String.format("%.2f%%", precision * 100));
        metrics.put("recall", String.format("%.2f%%", recall * 100));
        metrics.put("accuracy", String.format("%.2f%%", accuracy * 100));
        metrics.put("f1_score", String.format("%.2f", f1));

        // 4. 构建完整输出
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", metrics);
        output.put("cases", caseResults);

        // 5. 写入文件
        Path outputPath = Paths.get("evaluation_results.json");
        objectMapper.writeValue(outputPath.toFile(), output);

        System.out.println("===== 评估完成 =====");
        System.out.printf("TP: %d, FP: %d, TN: %d, FN: %d%n", tp, fp, tn, fn);
        System.out.printf("精确率: %.2f%%, 召回率: %.2f%%, 准确率: %.2f%%, F1: %.2f%n",
                precision * 100, recall * 100, accuracy * 100, f1);
        System.out.printf("评估结果已写入文件: %s%n", outputPath.toAbsolutePath());
    }

    private String convertTypeToChinese(HallucinationType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case POLICY_FABRICATION:
                return "政策与规则编造";
            case PRODUCT_PARAM_FABRICATION:
                return "产品参数编造";
            case CAPABILITY_OVERSTEP:
                return "能力越界";
            case SAFETY_MISLEADING:
                return "安全与合规误导";
            case INFORMATION_MISSING:
                return "信息遗漏与错误";
            case NONE:
                return "无幻觉";
            default:
                return type.name(); // 保底
        }
    }

}