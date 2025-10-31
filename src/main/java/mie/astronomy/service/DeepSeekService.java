package mie.astronomy.service;

import mie.astronomy.dto.AnalyzeResponse;

public interface DeepSeekService {
    /**
     * 分析任务：生成SQL、执行并总结结果
     * @param task 任务描述
     * @return 分析结果
     */
    AnalyzeResponse analyze(String task);
}

