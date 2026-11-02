package com.wechat.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 朋友圈一条动态。
 */
public class Moment {
    public long id;
    public String author;
    public int avatarColor;
    public String text;
    /** JSON 数组字符串，元素为图片路径 */
    public String images;
    public long ts;
    public int liked;
    /** 点赞人，逗号分隔 */
    public String likes;
    /** 评论 JSON 数组 */
    public String comments;

    public static class Comment {
        public String who;
        public String content;

        public Comment(String who, String content) {
            this.who = who;
            this.content = content;
        }
    }

    private transient List<Comment> commentList;

    public List<Comment> getComments() {
        if (commentList == null) {
            commentList = new ArrayList<>();
            if (comments != null && !comments.isEmpty()) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(comments);
                    for (int i = 0; i < arr.length(); i++) {
                        org.json.JSONObject o = arr.getJSONObject(i);
                        commentList.add(new Comment(o.optString("who"), o.optString("content")));
                    }
                } catch (Exception ignore) {
                }
            }
        }
        return commentList;
    }

    public List<String> imageList() {
        List<String> list = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(images);
                for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
            } catch (Exception ignore) {
            }
        }
        return list;
    }

    public void addComment(Comment c) {
        getComments().add(c);
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Comment x : commentList) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("who", x.who);
                o.put("content", x.content);
                arr.put(o);
            }
            comments = arr.toString();
        } catch (Exception ignore) {
        }
    }
}
