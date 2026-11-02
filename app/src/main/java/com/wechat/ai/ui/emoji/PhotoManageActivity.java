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
import com.wechat.ai.model.EmojiItem;
import com.wechat.ai.util.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 相册：用户上传真实照片并打标签（风景、美食、宠物…）。
 * 聊天时对方聊到相近话题，AI 会自动把对应照片发过去。
 * 逻辑与表情包一致（标签不必全命中），但触发源是"聊到什么"，不是情绪。
 */
public class PhotoManageActivity extends AppCompatActivity {

    private final List<EmojiItem> data = new ArrayList<>();
    private RecyclerView recycler;
    private TextView empty;
    private Adapter adapter;

    // 先声明 launcher，避免 Java 前向引用
    private final ActivityResultLauncher<String> pick =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String path = ImageUtils.copyToPrivate(this, uri);
                if (path == null) {
                    Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                askTags(path);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_manage);

        recycler = findViewById(R.id.recycler);
        empty = findViewById(R.id.empty);
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add).setOnClickListener(v -> pick.launch("image/*"));

        refresh();
    }

    /** 保存前让用户输入标签 */
    private void askTags(String path) {
        EditText et = new EditText(this);
        et.setHint("风景、美食、宠物…（逗号分隔，不必精确）");
        et.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("给这张照片打个标签")
                .setView(et)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String tags = et.getText() == null ? "" : et.getText().toString().trim();
                    if (TextUtils.isEmpty(tags)) {
                        Toast.makeText(this, "至少写一个标签", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    EmojiItem it = new EmojiItem(path, tags);
                    App.dbHelper().addPhoto(it);
                    refresh();
                    Toast.makeText(this, "已保存到 AI 相册", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void refresh() {
        data.clear();
        List<EmojiItem> all = App.dbHelper().allPhotos();
        if (all != null) data.addAll(all);
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        recycler.setVisibility(data.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo_manage, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int position) {
            EmojiItem it = data.get(position);
            Glide.with(h.img.getContext()).load(it.path).centerCrop().into(h.img);
            h.tags.setText(it.tags == null ? "" : it.tags);
            h.itemView.setOnClickListener(v -> showEdit(it));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView tags;

            Holder(View v) {
                super(v);
                img = v.findViewById(R.id.img);
                tags = v.findViewById(R.id.tags);
            }
        }
    }

    private void showEdit(EmojiItem it) {
        EditText et = new EditText(this);
        et.setText(it.tags == null ? "" : it.tags);
        et.setPadding(48, 32, 48, 32);
        new AlertDialog.Builder(this)
                .setTitle("编辑标签")
                .setView(et)
                .setNeutralButton("删除", (d, w) -> {
                    App.dbHelper().deletePhoto(it.id);
                    try {
                        new File(it.path).delete();
                    } catch (Throwable ignored) {
                    }
                    refresh();
                })
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    it.tags = et.getText() == null ? "" : et.getText().toString().trim();
                    App.dbHelper().updatePhoto(it);
                    refresh();
                })
                .show();
    }
}
