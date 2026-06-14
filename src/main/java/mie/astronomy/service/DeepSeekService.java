package mie.astronomy.service;

import mie.astronomy.dto.AnalyzeResponse;

public interface DeepSeekService {
    /**
     * 分析任务：生成SQL、执行并总结结果
     * @param task 任务描述
     * @return 分析结果
     */
    AnalyzeResponse analyze(String task);

    /**
     * 通用对话补全：发送 system + user 提示词，返回模型回复文本。
     * 复用本服务已配置的 LLM（DeepSeek）凭据，避免前端直连产生跨域问题。
     *
     * @param systemPrompt 系统提示词（可为空）
     * @param userPrompt   用户提示词
     * @param temperature  采样温度
     * @return 模型回复内容（失败时返回空字符串）
     */
    String chat(String systemPrompt, String userPrompt, double temperature);
}

