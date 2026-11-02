package com.wechat.ai.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.MemoryItem;

import java.util.List;

/**
 * AI 的人生经历库：用户把真人会经历的事写进来，AI 当成自己的记忆用，
 * 从而摆脱那种"被设定出来的角色"的空洞感。
 */
public class MemoryActivity extends AppCompatActivity {

    private LinearLayout list;
    private TextView empty;
    private List<MemoryItem> items;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory);

        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add).setOnClickListener(v -> showEdit(null));

        reload();
    }

    private void reload() {
        items = App.dbHelper().allMemories();
        list.removeAllViews();
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        for (MemoryItem m : items) {
            View v = getLayoutInflater().inflate(R.layout.item_memory, list, false);
            TextView cat = v.findViewById(R.id.category);
            TextView content = v.findViewById(R.id.content);
            Switch sw = v.findViewById(R.id.sw);

            cat.setText(MemoryItem.CATEGORIES[0].equals(m.category) || m.category.isEmpty()
                    ? "经历" : m.category);
            content.setText(m.content);
            sw.setChecked(m.enabled);
            sw.setOnCheckedChangeListener((btn, on) -> {
                m.enabled = on;
                App.dbHelper().updateMemory(m.id, m.category, m.content, on);
            });

            v.setOnClickListener(x -> showEdit(m));
            v.setOnLongClickListener(x -> {
                new AlertDialog.Builder(this)
                        .setTitle("删除这条经历？")
                        .setMessage(m.content)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (d, w) -> {
                            App.dbHelper().deleteMemory(m.id);
                            reload();
                        })
                        .show();
                return true;
            });
            list.addView(v);
        }
    }

    private void showEdit(final MemoryItem edit) {
        final boolean isNew = edit == null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        final TextView catView = new TextView(this);
        catView.setText(isNew ? "日常习惯" : edit.category);
        catView.setTextSize(14);
        catView.setTextColor(getResources().getColor(R.color.wx_green));
        catView.setPadding(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));
        catView.setOnClickListener(v -> pickCategory(catView));
        box.addView(catView);

        final EditText input = new EditText(this);
        input.setBackgroundResource(R.drawable.bg_edit_round);
        input.setHint("写下真实的人会经历的事，例如：早上闹钟要响三次才起得来");
        input.setTextSize(14);
        input.setPadding(pad, pad, pad, pad);
        input.setMinLines(4);
        input.setGravity(android.view.Gravity.TOP);
        if (!isNew) input.setText(edit.content);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(isNew ? "添加经历" : "编辑经历")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String cat = catView.getText().toString();
                    if (isNew) {
                        App.dbHelper().addMemory(cat, text);
                    } else {
                        edit.category = cat;
                        edit.content = text;
                        App.dbHelper().updateMemory(edit.id, cat, text, edit.enabled);
                    }
                    reload();
                })
                .show();
    }

    private void pickCategory(final TextView target) {
        new AlertDialog.Builder(this)
                .setTitle("选择分类")
                .setItems(MemoryItem.CATEGORIES, (d, which) ->
                        target.setText(MemoryItem.CATEGORIES[which]))
                .setNegativeButton("取消", null)
                .show();
    }
}
