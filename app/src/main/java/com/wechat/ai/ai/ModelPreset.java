package com.wechat.ai.ai;

/**
 * 内置模型厂商预设。用户只需要选一个厂商、填 Key、选模型即可，不用手敲接口地址。
 */
public class ModelPreset {

    public String id;
    public String name;
    /** OpenAI 兼容的 chat completions 地址 */
    public String baseUrl;
    /** 该厂商常用模型，直接下拉选择 */
    public String[] models;
    /** 默认模型 */
    public String defaultModel;
    /** 申请 Key 的地址说明 */
    public String keySite;
    /** 火山方舟之类的平台需要用户填推理接入点 ID */
    public boolean needEndpointId;

    public ModelPreset(String id, String name, String baseUrl, String[] models, String defaultModel,
                       String keySite, boolean needEndpointId) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.models = models;
        this.defaultModel = defaultModel;
        this.keySite = keySite;
        this.needEndpointId = needEndpointId;
    }
}
