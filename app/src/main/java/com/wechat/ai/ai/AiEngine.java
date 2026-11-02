package com.wechat.ai.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wechat.ai.util.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * 大模型调用引擎（OpenAI 兼容协议 + SSE 流式）。
 * 支持内置厂商 / 自定义地址，支持情绪标签回传。
 */
public class AiEngine {

    private static final String TAG = "AiEngine";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern EMO_PATTERN = Pattern.compile("\\[EMO\\](.*?)\\[/EMO\\]", Pattern.DOTALL);

    private static OkHttpClient sClient;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        /** 流式增量（子线程回调） */
        void onDelta(String text);
        /** 完成：full 为去掉情绪标记后的正文，emotion 为情绪标签 */
        void onDone(String full, String emotion);
        void onError(String message);
    }

    private static OkHttpClient client() {
        if (sClient == null) {
            sClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(180, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return sClient;
    }

    /**
     * 情绪标签指令，追加到系统提示词里。
     *
     * 注意：这部分 token 不能省 —— 表情包的模糊匹配（开心≈欢乐≈高兴）
     * 完全依赖 AI 给出的标签质量，指令越清晰标签越准，匹配效果越好。
     * 仅在用户没有上传任何表情包时才返回空串（此时无需判定情绪）。
     */
    public static String emotionInstruction() {
        if (!hasEmoji()) return "";
        return "\n\n【输出要求】正文写完后，另起一行输出本次回复的情绪标签，严格使用格式 [EMO]标签[/EMO]。"
                + "标签只能从以下里选一个最贴切的："
                + "happy, excited, funny, love, thanks, greet, bye, ok, calm, sad, cry, angry,"
                + " fear, disgust, surprised, tired, shy, sorry, think, question。"
                + "请结合你自己这句话的语气来选，不要总是选同一个。"
                + "不要输出任何解释。";
    }

    private static boolean hasEmoji() {
        try {
            return !com.wechat.ai.App.dbHelper().allEmojis().isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void chat(List<ChatMsg> input, Callback cb) {
        chat(input, null, null, cb);
    }

    /**
     * @param tempOverride  本次对话的温度；null 或 &lt;0 表示跟随全局设置
     * @param modelOverride 本次对话的模型；null 或空表示跟随全局设置
     */
    public static void chat(List<ChatMsg> input, Float tempOverride, String modelOverride, Callback cb) {
        String key = Prefs.getApiKey();
        if (key == null || key.trim().isEmpty()) {
            cb.onError("还没有配置 API Key");
            return;
        }

        boolean emotion = Prefs.isEmotionEnabled();
        java.util.List<ChatMsg> msgs = new java.util.ArrayList<>(input);
        if (emotion) {
            boolean hasSystem = false;
            for (ChatMsg m : msgs) {
                if (ChatMsg.ROLE_SYSTEM.equals(m.role)) {
                    m.content = m.content + emotionInstruction();
                    hasSystem = true;
                    break;
                }
            }
            if (!hasSystem) {
                msgs.add(0, ChatMsg.system(Prefs.getSystemPrompt() + emotionInstruction()));
            }
        }

        try {
            JSONArray arr = new JSONArray();
            for (ChatMsg m : msgs) {
                JSONObject o = new JSONObject();
                o.put("role", m.role);
                o.put("content", m.content);
                arr.put(o);
            }
            // 单个 AI 可指定自己的模型与温度，未指定则跟随全局
            String useModel = (modelOverride != null && !modelOverride.trim().isEmpty())
                    ? modelOverride.trim() : Prefs.getModel();
            float useTemp = (tempOverride != null && tempOverride >= 0f)
                    ? tempOverride : Prefs.getTemperature();

            JSONObject body = new JSONObject();
            body.put("model", useModel);
            body.put("messages", arr);
            body.put("temperature", (double) useTemp);
            body.put("stream", Prefs.isStream());
            // 限制单次输出上限：微信聊天不需要长篇大论。
            // 给 800 是为了保证末尾的 [EMO]标签[/EMO] 不会被截断（表情匹配靠它）。
            body.put("max_tokens", 800);

            Request req = new Request.Builder()
                    .url(Prefs.getBaseUrl())
                    .addHeader("Authorization", "Bearer " + key.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            client().newCall(req).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    cb.onError(e.getMessage() != null ? e.getMessage() : "网络异常");
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        String err = "";
                        try {
                            err = response.body() != null ? response.body().string() : "";
                        } catch (Exception ignore) {
                        }
                        response.close();
                        cb.onError("HTTP " + response.code() + " " + err);
                        return;
                    }
                    if (Prefs.isStream()) {
                        readStream(response, cb);
                    } else {
                        readOnce(response, cb);
                    }
                }
            });
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    /**
     * 安全取字符串。
     *
     * 关键坑：org.json 的 optString() 在字段值为 JSON null 时会返回字面量
     * 字符串 "null"，而不是空串。很多厂商流式响应的首个 chunk 是
     * {"delta":{"role":"assistant","content":null}}，直接 optString 就会
     * 把 "null" 拼进正文，于是对话开头出现一堆 null。这里统一挡掉。
     */
    private static String safeStr(JSONObject o, String key) {
        if (o == null || !o.has(key)) return "";
        if (o.isNull(key)) return "";
        String v = o.optString(key, "");
        if (v == null || "null".equals(v) || "NULL".equals(v)) return "";
        return v;
    }

    private static void readOnce(Response response, Callback cb) {
        try {
            String s = response.body() != null ? response.body().string() : "";
            response.close();
            JSONObject json = new JSONObject(s);
            JSONArray arr = json.optJSONArray("choices");
            if (arr == null || arr.length() == 0) {
                cb.onError("解析失败：响应里没有 choices");
                return;
            }
            String content = safeStr(arr.getJSONObject(0).optJSONObject("message"), "content");
            cb.onDelta(content);
            String emo = extractEmotion(content);
            cb.onDone(stripEmotion(content), emo);
        } catch (Exception e) {
            cb.onError("解析失败：" + e.getMessage());
        }
    }

    private static void readStream(Response response, Callback cb) {
        StringBuilder full = new StringBuilder();
        try {
            BufferedSource source = response.body().source();
            String line;
            while (!source.exhausted() && (line = source.readUtf8Line()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                try {
                    JSONObject json = new JSONObject(data);
                    JSONArray choices = json.optJSONArray("choices");
                    if (choices == null || choices.length() == 0) {
                        // 有些厂商会回错误信息
                        if (json.has("error")) {
                            cb.onError(json.optString("error"));
                            break;
                        }
                        continue;
                    }
                    JSONObject choice = choices.getJSONObject(0);
                    // 先取 delta（流式），没有再取 message（部分厂商非标准写法）
                    JSONObject delta = choice.optJSONObject("delta");
                    String piece = safeStr(delta, "content");
                    if (piece.isEmpty()) {
                        piece = safeStr(choice.optJSONObject("message"), "content");
                    }
                    if (piece.isEmpty()) continue;
                    full.append(piece);
                    cb.onDelta(piece);
                } catch (Exception ignore) {
                }
            }
            response.close();
            String content = full.toString();
            cb.onDone(stripEmotion(content), extractEmotion(content));
        } catch (IOException e) {
            cb.onError("读取失败：" + e.getMessage());
        } finally {
            response.close();
        }
    }

    public static String extractEmotion(String text) {
        if (text == null) return "";
        Matcher m = EMO_PATTERN.matcher(text);
        String last = "";
        while (m.find()) {
            last = m.group(1).trim().toLowerCase(Locale.US);
        }
        return last;
    }

    public static String stripEmotion(String text) {
        if (text == null) return "";
        return EMO_PATTERN.matcher(text).replaceAll("").trim();
    }

    /** 测试 Key 是否可用 */
    public static void test(Callback cb) {
        java.util.List<ChatMsg> msgs = new java.util.ArrayList<>();
        msgs.add(ChatMsg.system("你是一个测试助手。"));
        msgs.add(ChatMsg.user("回复两个字：可用"));
        boolean originStream = Prefs.isStream();
        Prefs.putBoolean(Prefs.KEY_STREAM, false);
        chat(msgs, new Callback() {
            @Override
            public void onDelta(String text) {
            }

            @Override
            public void onDone(String full, String emotion) {
                Prefs.putBoolean(Prefs.KEY_STREAM, originStream);
                final String r = full == null ? "" : full;
                MAIN.post(() -> cb.onDone(r, emotion));
            }

            @Override
            public void onError(String message) {
                Prefs.putBoolean(Prefs.KEY_STREAM, originStream);
                MAIN.post(() -> cb.onError(message));
            }
        });
    }

    static void log(String s) {
        Log.d(TAG, s);
    }
}
