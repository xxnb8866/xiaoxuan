package com.wechat.ai.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wechat.ai.R;
import com.wechat.ai.model.Message;
import com.wechat.ai.util.TimeUtils;
import com.wechat.ai.widget.AvatarView;

import org.json.JSONObject;

import java.util.List;

/**
 * 聊天消息适配器：文本 / 图片 / 表情 / 语音 / 红包 / 转账。
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {

    private final List<Message> data;

    /** 对方（AI）头像信息 */
    private String peerPath = "";
    private String peerIcon = "";
    private String peerText = "AI";
    private int peerColor = 0xFF07C160;

    public void setPeerAvatar(String path, String icon, String text, int color) {
        this.peerPath = path == null ? "" : path;
        this.peerIcon = icon == null ? "" : icon;
        this.peerText = text == null || text.isEmpty() ? "AI" : text;
        this.peerColor = color;
    }

    public interface OnMessageAction {
        void onRedPacketClick(Message m);

        void onTransferClick(Message m);

        void onLongClick(Message m, int position);

        void onImageClick(Message m);
    }

    private OnMessageAction action;

    public MessageAdapter(List<Message> data) {
        this.data = data;
    }

    public void setAction(OnMessageAction action) {
        this.action = action;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Message m = data.get(position);
        boolean mine = m.from == Message.FROM_ME;

        // 时间
        boolean showTime = position == 0 || TimeUtils.needTimeDivider(data.get(position - 1).ts, m.ts);
        h.time.setVisibility(showTime ? View.VISIBLE : View.GONE);
        if (showTime) h.time.setText(TimeUtils.chatTime(m.ts));

        // 头像与方向
        h.peer.setVisibility(mine ? View.GONE : View.VISIBLE);
        h.me.setVisibility(mine ? View.VISIBLE : View.GONE);
        h.row.setGravity(mine ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT);
        h.peer.bind(peerPath, com.wechat.ai.widget.AvatarIcons.resOf(peerIcon), peerText, peerColor);
        h.me.bind(com.wechat.ai.util.MyAvatar.path(),
                com.wechat.ai.util.MyAvatar.iconRes(),
                com.wechat.ai.util.MyAvatar.text(),
                com.wechat.ai.util.MyAvatar.color());

        hideAll(h);

        switch (m.type) {
            case Message.TYPE_TEXT:
                h.text.setVisibility(View.VISIBLE);
                h.text.setText(m.content);
                if (m.from == Message.FROM_SYSTEM) {
                    // 撤回提示：居中灰底小字
                    h.peer.setVisibility(View.GONE);
                    h.me.setVisibility(View.GONE);
                    h.row.setGravity(android.view.Gravity.CENTER);
                    h.text.setBackgroundResource(R.drawable.bg_time_chip);
                    h.text.setTextColor(0xFFFFFFFF);
                    h.text.setTextSize(12);
                } else {
                    h.text.setBackgroundResource(mine ? R.drawable.bg_bubble_out : R.drawable.bg_bubble_in);
                    h.text.setTextColor(0xFF181818);
                    h.text.setTextSize(16);
                }
                break;

            case Message.TYPE_IMAGE:
            case Message.TYPE_EMOJI:
                h.image.setVisibility(View.VISIBLE);
                if (m.type == Message.TYPE_EMOJI) {
                    h.image.getLayoutParams().width = (int) (110 * h.image.getResources().getDisplayMetrics().density);
                    h.image.getLayoutParams().height = (int) (110 * h.image.getResources().getDisplayMetrics().density);
                }
                Glide.with(h.image.getContext()).load(m.extra).centerCrop()
                        .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(
                                (int) (8 * h.image.getResources().getDisplayMetrics().density)))
                        .into(h.image);
                h.image.setOnClickListener(v -> {
                    if (action != null) action.onImageClick(m);
                });
                break;

            case Message.TYPE_VOICE:
                h.voice.setVisibility(View.VISIBLE);
                h.voice.setBackgroundResource(mine ? R.drawable.bg_bubble_out : R.drawable.bg_bubble_in);
                int sec = 1;
                try {
                    sec = Integer.parseInt(m.extra);
                } catch (Exception ignore) {
                }
                h.voiceSeconds.setText(sec + "″");
                break;

            case Message.TYPE_REDPACKET:
                h.redpacket.setVisibility(View.VISIBLE);
                try {
                    JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
                    String wish = o.optString("wish", "恭喜发财，大吉大利");
                    boolean opened = o.optInt("opened", 0) == 1;
                    h.rpWish.setText(wish);
                    h.rpAction.setText(opened ? "已领取" : (mine ? "等待对方领取" : "领取红包"));
                } catch (Exception ignore) {
                }
                h.redpacket.setOnClickListener(v -> {
                    if (action != null) action.onRedPacketClick(m);
                });
                break;

            case Message.TYPE_TRANSFER:
                h.transfer.setVisibility(View.VISIBLE);
                try {
                    JSONObject o = new JSONObject(m.extra == null ? "{}" : m.extra);
                    h.trAmount.setText("¥" + o.optString("amount", "0.00"));
                    String note = o.optString("note", "");
                    h.trNote.setText(note.isEmpty() ? "转账" : note);
                    boolean accepted = "received".equals(o.optString("status", ""));
                    h.trStatus.setText(mine ? (accepted ? "对方已收款" : "等待确认收款")
                            : (accepted ? "已收款" : "点击收款"));
                } catch (Exception ignore) {
                }
                h.transfer.setOnClickListener(v -> {
                    if (action != null) action.onTransferClick(m);
                });
                break;

            default:
                break;
        }

        h.itemView.setOnLongClickListener(v -> {
            if (action != null) action.onLongClick(m, h.getAdapterPosition());
            return true;
        });
    }

    private void hideAll(Holder h) {
        h.text.setVisibility(View.GONE);
        h.image.setVisibility(View.GONE);
        h.voice.setVisibility(View.GONE);
        h.redpacket.setVisibility(View.GONE);
        h.transfer.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView time;
        LinearLayout row;
        AvatarView peer;
        AvatarView me;
        TextView text;
        ImageView image;
        LinearLayout voice;
        TextView voiceSeconds;
        LinearLayout redpacket;
        TextView rpWish;
        TextView rpAction;
        TextView rpFooter;
        LinearLayout transfer;
        TextView trAmount;
        TextView trNote;
        TextView trStatus;

        Holder(View v) {
            super(v);
            time = v.findViewById(R.id.time);
            row = v.findViewById(R.id.row);
            peer = v.findViewById(R.id.avatar_peer);
            me = v.findViewById(R.id.avatar_me);
            text = v.findViewById(R.id.bubble_text);
            image = v.findViewById(R.id.msg_image);
            voice = v.findViewById(R.id.voice_layout);
            voiceSeconds = v.findViewById(R.id.voice_seconds);
            redpacket = v.findViewById(R.id.redpacket_card);
            rpWish = v.findViewById(R.id.rp_wish);
            rpAction = v.findViewById(R.id.rp_action);
            rpFooter = v.findViewById(R.id.rp_footer);
            transfer = v.findViewById(R.id.transfer_card);
            trAmount = v.findViewById(R.id.tr_amount);
            trNote = v.findViewById(R.id.tr_note);
            trStatus = v.findViewById(R.id.tr_status);
        }
    }
}
