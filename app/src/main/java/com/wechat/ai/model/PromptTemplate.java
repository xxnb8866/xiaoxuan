package com.wechat.ai.model;

/**
 * 提示词模板：用户自定义名称后保存，可在创建/编辑 AI 时一键套用。
 */
public class PromptTemplate {

    public long id;
    /** 用户自定义的名称 */
    public String name;
    /** 提示词正文 */
    public String content;
    public long ts;

    public PromptTemplate() {
    }

    public PromptTemplate(String name, String content) {
        this.name = name;
        this.content = content;
        this.ts = System.currentTimeMillis();
    }
}
