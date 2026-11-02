package com.wechat.ai.ui.emoji;

import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.emoji.EmotionLexicon;
import com.wechat.ai.model.EmojiItem;
import com.wechat.ai.util.ImageUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 添加表情：预览图片 + 打情绪标签。
 */
public class AddEmojiActivity extends AppCompatActivity {

    private String path;
    private final Set<String> selected = new HashSet<>();
    private final List<String> chips = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_emoji);

        String uriStr = getIntent().getStringExtra("uri");
        ImageView preview = findViewById(R.id.preview);
        EditText etCustom = findViewById(R.id.et_custom);
        RecyclerView tags = findViewById(R.id.tags);

        if (uriStr == null) {
            Toast.makeText(this, "没有选择图片", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Uri uri = Uri.parse(uriStr);
        path = ImageUtils.copyToPrivate(this, uri);
        Glide.with(preview).load(path).into(preview);

        for (String t : EmotionLexicon.TAGS) {
            chips.add(EmotionLexicon.cnName(t));
        }
        // 补充常用口语标签
        chips.add("无语");
        chips.add("摸鱼");
        chips.add("加油");
        chips.add("干饭");

        tags.setLayoutManager(new GridLayoutManager(this, 4));
        tags.setNestedScrollingEnabled(false);
        tags.setAdapter(new RecyclerView.Adapter<ChipHolder>() {
            @Override
            public ChipHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                android.view.View v = getLayoutInflater()
                        .inflate(R.layout.item_tag_chip, parent, false);
                return new ChipHolder(v);
            }

            @Override
            public void onBindViewHolder(ChipHolder h, int position) {
                String tag = chips.get(position);
                h.chip.setText(tag);
                h.chip.setBackgroundResource(selected.contains(tag)
                        ? R.drawable.bg_tag_chip_selected : R.drawable.bg_tag_chip);
                h.chip.setOnClickListener(v -> {
                    if (selected.contains(tag)) selected.remove(tag);
                    else selected.add(tag);
                    notifyItemChanged(position);
                });
            }

            @Override
            public int getItemCount() {
                return chips.size();
            }
        });

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            View sv = findViewById(R.id.btn_save);
            if (sv instanceof com.wechat.ai.widget.ConfirmButton) {
                ((com.wechat.ai.widget.ConfirmButton) sv)
                        .setOnConfirmedListener(() -> save(etCustom));
                ((com.wechat.ai.widget.ConfirmButton) sv).confirm();
            } else {
                save(etCustom);
            }
        });
        findViewById(R.id.btn_ok).setOnClickListener(v -> save(etCustom));
    }

    private void save(EditText etCustom) {
        StringBuilder sb = new StringBuilder();
        for (String s : selected) {
            if (sb.length() > 0) sb.append(",");
            sb.append(s);
        }
        String custom = etCustom.getText().toString().trim();
        if (!custom.isEmpty()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(custom.replace("，", ","));
        }
        if (sb.length() == 0) {
            Toast.makeText(this, "至少选一个标签或填一个自定义标签", Toast.LENGTH_SHORT).show();
            return;
        }
        EmojiItem e = new EmojiItem(path, sb.toString());
        App.dbHelper().addEmoji(e);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK, new Intent());
        finish();
    }

    static class ChipHolder extends RecyclerView.ViewHolder {
        TextView chip;

        ChipHolder(android.view.View v) {
            super(v);
            chip = v.findViewById(R.id.chip);
        }
    }
}
