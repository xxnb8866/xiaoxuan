package com.wechat.ai.ui.moments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.model.Moment;
import com.wechat.ai.util.Prefs;
import com.wechat.ai.util.TimeUtils;
import com.wechat.ai.widget.AvatarView;

import java.util.ArrayList;
import java.util.List;

/**
 * 朋友圈：发动态、点赞、评论。
 */
public class MomentsActivity extends AppCompatActivity {

    private final List<Moment> data = new ArrayList<>();
    private ImageView cover;
    private final androidx.activity.result.ActivityResultLauncher<String> pickCover =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        String path = com.wechat.ai.util.ImageUtils.copyToPrivate(this, uri);
                        if (path == null) {
                            Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Prefs.setMomentsCover(path);
                        applyCover();
                        Toast.makeText(this, "封面已更换", Toast.LENGTH_SHORT).show();
                    });

    private RecyclerView recycler;
    private TextView empty;
    private Adapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moments);

        AvatarView myAvatar = findViewById(R.id.my_avatar);
        TextView myName = findViewById(R.id.my_name);
        myAvatar.bind(com.wechat.ai.util.MyAvatar.path(),
                com.wechat.ai.util.MyAvatar.iconRes(),
                com.wechat.ai.util.MyAvatar.text(),
                com.wechat.ai.util.MyAvatar.color());
        myName.setText(Prefs.getNickname());

        cover = findViewById(R.id.cover);
        applyCover();
        cover.setOnClickListener(v -> pickCover.launch("image/*"));

        recycler = findViewById(R.id.recycler);
        empty = findViewById(R.id.empty);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setNestedScrollingEnabled(false);
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.iv_camera).setOnClickListener(v ->
                startActivity(new Intent(this, PostMomentActivity.class)));

        refresh();
    }

    /** 应用朋友圈封面：有自定义图就显示，没有用默认渐变 */
    private void applyCover() {
        if (cover == null) return;
        String path = Prefs.getMomentsCover();
        if (path != null && !path.isEmpty()) {
            try {
                com.bumptech.glide.Glide.with(this).load(path).centerCrop().into(cover);
                return;
            } catch (Throwable ignored) {
            }
        }
        cover.setImageResource(R.drawable.bg_moments_cover);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        data.clear();
        data.addAll(App.dbHelper().allMoments());
        adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        /** 按昵称找到对应的 AI 好友，用于取 TA 的真实头像 */
        private Conversation findConvByName(String name) {
            if (name == null || name.isEmpty()) return null;
            try {
                for (Conversation c : App.dbHelper().getConversations()) {
                    if (c.name != null && c.name.equals(name)) return c;
                }
            } catch (Throwable ignored) {
            }
            return null;
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_moment, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int position) {
            com.wechat.ai.widget.AnimHelper.expand(h.itemView, 50L * Math.min(position, 8));
            Moment m = data.get(position);
            String author = m.author == null || m.author.isEmpty() ? "我" : m.author;
            // 头像同步真实头像：自己发的用「我的头像」，AI 好友发的用 TA 的头像
            if (author.equals(Prefs.getNickname())) {
                h.avatar.bind(com.wechat.ai.util.MyAvatar.path(),
                        com.wechat.ai.util.MyAvatar.iconRes(),
                        com.wechat.ai.util.MyAvatar.text(),
                        com.wechat.ai.util.MyAvatar.color());
            } else {
                Conversation c = findConvByName(author);
                if (c != null) {
                    h.avatar.bind(c.avatarPath, com.wechat.ai.widget.AvatarIcons.resOf(c.avatarIcon),
                            author.substring(0, 1), c.avatarColor);
                } else {
                    h.avatar.setAvatar(author.substring(0, 1), m.avatarColor);
                }
            }
            h.name.setText(m.author);
            h.text.setText(m.text == null ? "" : m.text);
            h.text.setVisibility(TextUtils.isEmpty(m.text) ? View.GONE : View.VISIBLE);
            h.time.setText(TimeUtils.momentTime(m.ts));

            // 图片九宫格
            h.images.removeAllViews();
            List<String> imgs = m.imageList();
            if (!imgs.isEmpty()) {
                h.images.setVisibility(View.VISIBLE);
                int size = (int) (78 * getResources().getDisplayMetrics().density);
                for (int i = 0; i < imgs.size(); i++) {
                    ImageView iv = new ImageView(MomentsActivity.this);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setBackgroundResource(R.drawable.bg_image_placeholder);
                    GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                    lp.width = size;
                    lp.height = size;
                    lp.setMargins(4, 4, 4, 4);
                    Glide.with(MomentsActivity.this).load(imgs.get(i)).centerCrop()
                            .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(
                                    (int) (4 * getResources().getDisplayMetrics().density)))
                            .into(iv);
                    h.images.addView(iv, lp);
                }
            } else {
                h.images.setVisibility(View.GONE);
            }

            h.divider.setVisibility(position == data.size() - 1 ? View.GONE : View.VISIBLE);
            renderSocial(h, m);

            h.btnLike.setOnClickListener(v -> {
                m.liked = m.liked == 1 ? 0 : 1;
                if (m.liked == 1) {
                    String likes = m.likes == null ? "" : m.likes;
                    m.likes = likes.isEmpty() ? Prefs.getNickname() : likes + "," + Prefs.getNickname();
                } else {
                    if (m.likes != null) {
                        m.likes = m.likes.replace("," + Prefs.getNickname(), "")
                                .replace(Prefs.getNickname(), "").replace(",,", ",");
                        if (m.likes.startsWith(",")) m.likes = m.likes.substring(1);
                    }
                }
                App.dbHelper().updateMoment(m);
                notifyItemChanged(h.getAdapterPosition());
            });

            h.btnComment.setOnClickListener(v -> {
                EditText input = new EditText(MomentsActivity.this);
                input.setHint("说点什么…");
                new AlertDialog.Builder(MomentsActivity.this)
                        .setTitle("评论")
                        .setView(input)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("发送", (dialog, which) -> {
                            String c = input.getText().toString().trim();
                            if (c.isEmpty()) return;
                            m.addComment(new Moment.Comment(Prefs.getNickname(), c));
                            App.dbHelper().updateMoment(m);
                            notifyItemChanged(h.getAdapterPosition());
                        })
                        .show();
            });
        }

        private void renderSocial(Holder h, Moment m) {
            h.social.removeAllViews();
            boolean hasLike = m.likes != null && !m.likes.isEmpty();
            boolean hasComment = !m.getComments().isEmpty();
            if (!hasLike && !hasComment) {
                h.social.setVisibility(View.GONE);
                return;
            }
            h.social.setVisibility(View.VISIBLE);
            if (hasLike) {
                TextView tv = new TextView(MomentsActivity.this);
                tv.setTextColor(getResources().getColor(R.color.wx_link));
                tv.setTextSize(13);
                tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_heart, 0, 0, 0);
                tv.setCompoundDrawablePadding(6);
                tv.setText(m.likes);
                h.social.addView(tv);
            }
            if (hasComment) {
                if (hasLike) {
                    View line = new View(MomentsActivity.this);
                    line.setBackgroundColor(getResources().getColor(R.color.wx_divider));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    lp.topMargin = 6;
                    lp.bottomMargin = 6;
                    h.social.addView(line, lp);
                }
                for (Moment.Comment c : m.getComments()) {
                    TextView tv = new TextView(MomentsActivity.this);
                    tv.setTextSize(13);
                    tv.setTextColor(getResources().getColor(R.color.wx_text_primary));
                    tv.setText(c.who + "：" + c.content);
                    h.social.addView(tv);
                }
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            AvatarView avatar;
            TextView name;
            TextView text;
            GridLayout images;
            TextView time;
            TextView btnLike;
            TextView btnComment;
            LinearLayout social;
            View divider;

            Holder(View v) {
                super(v);
                avatar = v.findViewById(R.id.avatar);
                name = v.findViewById(R.id.name);
                text = v.findViewById(R.id.text);
                images = v.findViewById(R.id.images);
                time = v.findViewById(R.id.time);
                btnLike = v.findViewById(R.id.btn_like);
                btnComment = v.findViewById(R.id.btn_comment);
                social = v.findViewById(R.id.social);
                divider = v.findViewById(R.id.divider);
            }
        }
    }
}
