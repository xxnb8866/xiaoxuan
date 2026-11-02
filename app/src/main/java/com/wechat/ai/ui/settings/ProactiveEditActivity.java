package com.wechat.ai.ui.settings;

import android.app.TimePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.model.ProactiveTask;
import com.wechat.ai.proactive.ProactiveManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 新建 / 编辑一个主动消息任务。
 */
public class ProactiveEditActivity extends AppCompatActivity {

    private TextView tvStart, tvEnd, tvRepeat, convName;
    private LinearLayout rowWeek, weekBox;
    private EditText etCount, etPrompt;
    private TextView btnDelete;

    private ProactiveTask task;
    private boolean isNew = true;

    private final List<String> chosen = new ArrayList<>();
    private boolean[] checked;
    private List<Conversation> convs;
    private final boolean[] weekOn = new boolean[7];
    private final TextView[] weekViews = new TextView[7];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proactive_edit);

        tvStart = findViewById(R.id.tv_start);
        tvEnd = findViewById(R.id.tv_end);
        tvRepeat = findViewById(R.id.tv_repeat);
        convName = findViewById(R.id.conv_name);
        rowWeek = findViewById(R.id.row_week);
        weekBox = findViewById(R.id.week_box);
        etCount = findViewById(R.id.et_count);
        etPrompt = findViewById(R.id.et_prompt);
        btnDelete = findViewById(R.id.btn_delete);

        long id = getIntent().getLongExtra(ProactiveActivity.EXTRA_TASK_ID, -1);
        if (id >= 0) {
            task = App.dbHelper().getTask(id);
            isNew = (task == null);
        }
        if (task == null) task = new ProactiveTask();
        if (isNew) {
            etPrompt.setText("现在是空闲时间，你主动找我聊一句。"
                    + "像朋友随口开口那样，别汇报、别问「在吗」，一句话就好。");
        }
        ((TextView) findViewById(R.id.title)).setText(isNew ? "新建任务" : "编辑任务");

        convs = App.dbHelper().getConversations();
        checked = new boolean[convs.size()];

        etCount.setText(String.valueOf(task.sendCount));
        if (!isNew) etPrompt.setText(task.prompt);
        for (String s : task.convs.split("\\|")) {
            if (!s.trim().isEmpty()) chosen.add(s.trim());
        }
        for (int i = 0; i < convs.size(); i++) {
            if (chosen.contains(convs.get(i).id)) checked[i] = true;
        }

        renderTime();
        renderRepeat();
        renderConvName();
        buildWeekChips();
        loadWeek();

        tvStart.setOnClickListener(v -> pickTime(true));
        tvEnd.setOnClickListener(v -> pickTime(false));
        findViewById(R.id.row_start).setOnClickListener(v -> pickTime(true));
        findViewById(R.id.row_end).setOnClickListener(v -> pickTime(false));
        findViewById(R.id.row_conv).setOnClickListener(v -> pickConv());
        findViewById(R.id.row_repeat).setOnClickListener(v -> pickRepeat());
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            View sv = findViewById(R.id.btn_save);
            if (sv instanceof com.wechat.ai.widget.ConfirmButton) {
                ((com.wechat.ai.widget.ConfirmButton) sv).setOnConfirmedListener(() -> save());
                ((com.wechat.ai.widget.ConfirmButton) sv).confirm();
            } else {
                save();
            }
        });

        if (!isNew) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("删除这个任务？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除", (d, w) -> {
                        App.dbHelper().deleteTask(task.id);
                        ProactiveManager.apply(this);
                        finish();
                    })
                    .show());
        }
    }

    private void renderTime() {
        tvStart.setText(ProactiveTask.fmt(task.startMin));
        tvEnd.setText(ProactiveTask.fmt(task.endMin));
    }

    private void pickTime(boolean isStart) {
        int cur = isStart ? task.startMin : task.endMin;
        new TimePickerDialog(this, (view, h, m) -> {
            if (isStart) task.startMin = h * 60 + m;
            else task.endMin = h * 60 + m;
            renderTime();
        }, cur / 60, cur % 60, true).show();
    }

    private void pickConv() {
        if (convs.isEmpty()) {
            Toast.makeText(this, "还没有 AI 好友，先去通讯录添加一个", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[convs.size()];
        for (int i = 0; i < convs.size(); i++) names[i] = convs.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("发给哪些 AI（可多选）")
                .setMultiChoiceItems(names, checked, (d, which, on) -> checked[which] = on)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    chosen.clear();
                    for (int i = 0; i < convs.size(); i++) {
                        if (checked[i]) chosen.add(convs.get(i).id);
                    }
                    renderConvName();
                })
                .show();
    }

    private void renderConvName() {
        if (chosen.isEmpty()) {
            convName.setText("未选择");
            convName.setTextColor(getResources().getColor(R.color.wx_text_hint));
            return;
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Conversation c : convs) {
            if (chosen.contains(c.id)) {
                if (n > 0) sb.append("、");
                sb.append(c.name);
                n++;
                if (n >= 2 && chosen.size() > 2) {
                    sb.append(" 等").append(chosen.size()).append("个");
                    break;
                }
            }
        }
        convName.setText(sb.toString());
        convName.setTextColor(getResources().getColor(R.color.wx_text_primary));
    }

    private void pickRepeat() {
        String[] items = {"仅一次", "每天", "每周指定几天"};
        new AlertDialog.Builder(this)
                .setTitle("重复方式")
                .setSingleChoiceItems(items, task.repeatMode, (d, w) -> {
                    task.repeatMode = w;
                    renderRepeat();
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void renderRepeat() {
        tvRepeat.setText(task.repeatMode == ProactiveTask.REPEAT_ONCE ? "仅一次"
                : task.repeatMode == ProactiveTask.REPEAT_DAILY ? "每天" : "每周");
        rowWeek.setVisibility(task.repeatMode == ProactiveTask.REPEAT_WEEKLY
                ? View.VISIBLE : View.GONE);
    }

    private void buildWeekChips() {
        weekBox.removeAllViews();
        int size = (int) (34 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView t = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.weight = 1;
            if (i > 0) lp.leftMargin = (int) (6 * getResources().getDisplayMetrics().density);
            t.setLayoutParams(lp);
            t.setGravity(Gravity.CENTER);
            t.setText(ProactiveTask.WEEK_CN[i]);
            t.setTextSize(13);
            t.setIncludeFontPadding(false);
            t.setOnClickListener(v -> {
                weekOn[idx] = !weekOn[idx];
                paintWeek(idx);
            });
            weekViews[i] = t;
            weekBox.addView(t);
            paintWeek(i);
        }
    }

    private void paintWeek(int i) {
        TextView t = weekViews[i];
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(17 * getResources().getDisplayMetrics().density);
        if (weekOn[i]) {
            gd.setColor(getResources().getColor(R.color.wx_green));
            t.setTextColor(getResources().getColor(R.color.wx_text_white));
        } else {
            gd.setColor(getResources().getColor(R.color.wx_bg_gray));
            t.setTextColor(getResources().getColor(R.color.wx_text_secondary));
        }
        t.setBackground(gd);
    }

    private void loadWeek() {
        boolean any = false;
        for (int i = 0; i < 7; i++) {
            if (task.isWeekdayOn(ProactiveTask.WEEK_DAYS[i])) {
                weekOn[i] = true;
                any = true;
            }
        }
        if (!any) weekOn[1] = true;
        for (int i = 0; i < 7; i++) paintWeek(i);
    }

    private String weekToStr() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (weekOn[i]) {
                if (sb.length() > 0) sb.append(",");
                sb.append(ProactiveTask.WEEK_DAYS[i]);
            }
        }
        return sb.toString();
    }

    private void save() {
        String prompt = etPrompt.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "提示词不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chosen.isEmpty()) {
            Toast.makeText(this, "请至少选择一个 AI", Toast.LENGTH_SHORT).show();
            return;
        }
        if (task.repeatMode == ProactiveTask.REPEAT_WEEKLY && weekToStr().isEmpty()) {
            Toast.makeText(this, "请选择至少一天", Toast.LENGTH_SHORT).show();
            return;
        }
        int count;
        try {
            count = Integer.parseInt(etCount.getText().toString().trim());
        } catch (Exception e) {
            count = 1;
        }
        if (count < 1) count = 1;
        if (count > 10) count = 10;

        StringBuilder ids = new StringBuilder();
        for (String id : chosen) {
            if (ids.length() > 0) ids.append("|");
            ids.append(id);
        }

        task.convs = ids.toString();
        task.prompt = prompt;
        task.sendCount = count;
        task.weekdays = weekToStr();
        task.enabled = true;
        task.plannedDay = -1;      // 设置变了，重新排时刻
        task.firedCount = 0;
        task.plannedTimes = "";

        if (isNew) {
            App.dbHelper().addTask(task);
        } else {
            App.dbHelper().updateTask(task);
        }
        ProactiveManager.apply(this);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
}
