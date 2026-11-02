package com.wechat.ai.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.model.ProactiveTask;
import com.wechat.ai.proactive.ProactiveManager;

import java.util.List;

/**
 * AI 主动消息：支持创建多个任务，每个任务有自己的时间段、目标 AI 和发送条数。
 */
public class ProactiveActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private LinearLayout list;
    private TextView empty;
    private List<ProactiveTask> tasks;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proactive);

        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add).setOnClickListener(v ->
                startActivity(new Intent(this, ProactiveEditActivity.class)));

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        tasks = App.dbHelper().allTasks();
        list.removeAllViews();
        empty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);

        for (ProactiveTask t : tasks) {
            View v = getLayoutInflater().inflate(R.layout.item_proactive_task, list, false);
            TextView timeView = v.findViewById(R.id.time);
            TextView detail = v.findViewById(R.id.detail);
            Switch sw = v.findViewById(R.id.sw);

            timeView.setText(t.timeText());
            detail.setText(t.repeatText() + " · 发 " + t.sendCount + " 条 · "
                    + convNames(t.convs));
            sw.setChecked(t.enabled);
            sw.setOnCheckedChangeListener((btn, on) -> {
                t.enabled = on;
                App.dbHelper().updateTask(t);
                ProactiveManager.apply(ProactiveActivity.this);
            });

            v.setOnClickListener(x -> {
                Intent i = new Intent(this, ProactiveEditActivity.class);
                i.putExtra(EXTRA_TASK_ID, t.id);
                startActivity(i);
            });
            com.wechat.ai.widget.AnimHelper.expand(v, 45L * list.getChildCount());
            list.addView(v);
        }
    }

    private String convNames(String raw) {
        if (raw == null || raw.isEmpty()) return "未选择";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String id : raw.split("\\|")) {
            if (id.trim().isEmpty()) continue;
            Conversation c = App.dbHelper().getConversation(id.trim());
            if (n > 0) sb.append("、");
            sb.append(c != null ? c.name : "已删除");
            n++;
            if (n >= 2) break;
        }
        if (sb.length() == 0) return "未选择";
        return sb.toString();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
