package com.wechat.ai.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置各家大模型接口（统一走 OpenAI 兼容协议）。默认 DeepSeek。
 */
public class ModelRegistry {

    private static final List<ModelPreset> PRESETS = new ArrayList<>();

    static {
        // 默认厂商放第一位
        PRESETS.add(new ModelPreset("deepseek", "DeepSeek（默认）",
                "https://api.deepseek.com/v1/chat/completions",
                new String[]{"deepseek-chat", "deepseek-reasoner"}, "deepseek-chat",
                "platform.deepseek.com", false));

        PRESETS.add(new ModelPreset("zhipu", "智谱 GLM",
                "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                new String[]{"glm-4-flash", "glm-4-air", "glm-4-plus", "glm-4.6"}, "glm-4-flash",
                "open.bigmodel.cn", false));

        PRESETS.add(new ModelPreset("moonshot", "Moonshot / Kimi",
                "https://api.moonshot.cn/v1/chat/completions",
                new String[]{"moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k", "kimi-k2-0905"},
                "moonshot-v1-8k", "platform.moonshot.cn", false));

        PRESETS.add(new ModelPreset("qwen", "阿里通义千问",
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
                new String[]{"qwen-plus", "qwen-max", "qwen-turbo", "qwen-flash", "qwen2.5-72b-instruct"},
                "qwen-plus", "bailian.console.aliyun.com", false));

        PRESETS.add(new ModelPreset("doubao", "火山方舟（豆包）",
                "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
                new String[]{"doubao-pro-32k", "doubao-lite-32k", "doubao-1.5-pro-32k"},
                "doubao-pro-32k", "console.volcengine.com/ark", true));

        PRESETS.add(new ModelPreset("hunyuan", "腾讯混元",
                "https://api.hunyuan.cloud.tencent.com/v1/chat/completions",
                new String[]{"hunyuan-turbo", "hunyuan-pro", "hunyuan-standard"},
                "hunyuan-turbo", "cloud.tencent.com/product/hunyuan", false));

        PRESETS.add(new ModelPreset("ernie", "百度文心（千帆）",
                "https://qianfan.baidubce.com/v2/chat/completions",
                new String[]{"ernie-4.0-8k", "ernie-4.0-turbo-8k", "ernie-speed-128k", "ernie-lite-8k"},
                "ernie-speed-128k", "console.bce.baidu.com/qianfan", false));

        PRESETS.add(new ModelPreset("spark", "讯飞星火",
                "https://spark-api-open.xf-yun.com/v1/chat/completions",
                new String[]{"lite", "generalv3", "pro-128k", "4.0Ultra", "max-32k"},
                "generalv3", "console.xfyun.cn", false));

        PRESETS.add(new ModelPreset("siliconflow", "硅基流动 SiliconFlow",
                "https://api.siliconflow.cn/v1/chat/completions",
                new String[]{"Qwen/Qwen2.5-7B-Instruct", "deepseek-ai/DeepSeek-V3",
                        "THUDM/glm-4-9b-chat", "meta-llama/Meta-Llama-3.1-8B-Instruct"},
                "Qwen/Qwen2.5-7B-Instruct", "cloud.siliconflow.cn", false));

        PRESETS.add(new ModelPreset("openai", "OpenAI",
                "https://api.openai.com/v1/chat/completions",
                new String[]{"gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "o4-mini"},
                "gpt-4o-mini", "platform.openai.com", false));

        PRESETS.add(new ModelPreset("openrouter", "OpenRouter（聚合）",
                "https://openrouter.ai/api/v1/chat/completions",
                new String[]{"deepseek/deepseek-chat", "openai/gpt-4o-mini",
                        "anthropic/claude-3.5-sonnet", "google/gemini-2.0-flash-001"},
                "deepseek/deepseek-chat", "openrouter.ai", false));

        PRESETS.add(new ModelPreset("groq", "Groq",
                "https://api.groq.com/openai/v1/chat/completions",
                new String[]{"llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it"},
                "llama-3.3-70b-versatile", "console.groq.com", false));

        PRESETS.add(new ModelPreset("ollama", "本地 Ollama",
                "http://10.0.2.2:11434/v1/chat/completions",
                new String[]{"qwen2.5", "llama3.2", "deepseek-r1:7b", "glm4"},
                "qwen2.5", "本机部署，模拟器填 10.0.2.2，真机填电脑局域网 IP", false));

        PRESETS.add(new ModelPreset("custom", "自定义（OpenAI 兼容）",
                "https://api.deepseek.com/v1/chat/completions",
                new String[]{"deepseek-chat"}, "deepseek-chat",
                "自行填写接口地址与模型名", true));
    }

    public static List<ModelPreset> all() {
        return PRESETS;
    }

    public static ModelPreset get(String id) {
        for (ModelPreset p : PRESETS) {
            if (p.id.equals(id)) return p;
        }
        return PRESETS.get(0);
    }
}
