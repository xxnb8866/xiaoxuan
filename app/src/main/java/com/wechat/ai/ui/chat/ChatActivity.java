package com.wechat.ai.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.ai.AiEngine;
import com.wechat.ai.ai.ChatMsg;
import com.wechat.ai.emoji.EmojiMatcher;
import com.wechat.ai.model.EmojiItem;
import com.wechat.ai.model.Message;
import com.wechat.ai.ui.emoji.EmojiManageActivity;
import com.wechat.ai.ui.pay.RedPacketOpenActivity;
import com.wechat.ai.ui.pay.RedPacketSendActivity;
import com.wechat.ai.ui.pay.TransferActivity;
import com.wechat.ai.util.ImageUtils;
import com.wechat.ai.util.Prefs;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 聊天界面：文本 / 图片 / 表情 / 语音 / 红包 / 转账，AI 流式回复 + 情绪表情包自动匹配。
 */
public class ChatActivity extends AppCompatActivity {

    /** 每次请求携带的最近历史条数（多了烧 token） */
    private static final int HISTORY_MAX = 8;


    public static final String EXTRA_CONV_ID = "conv_id";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_IS_AI = "is_ai";

    private String convId = "ai";
    private String name = "AI 助手";
    private boolean isAi = true;
    /** 当前会话（含该 AI 的人设 / 温度 / 模型） */
    private com.wechat.ai.model.Conversation conv;

    private RecyclerView recycler;
    private final List<Message> data = new ArrayList<>();
    private MessageAdapter adapter;
    private EditText etInput;
    private TextView btnSend;
    private FrameLayout panel;
    private RecyclerView emojiPanel;
    private RecyclerView plusPanel;
    private TextView subtitle;

    private final List<EmojiItem> emojiList = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    /** AI 上下文里待注入的红包/转账信息 */
    private final StringBuilder pendingContext = new StringBuilder();

    private Message streaming;
    private int aiReplyCount = 0;
    /** 用户上一句话的本地情绪检测结果，作为表情匹配的兜底 */
    private String lastUserEmotion = "";
    private String lastAiEmotion = "";
    private String lastPhotoQuery = "";

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String path = ImageUtils.copyToPrivate(this, uri);
                if (path == null) {
                    Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendImage(path);
            });

    private final ActivityResultLauncher<Intent> sendRedPacket =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                Intent d = r.getData();
                String amount = d.getStringExtra("amount");
                String wish = d.getStringExtra("wish");
                String theme = d.getStringExtra("theme");
                sendRedPacketMessage(amount, wish, theme);
            });

    private final ActivityResultLauncher<Intent> sendTransfer =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                Intent d = r.getData();
                String amount = d.getStringExtra("amount");
                String note = d.getStringExtra("note");
                sendTransferMessage(amount, note);
            });

    private final ActivityResultLauncher<Intent> openRedPacket =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                long id = r.getData().getLongExtra("msg_id", -1);
                String amount = r.getData().getStringExtra("amount");
                String wish = r.getData().getStringExtra("wish");
                String theme = r.getData().getStringExtra("theme");
                markRedPacketOpened(id);
                // 金额入账
                Prefs.setBalance(Prefs.getBalance() + parseDouble(amount));
                Toast.makeText(this, "已存入零钱 ¥" + amount, Toast.LENGTH_SHORT).show();
                // 让 AI 知道红包信息
                pendingContext.append("\n（对方刚才领取/查看了一个红包：金额 ")
                        .append(amount).append(" 元，主题「").append(wish)
                        .append("」，类型「").append(theme).append("」）");
                if (isAi) {
                    appendLocalMessage(Message.FROM_PEER, "谢谢你的红包，金额 " + amount
                            + " 元，主题「" + wish + "」，我很喜欢这句话～");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        convId = getIntent().getStringExtra(EXTRA_CONV_ID);
        if (convId == null) convId = "ai";

        // 会话资料从数据库读取：名称、人设、温度、模型都跟着这个 AI 走
        conv = App.dbHelper().getConversation(convId);
        name = conv.name == null || conv.name.isEmpty() ? "AI 助手" : conv.name;
        isAi = conv.isAi;

        ((TextView) findViewById(R.id.title)).setText(name);
        subtitle = findViewById(R.id.subtitle);
        recycler = findViewById(R.id.recycler);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);
        panel = findViewById(R.id.panel);
        emojiPanel = findViewById(R.id.emoji_panel);
        plusPanel = findViewById(R.id.plus_panel);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        adapter = new MessageAdapter(data);
        recycler.setAdapter(adapter);
        adapter.setAction(new MessageAdapter.OnMessageAction() {
            @Override
            public void onRedPacketClick(Message m) {
                openRedPacket(m);
            }

            @Override
            public void onTransferClick(Message m) {
                onTransferClickMessage(m);
            }

            @Override
            public void onLongClick(Message m, int position) {
                showMessageMenu(m, position);
            }

            @Override
            public void onImageClick(Message m) {
                Toast.makeText(ChatActivity.this, "图片：" + m.extra, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.iv_more).setOnClickListener(v -> showChatMenu());
        findViewById(R.id.iv_plus).setOnClickListener(v -> togglePlusPanel());
        findViewById(R.id.iv_emoji).setOnClickListener(v -> toggleEmojiPanel());
        findViewById(R.id.iv_voice).setOnClickListener(v -> sendVoice());
        btnSend.setOnClickListener(v -> sendText());

        etInput.setOnClickListener(v -> hidePanel());
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean has = s != null && s.length() > 0;
                btnSend.setVisibility(has ? View.VISIBLE : View.GONE);
                findViewById(R.id.iv_plus).setVisibility(has ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (!TextUtils.isEmpty(etInput.getText().toString().trim())) sendText();
            return true;
        });

        // 把这个 AI 的头像传给消息适配器（我的头像读本地资料）
        adapter.setPeerAvatar(conv.avatarPath, conv.avatarIcon, conv.firstChar(), conv.avatarColor);

        setupEmojiPanel();
        setupPlusPanel();
        loadHistory();
        // 恢复该会话的回复计数，保证表情包频率跨页面连续
        aiReplyCount = com.wechat.ai.util.Prefs.getEmojiCounter(convId);
        scrollToBottom();
    }

    // ---------------- 数据 ----------------

    private void loadHistory() {
        data.clear();
        data.addAll(App.dbHelper().getMessages(convId));
        adapter.notifyDataSetChanged();
    }

    private void append(Message m) {
        m.id = App.dbHelper().addMessage(m);
        data.add(m);
        adapter.notifyItemInserted(data.size() - 1);
        scrollToBottom();
    }

    private void appendLocalMessage(int from, String content) {
        Message m = Message.text(convId, from, content);
        append(m);
    }

    private void updateMessage(Message m) {
        App.dbHelper().updateMessage(m);
        int idx = data.indexOf(m);
        if (idx >= 0) adapter.notifyItemChanged(idx);
    }

    private void scrollToBottom() {
        recycler.post(() -> {
            if (data.size() > 0) recycler.scrollToPosition(data.size() - 1);
        });
    }

    // ---------------- 发送 ----------------

    private void sendText() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        btnSend.setVisibility(View.GONE);
        findViewById(R.id.iv_plus).setVisibility(View.VISIBLE);
        Message m = Message.text(convId, Message.FROM_ME, text);
        append(m);
        if (isAi) {
            // 本地先做一次语气/情绪检测，作为上下文提示
            lastUserEmotion = com.wechat.ai.emoji.EmotionLexicon.detectEmotion(text);
            // AI 相册：对方这句话聊到什么话题，就去相册里找相近的照片
            maybeSendPhoto(text);
            if (!lastUserEmotion.isEmpty()) {
                pendingContext.append("\n（检测到用户这句话的情绪是「")
                        .append(com.wechat.ai.emoji.EmotionLexicon.cnName(lastUserEmotion))
                        .append("」，请用对应的语气回应）");
            }
            requestAiReply();
        } else {
            friendAutoReply(text);
        }
    }

    private void sendImage(String path) {
        append(Message.image(convId, Message.FROM_ME, path));
        if (isAi) {
            pendingContext.append("\n（用户给你发了一张图片，请用自然的语气回应）");
            requestAiReply();
        } else {
            friendAutoReply("");
        }
    }

    private void sendEmoji(EmojiItem e) {
        append(Message.emoji(convId, Message.FROM_ME, e.path));
    }

    private void sendVoice() {
        int sec = 2 + random.nextInt(8);
        append(Message.voice(convId, Message.FROM_ME, sec));
        if (isAi) {
            pendingContext.append("\n（用户发来一条 ").append(sec).append(" 秒的语音）");
            requestAiReply();
        } else {
            friendAutoReply("");
        }
    }

    private void sendRedPacketMessage(String amount, String wish, String theme) {
        try {
            JSONObject o = new JSONObject();
            o.put("amount", amount);
            o.put("wish", wish);
            o.put("theme", theme);
            o.put("opened", 0);
            o.put("from", Prefs.getNickname());
            Message m = new Message();
            m.convId = convId;
            m.from = Message.FROM_ME;
            m.type = Message.TYPE_REDPACKET;
            m.content = "[微信红包]";
            m.extra = o.toString();
            append(m);

            // 扣款
            Prefs.setBalance(Prefs.getBalance() - parseDouble(amount));

            // AI 读取红包金额 / 主题 / 标语
            pendingContext.append("\n（用户给你发了一个红包：金额 ").append(amount)
                    .append(" 元，主题/标语「").append(wish).append("」，类型「").append(theme)
                    .append("」。请用自然的语气感谢，并可以提到金额和这句祝福语）");

            if (isAi) {
                // 对方稍后领取
                main.postDelayed(() -> {
                    markRedPacketOpened(m.id);
                    requestAiReply();
                }, 1200);
            } else {
                main.postDelayed(() -> {
                    markRedPacketOpened(m.id);
                    appendLocalMessage(Message.FROM_PEER, "谢谢老板的红包！");
                }, 1500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTransferMessage(String amount, String note) {
        try {
            JSONObject o = new JSONObject();
            o.put("amount", amount);
            o.put("note", note);
            o.put("status", "wait");
            Message m = new Message();
            m.convId = convId;
            m.from = Message.FROM_ME;
            m.type = Message.TYPE_TRANSFER;
            m.content = "[转账]";
            m.extra = o.toString();
            append(m);
            Prefs.setBalance(Prefs.getBalance() - parseDouble(amount));

            pendingContext.append("\n（用户给你转账 ").append(amount).append(" 元，备注「")
                    .append(note).append("」）");

            if (isAi) {
                main.postDelayed(() -> {
                    acceptTransfer(m, false);
                    requestAiReply();
                }, 1200);
            } else {
                main.postDelayed(() -> {
                    acceptTransfer(m, false);
                    appendLocalMessage(Message.FROM_PEER, "收到啦，谢谢！");
                }, 1500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------- 红包 / 转账交互 ----------------

    private void openRedPacket(Message m) {
        try {
            JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
            Intent i = new Intent(this, RedPacketOpenActivity.class);
            i.putExtra("msg_id", m.id);
            i.putExtra("amount", o.optString("amount", "0.00"));
            i.putExtra("wish", o.optString("wish", "恭喜发财，大吉大利"));
            i.putExtra("theme", o.optString("theme", "普通红包"));
            i.putExtra("from", o.optString("from", name));
            openRedPacket.launch(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markRedPacketOpened(long id) {
        for (Message m : data) {
            if (m.id == id) {
                try {
                    JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
                    o.put("opened", 1);
                    m.extra = o.toString();
                    updateMessage(m);
                } catch (Exception ignore) {
                }
                break;
            }
        }
    }

    private void onTransferClickMessage(Message m) {
        try {
            JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
            String status = o.optString("status", "");
            if ("received".equals(status)) {
                Toast.makeText(this, "该转账已完成", Toast.LENGTH_SHORT).show();
                return;
            }
            if (m.from == Message.FROM_ME) {
                Toast.makeText(this, "等待对方确认收款", Toast.LENGTH_SHORT).show();
                return;
            }
            acceptTransfer(m, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acceptTransfer(Message m, boolean byMe) {
        try {
            JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
            String amount = o.optString("amount", "0");
            o.put("status", "received");
            m.extra = o.toString();
            updateMessage(m);

            String who = m.from == Message.FROM_ME ? name : Prefs.getNickname();
            if (byMe) {
                // 我收下对方转来的钱
                Prefs.setBalance(Prefs.getBalance() + parseDouble(amount));
                showTransferNotify("你已收款", "¥" + amount + " 已存入零钱");
            } else {
                // 对方（AI）收下我转的钱：弹通知 + 发一条回执消息
                showTransferNotify("「" + who + "」已接收", "转账 ¥" + amount + " 已被接收");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 顶部滑入一条微信风格的收款/接收通知，2.8 秒后自动滑出 */
    private void showTransferNotify(String title, String sub) {
        View bar = findViewById(R.id.notify_bar);
        if (bar == null) return;
        TextView tvTitle = findViewById(R.id.notify_title);
        TextView tvSub = findViewById(R.id.notify_sub);
        tvTitle.setText(title);
        tvSub.setText(sub);

        bar.clearAnimation();
        bar.setVisibility(View.VISIBLE);
        bar.setAlpha(0f);
        bar.setTranslationY(-24f);
        bar.animate().alpha(1f).translationY(0f).setDuration(260).start();

        main.removeCallbacks(hideNotify);
        main.postDelayed(hideNotify, 2800);
    }

    private final Runnable hideNotify = new Runnable() {
        @Override
        public void run() {
            View bar = findViewById(R.id.notify_bar);
            if (bar == null || bar.getVisibility() != View.VISIBLE) return;
            bar.animate().alpha(0f).translationY(-24f).setDuration(240)
                    .withEndAction(() -> bar.setVisibility(View.GONE)).start();
        }
    };

    // ---------------- AI 回复 ----------------

    private void requestAiReply() {
        if (Prefs.getApiKey().trim().isEmpty()) {
            appendLocalMessage(Message.FROM_PEER, "我还没连上大脑呢～去「我 - 设置 - 模型设置」里填个 API Key 吧。");
            return;
        }
        List<ChatMsg> msgs = buildContext();
        subtitle.setVisibility(View.VISIBLE);
        subtitle.setText("对方正在输入…");

        streaming = Message.text(convId, Message.FROM_PEER, "");
        streaming.id = App.dbHelper().addMessage(streaming);
        data.add(streaming);
        adapter.notifyItemInserted(data.size() - 1);
        scrollToBottom();

        Float temp = (conv != null && conv.temperature >= 0f) ? conv.temperature : null;
        String model = (conv != null && conv.model != null && !conv.model.trim().isEmpty())
                ? conv.model.trim() : null;
        AiEngine.chat(msgs, temp, model, new AiEngine.Callback() {
            @Override
            public void onDelta(String text) {
                runOnUiThread(() -> {
                    if (streaming == null) return;
                    String cur = streaming.content == null ? "" : streaming.content;
                    String add = text == null ? "" : text;
                    streaming.content = AiEngine.stripEmotion(cur + add);
                    int idx = data.indexOf(streaming);
                    if (idx >= 0) adapter.notifyItemChanged(idx);
                    scrollToBottom();
                });
            }

            @Override
            public void onDone(String full, String emotion) {
                runOnUiThread(() -> {
                    subtitle.setVisibility(View.GONE);
                    if (streaming == null) return;
                    streaming.content = full == null ? "" : full;
                    if (streaming.content.trim().isEmpty()) {
                        streaming.content = "……";
                    }
                    // 兜底：万一内容里还有脏的 "null" 前缀，清掉再显示
                    if (streaming.content != null) {
                        while (streaming.content.startsWith("null")) {
                            streaming.content = streaming.content.substring(4);
                        }
                    }
                    updateMessage(streaming);
                    if (emotion != null && !emotion.isEmpty()) lastAiEmotion = emotion;
                    aiReplyCount++;
                    // 按会话持久化计数：退出页面/重启后频率依然准确
                    com.wechat.ai.util.Prefs.setEmojiCounter(convId, aiReplyCount);
                    Message done = streaming;
                    streaming = null;
                    // 情绪识别后自动选表情
                    maybeSendEmoji(emotion);
                    scrollToBottom();
                    // 手机系统通知：只有用户不在这个聊天页时才弹，避免自聊自扰
                    if (!convId.equals(com.wechat.ai.App.getForegroundConv())) {
                        com.wechat.ai.util.NotifyHelper.show(
                                ChatActivity.this, convId, name, done.content);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    subtitle.setVisibility(View.GONE);
                    if (streaming == null) return;
                    streaming.content = "（请求失败）" + message;
                    updateMessage(streaming);
                    streaming = null;
                });
            }
        });
    }

    /** 按图片路径找到用户给这个表情包打的标签；找不到就给个中性描述 */
    private String emojiTagsOf(String path) {
        if (path == null || path.isEmpty()) return "某个表情";
        try {
            for (com.wechat.ai.model.EmojiItem e : com.wechat.ai.App.dbHelper().allEmojis()) {
                if (path.equals(e.path)) {
                    String t = e.tags;
                    if (t != null && !t.trim().isEmpty()) return t.trim();
                    return "某个表情";
                }
            }
        } catch (Throwable ignored) {
        }
        return "某个表情";
    }

    /** 最近几条朋友圈的简要文本，供 AI 了解对方动态 */
    private String recentMomentsBrief() {
        try {
            java.util.List<com.wechat.ai.model.Moment> all =
                    com.wechat.ai.App.dbHelper().allMoments();
            if (all == null || all.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (com.wechat.ai.model.Moment m : all) {
                if (n >= 5) break;
                String text = m.text == null ? "" : m.text.trim();
                if (text.isEmpty()) continue;
                sb.append("\n- ").append(text);
                n++;
            }
            return sb.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private List<ChatMsg> buildContext() {
        List<ChatMsg> msgs = new ArrayList<>();
        String persona = (conv != null && conv.systemPrompt != null && !conv.systemPrompt.trim().isEmpty())
                ? conv.systemPrompt.trim()
                : Prefs.getSystemPrompt();
        String nick = Prefs.getNickname();
        if (nick == null || nick.trim().isEmpty()) nick = "对方";
        // 人物经历：让 AI 拥有"真实人生"的记忆，说话才不像机器
        String memory = "";
        try {
            memory = com.wechat.ai.App.dbHelper().buildMemoryBrief();
        } catch (Throwable ignored) {
        }

        StringBuilder personaSb = new StringBuilder();
        personaSb.append(persona)
                .append("\n你叫「").append(name).append("」，对方叫「").append(nick)
                .append("」。别问名字、别自我介绍。");
        if (!memory.isEmpty()) {
            // 关键措辞：说成"你的经历/你记得"，而不是"用户给你的设定"。
            // AI 会把自己当成真的经历过这些事的人，而不是在扮演一个被描述的角色。
            personaSb.append("\n\n【你的人生】以下是你亲身经历过的、你本来就知道的事，"
                    + "不是别人告诉你的人设，而是你自己的记忆：")
                    .append(memory)
                    .append("\n【怎么用】像正常人一样自然地用到这些：聊到相关话题会想起来、"
                            + "会拿自己的经历举例子、有自己的习惯和偏好。"
                            + "但不要主动罗列、不要背诵、不要把上面这段话复述出来，"
                            + "更不要说「我记得」「根据我的经历」这类话。就像人不会刻意提起自己的过去一样。");
        }
        personaSb.append("\n像微信聊天：一次一到三句，短句口语，不用Markdown和列表。");
        msgs.add(ChatMsg.system(personaSb.toString()));
        if (pendingContext.length() > 0) {
            msgs.add(ChatMsg.system("以下是本次对话中发生的行为，请据此自然地回应：\n" + pendingContext.toString()));
            pendingContext.setLength(0);
        }
        // 朋友圈：让 AI 知道对方发了什么，但不要求它逐条回复
        String moments = recentMomentsBrief();
        if (!moments.isEmpty()) {
            msgs.add(ChatMsg.system(
                    "【对方的朋友圈】以下是 TA 最近发的朋友圈，你刷到过、知道这些事：\n"
                            + moments
                            + "\n【怎么用】这是朋友圈动态，不是对方在问你问题，"
                            + "不要逐条回复、不要复述、不要说「我看到你发了朋友圈」。"
                            + "但你可以自然提起：聊到相关话题时随口带一句，"
                            + "就像朋友之间会说「你那天发的那个…」一样。"));
        }
        List<Message> history = App.dbHelper().getMessages(convId);
        // 只带最近 12 条历史：微信聊天语境足够，且大幅节省 token
        int start = Math.max(0, history.size() - HISTORY_MAX);
        for (int i = start; i < history.size(); i++) {
            Message m = history.get(i);
            if (m.type == Message.TYPE_TEXT) {
                // 空内容（DB NULL / 空串）绝不能进上下文，否则会变成 "null" 被 AI 学去
                String body = com.wechat.ai.db.DbHelper.nz(m.content);
                if (body.trim().isEmpty()) continue;
                if (m.from == Message.FROM_ME) msgs.add(ChatMsg.user(body));
                else if (m.from == Message.FROM_PEER) msgs.add(ChatMsg.assistant(body));
            } else if (m.type == Message.TYPE_EMOJI) {
                // 表情包：把它的标签告诉 AI，并说明这是表情包、不是文字，不用回复
                String tags = emojiTagsOf(m.extra);
                if (m.from == Message.FROM_ME) {
                    msgs.add(ChatMsg.user("[对方发了一个表情包，表达的意思大概是："
                            + tags + "。这是表情包图片，不是要你回应的问题，顺着这个情绪接着聊就好。]"));
                } else {
                    msgs.add(ChatMsg.assistant("[我发了一个表情包，意思大概是：" + tags + "。]"));
                }
            } else if (m.type == Message.TYPE_IMAGE) {
                msgs.add(ChatMsg.user(m.from == Message.FROM_ME ? "[对方发了一张图片]" : "[我发了一张图片]"));
            } else if (m.type == Message.TYPE_VOICE) {
                msgs.add(ChatMsg.user("[一条语音消息]"));
            } else if (m.type == Message.TYPE_REDPACKET) {
                try {
                    JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
                    String line = "[红包] 金额 " + o.optString("amount") + " 元，主题/标语："
                            + o.optString("wish") + "，类型：" + o.optString("theme");
                    msgs.add(ChatMsg.user(line));
                } catch (Exception ignore) {
                }
            } else if (m.type == Message.TYPE_TRANSFER) {
                try {
                    JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
                    msgs.add(ChatMsg.user("[转账] " + o.optString("amount") + " 元，备注："
                            + o.optString("note")));
                } catch (Exception ignore) {
                }
            }
        }
        return msgs;
    }

    /**
     * AI 相册：根据用户这句话聊到的话题，从相册里挑一张照片发出。
     * 触发源是"聊到什么"（区别于表情包的"情绪"），标签同样不必全命中。
     */
    private void maybeSendPhoto(String userText) {
        if (userText == null || userText.trim().isEmpty()) return;
        if (!com.wechat.ai.util.Prefs.isPhotoAlbumEnabled()) return;
        if (!com.wechat.ai.emoji.PhotoMatcher.hasAny(App.dbHelper())) return;
        if (userText.equals(lastPhotoQuery)) return;
        lastPhotoQuery = userText;

        EmojiItem photo = com.wechat.ai.emoji.PhotoMatcher.pick(App.dbHelper(), userText);
        if (photo == null) return;
        final EmojiItem picked = photo;
        main.postDelayed(() -> {
            Message m = Message.emoji(convId, Message.FROM_PEER, picked.path);
            append(m);
        }, 450);
    }

    /** 根据 AI 判定的情绪，从表情库挑一张发出（标签不必全命中） */
    private void maybeSendEmoji(String emotion) {
        if (!Prefs.isEmotionEnabled()) return;
        // ① 先判频率，再找图。之前顺序反了：情绪没识别出来就直接 return，
        //    导致「明明到了该发的条数却一条都没发」。
        int freq = Prefs.getEmotionFreq();
        if (freq < 1) freq = 1;
        if (freq > 1 && aiReplyCount % freq != 0) return;

        // ② 情绪来源三级兜底：AI 标签 → 本地词典 → 上一次识别到的情绪
        if (emotion == null || emotion.isEmpty()) emotion = lastUserEmotion;
        if (emotion.isEmpty()) emotion = lastAiEmotion;

        EmojiItem best = null;
        if (!emotion.isEmpty()) {
            best = EmojiMatcher.pick(App.dbHelper(), emotion);
        }
        // ③ 最后兜底：实在没匹配上就从库里挑一张，保证「到点必发」
        if (best == null) {
            best = EmojiMatcher.pickAny(App.dbHelper());
            if (best != null) emotion = "";
        }
        if (best == null) return;
        // lambda 里只能用 effectively final 的变量，所以拷一份
        final EmojiItem picked = best;
        main.postDelayed(() -> {
            Message m = Message.emoji(convId, Message.FROM_PEER, picked.path);
            append(m);
        }, 350);
    }

    // ---------------- 好友模拟回复 ----------------

    private void friendAutoReply(String text) {
        subtitle.setVisibility(View.VISIBLE);
        subtitle.setText("对方正在输入…");
        main.postDelayed(() -> {
            subtitle.setVisibility(View.GONE);
            String reply = "好的";
            if (text.contains("在吗")) reply = "在的，怎么啦？";
            else if (text.contains("?" ) || text.contains("？")) reply = "我看看哈～";
            else if (text.contains("吃饭")) reply = "刚吃完，你呢？";
            else if (text.contains("红包")) reply = "哇，谢谢老板！";
            else if (!text.isEmpty()) reply = "收到，我这边没问题。";
            appendLocalMessage(Message.FROM_PEER, reply);
        }, 900);
    }

    // ---------------- 面板 ----------------

    private void setupEmojiPanel() {
        emojiPanel.setLayoutManager(new GridLayoutManager(this, 4));
        emojiList.clear();
        emojiList.addAll(App.dbHelper().allEmojis());
        emojiPanel.setAdapter(new RecyclerView.Adapter<EmojiHolder>() {
            @NonNull
            @Override
            public EmojiHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_emoji, parent, false);
                return new EmojiHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull EmojiHolder h, int position) {
                EmojiItem e = emojiList.get(position);
                Glide.with(h.img.getContext()).load(e.path).into(h.img);
                h.tags.setText(e.tags);
                h.itemView.setOnClickListener(v -> sendEmoji(e));
                h.itemView.setOnLongClickListener(v -> {
                    startActivity(new Intent(ChatActivity.this, EmojiManageActivity.class));
                    return true;
                });
            }

            @Override
            public int getItemCount() {
                return emojiList.size();
            }
        });
    }

    private void setupPlusPanel() {
        int[] icons = {
                R.drawable.ic_panel_image, R.drawable.ic_panel_camera, R.drawable.ic_panel_redpacket,
                R.drawable.ic_panel_transfer, R.drawable.ic_panel_voice_input, R.drawable.ic_panel_location,
                R.drawable.ic_panel_favorite, R.drawable.ic_panel_file, R.drawable.ic_panel_card
        };
        String[] labels = {"照片", "拍摄", "红包", "转账", "语音输入", "位置", "收藏", "文件", "个人名片"};
        // 微信风格：每个功能一个主色，白色圆角方块上放彩色图标
        final int[] tints = {
                0xFF4CAF50, 0xFF2196F3, 0xFFFF9800, 0xFFFF5722,
                0xFF9C27B0, 0xFF00BCD4, 0xFFFFC107, 0xFF607D8B, 0xFF3F51B5
        };
        plusPanel.setLayoutManager(new GridLayoutManager(this, 4));
        plusPanel.setAdapter(new RecyclerView.Adapter<PlusHolder>() {
            @NonNull
            @Override
            public PlusHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_panel_action, parent, false);
                return new PlusHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull PlusHolder h, int position) {
                h.icon.setImageResource(icons[position]);
                h.icon.setColorFilter(tints[position]);
                h.label.setText(labels[position]);
                h.itemView.setOnClickListener(v -> onPlusAction(position));
            }

            @Override
            public int getItemCount() {
                return labels.length;
            }
        });
    }

    private void onPlusAction(int position) {
        switch (position) {
            case 0:
                pickImage.launch("image/*");
                break;
            case 1:
                Toast.makeText(this, "拍摄（界面模拟）", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Intent rp = new Intent(this, RedPacketSendActivity.class);
                rp.putExtra("to", name);
                sendRedPacket.launch(rp);
                break;
            case 3:
                Intent tr = new Intent(this, TransferActivity.class);
                tr.putExtra("to", name);
                sendTransfer.launch(tr);
                break;
            default:
                Toast.makeText(this, "该功能为界面模拟", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void toggleEmojiPanel() {
        hideKeyboard();
        if (panel.getVisibility() == View.VISIBLE && emojiPanel.getVisibility() == View.VISIBLE) {
            hidePanel();
            return;
        }
        emojiList.clear();
        emojiList.addAll(App.dbHelper().allEmojis());
        if (emojiList.isEmpty()) {
            Toast.makeText(this, R.string.emoji_panel_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        emojiPanel.getAdapter().notifyDataSetChanged();
        panel.setVisibility(View.VISIBLE);
        emojiPanel.setVisibility(View.VISIBLE);
        plusPanel.setVisibility(View.GONE);
    }

    private void togglePlusPanel() {
        hideKeyboard();
        if (panel.getVisibility() == View.VISIBLE && plusPanel.getVisibility() == View.VISIBLE) {
            hidePanel();
            return;
        }
        panel.setVisibility(View.VISIBLE);
        plusPanel.setVisibility(View.VISIBLE);
        emojiPanel.setVisibility(View.GONE);
    }

    private void hidePanel() {
        panel.setVisibility(View.GONE);
        emojiPanel.setVisibility(View.GONE);
        plusPanel.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        etInput.clearFocus();
    }

    // ---------------- 菜单 ----------------

    private void showMessageMenu(Message m, int position) {
        // 注意：items 必须在 lambda 之前确定且不再赋值（lambda 引用的局部变量需 effectively final）
        final String[] items = m.type == Message.TYPE_TEXT
                ? new String[]{"复制", "删除", "撤回"}
                : new String[]{"删除", "撤回"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (items[which].equals("复制")) {
                        android.content.ClipboardManager cm =
                                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        if (cm != null) cm.setText(m.content);
                        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                    } else if (items[which].equals("删除")) {
                        App.dbHelper().deleteMessage(m.id);
                        data.remove(m);
                        adapter.notifyDataSetChanged();
                    } else {
                        App.dbHelper().deleteMessage(m.id);
                        data.remove(m);
                        adapter.notifyDataSetChanged();
                        String who = m.from == Message.FROM_ME ? "你" : name;
                        appendLocalMessage(Message.FROM_SYSTEM, who + " 撤回了一条消息");
                    }
                })
                .show();
    }

    private void showChatMenu() {
        String[] items = {"清空聊天记录", "查看对方资料", "表情包管理"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            App.dbHelper().clearConversation(convId);
                            loadHistory();
                            break;
                        case 1:
                            Toast.makeText(this, name + "：" + (isAi ? "AI 助手" : "普通好友"),
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            startActivity(new Intent(this, EmojiManageActivity.class));
                            break;
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.wechat.ai.App.setForegroundConv(convId);
        emojiList.clear();
        emojiList.addAll(App.dbHelper().allEmojis());
        if (emojiPanel.getAdapter() != null) emojiPanel.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        com.wechat.ai.App.setForegroundConv(null);
    }

    @Override
    public void onBackPressed() {
        if (panel.getVisibility() == View.VISIBLE) {
            hidePanel();
            return;
        }
        super.onBackPressed();
    }

    static class EmojiHolder extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tags;

        EmojiHolder(View v) {
            super(v);
            img = v.findViewById(R.id.emoji_img);
            tags = v.findViewById(R.id.emoji_tags);
        }
    }

    static class PlusHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;

        PlusHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            label = v.findViewById(R.id.label);
        }
    }
}
