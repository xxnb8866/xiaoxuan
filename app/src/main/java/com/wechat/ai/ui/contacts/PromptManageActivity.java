package com.wechat.ai.ui.contacts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.PromptTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词管理：用户自定义名称保存人设，可在创建 AI 时一键套用。
 * <p>
 * 普通模式：管理（新增 / 编辑 / 删除 / 复制）。
 * 选择模式（EXTRA_PICK_MODE=true）：点一条即把内容回传给上一个页面。
 */
public class PromptManageActivity extends AppCompatActivity {

    public static final String EXTRA_PICK_MODE = "pick_mode";

    private RecyclerView recycler;
    private View empty;
    private TextView btnAdd;
    private final List<PromptTemplate> data = new ArrayList<>();
    private Adapter adapter;
    private boolean pickMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt_manage);

        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        recycler = findViewById(R.id.recycler);
        empty = findViewById(R.id.empty);
        btnAdd = findViewById(R.id.btn_add);
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(pickMode ? "选择人设模板" : "提示词管理");

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showEditDialog(null));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        refresh();
    }

    private void refresh() {
        data.clear();
        data.addAll(App.dbHelper().allPrompts());
        adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 新增 / 编辑模板 */
    private void showEditDialog(PromptTemplate existing) {
        boolean editing = existing != null;
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_prompt_edit, null);
        EditText etName = v.findViewById(R.id.et_name);
        EditText etContent = v.findViewById(R.id.et_content);
        if (editing) {
            etName.setText(existing.name);
            etContent.setText(existing.content);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "编辑提示词" : "新建提示词")
                .setView(v)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(vv -> {
                    String name = etName.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "请填写名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(content)) {
                        Toast.makeText(this, "请填写提示词内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PromptTemplate p = editing ? existing : new PromptTemplate();
                    p.name = name;
                    p.content = content;
                    if (editing) {
                        App.dbHelper().updatePrompt(p);
                    } else {
                        App.dbHelper().addPrompt(p);
                    }
                    dialog.dismiss();
                    refresh();
                    Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
                }));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.wx_green));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.wx_text_secondary));
    }

    private void confirmDelete(PromptTemplate p) {
        new AlertDialog.Builder(this)
                .setTitle("删除「" + p.name + "」？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    App.dbHelper().deletePrompt(p.id);
                    refresh();
                })
                .show();
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_prompt, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            PromptTemplate p = data.get(position);
            h.name.setText(p.name);
            h.content.setText(p.content);

            h.itemView.setOnClickListener(v -> {
                if (pickMode) {
                    Intent i = new Intent();
                    i.putExtra(AiEditActivity.EXTRA_PROMPT_CONTENT, p.content);
                    i.putExtra(AiEditActivity.EXTRA_PROMPT_NAME, p.name);
                    setResult(Activity.RESULT_OK, i);
                    finish();
                    return;
                }
                String[] items = {"编辑", "删除", "复制内容"};
                new AlertDialog.Builder(PromptManageActivity.this)
                        .setTitle(p.name)
                        .setItems(items, (d, which) -> {
                            if (which == 0) showEditDialog(p);
                            else if (which == 1) confirmDelete(p);
                            else {
                                android.content.ClipboardManager cm =
                                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                if (cm != null) cm.setText(p.content);
                                Toast.makeText(PromptManageActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView name;
            TextView content;

            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.name);
                content = v.findViewById(R.id.content);
            }
        }
    }
}
