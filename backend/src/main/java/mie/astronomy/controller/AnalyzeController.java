package mie.astronomy.controller;

import mie.astronomy.dto.AnalyzeResponse;
import mie.astronomy.dto.QueryRequest;
import mie.astronomy.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analyze")
public class AnalyzeController {

    @Autowired
    private DeepSeekService deepSeekService;

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("status", "ok", "message", "FastAPI服务运行正常");
    }

    @GetMapping("/health")
    public Map<String, String> healthCheck() {
        return Map.of("status", "healthy", "service", "ai-agent-api");
    }

    @PostMapping("")
    public AnalyzeResponse analyze(@RequestBody QueryRequest request) {
        return deepSeekService.analyze(request.getTask());
    }
}

