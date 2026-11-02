package com.wechat.ai.ui.emoji;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.emoji.EmotionLexicon;
import com.wechat.ai.model.EmojiItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情包管理：从相册添加、打标签、删除。
 */
public class EmojiManageActivity extends AppCompatActivity {

    private final List<EmojiItem> data = new ArrayList<>();
    private RecyclerView recycler;
    private TextView empty;
    private Adapter adapter;

    // 注意：addEmoji 必须先声明，pick 的回调里才会用到它（Java 不允许前向引用）
    private final ActivityResultLauncher<Intent> addEmoji =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                refresh();
            });

    private final ActivityResultLauncher<String> pick =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                Intent i = new Intent(this, AddEmojiActivity.class);
                i.putExtra("uri", uri.toString());
                addEmoji.launch(i);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emoji_manage);

        recycler = findViewById(R.id.recycler);
        empty = findViewById(R.id.empty);
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add).setOnClickListener(v -> pick.launch("image/*"));
        findViewById(R.id.btn_help).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("表情包怎么用")
                .setMessage("1. 点「添加表情」从相册选图\n"
                        + "2. 给它打上标签，比如：开心、伤心、无语\n"
                        + "3. 聊天时 AI 会识别你的情绪，自动挑最匹配的一张发出去\n\n"
                        + "标签不必全命中：比如你打「开心」，AI 判定为「欢乐 / 高兴 / 兴奋」时也会命中这张。")
                .setPositiveButton("知道了", null)
                .show());

        refresh();
    }

    private void refresh() {
        data.clear();
        data.addAll(App.dbHelper().allEmojis());
        adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void editTags(EmojiItem e) {
        EditText input = new EditText(this);
        input.setText(e.tags);
        input.setHint("标签，用逗号分隔，如：开心,欢乐");
        input.setSingleLine(true);
        new AlertDialog.Builder(this)
                .setTitle("编辑标签")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String tags = input.getText().toString().trim();
                    if (TextUtils.isEmpty(tags)) {
                        Toast.makeText(this, "至少写一个标签", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    e.tags = tags;
                    App.dbHelper().updateEmoji(e);
                    refresh();
                })
                .show();
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_emoji_manage, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int position) {
            EmojiItem e = data.get(position);
            Glide.with(h.img.getContext()).load(e.path).into(h.img);
            h.tags.setText(e.tags + "  ·  命中 " + e.hits + " 次");
            h.itemView.setOnClickListener(v -> editTags(e));
            h.delete.setOnClickListener(v -> {
                App.dbHelper().deleteEmoji(e.id);
                refresh();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView img;
            ImageView delete;
            TextView tags;

            Holder(View v) {
                super(v);
                img = v.findViewById(R.id.img);
                delete = v.findViewById(R.id.btn_delete);
                tags = v.findViewById(R.id.tags);
            }
        }
    }
}
