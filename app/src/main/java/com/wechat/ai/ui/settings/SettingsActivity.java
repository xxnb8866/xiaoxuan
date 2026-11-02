package com.wechat.ai.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.ui.emoji.EmojiManageActivity;
import com.wechat.ai.util.Prefs;

/**
 * 设置页：个人信息 / 模型设置 / 表情包管理 / 通用 / 隐私 / 关于。
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int[] ICONS = {
            R.drawable.ic_profile, R.drawable.ic_settings_model, R.drawable.ic_prompt,
            R.drawable.ic_life, R.drawable.ic_emoji_smile, R.drawable.ic_photo_album,
            R.drawable.ic_bell, R.drawable.ic_settings_general, R.drawable.ic_privacy,
            R.drawable.ic_about
    };
    private static final String[] TITLES = {
            "个人信息", "模型设置", "提示词管理", "AI 的人生经历", "表情包管理",
            "AI 相册", "AI 主动消息", "通用", "隐私", "关于"
    };
    // 注意：SUBS 必须与 TITLES 一一对应、长度相同，
    // 否则循环里取 SUBS[i] 会数组越界直接闪退。
    private static final String[] SUBS = {
            "",                                     // 个人信息
            "",                                     // 模型设置
            "保存并复用人设模板",                     // 提示词管理
            "让 AI 像个真人，不再有 AI 腔",           // AI 的人生经历
            "",                                     // 表情包管理
            "上传照片打标签，聊到相近话题自动发",     // AI 相册
            "可创建多个时间段任务",                   // AI 主动消息
            "聊天字体大小、表情包频率",               // 通用
            "通讯录黑名单",                          // 隐私
            "版本 2.3"                               // 关于
    };
    private static final int[] COLORS = {
            0xFF3D7EFF,  // 个人信息
            0xFF07C160,  // 模型设置
            0xFF9270CA,  // 提示词管理
            0xFFE8544E,  // AI 的人生经历
            0xFFF5A623,  // 表情包管理
            0xFF00C8C8,  // AI 相册
            0xFFFA9D3B,  // AI 主动消息
            0xFF3D7EFF,  // 通用
            0xFFFF9500,  // 隐私
            0xFF8A8A8A   // 关于
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View entries = findViewById(R.id.entries);
        android.widget.LinearLayout box = findViewById(R.id.entries);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        for (int i = 0; i < TITLES.length; i++) {
            View item = getLayoutInflater().inflate(R.layout.item_discover, box, false);
            android.widget.ImageView icon = item.findViewById(R.id.icon);
            android.widget.TextView title = item.findViewById(R.id.title);
            android.widget.TextView sub = item.findViewById(R.id.sub);
            View iconBox = item.findViewById(R.id.icon_box);
            // 越界防护：四个数组理论上等长，万一以后增删条目漏改一个，
            // 也只当缺省值处理，绝不因此闪退。
            if (i < ICONS.length) icon.setImageResource(ICONS[i]);
            icon.setColorFilter(0xFFFFFFFF);
            title.setText(TITLES[i]);
            String subText = (i < SUBS.length) ? SUBS[i] : "";
            if (subText != null && !subText.isEmpty()) {
                sub.setVisibility(View.VISIBLE);
                sub.setText(subText);
            }
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(i < COLORS.length ? COLORS[i] : 0xFF3D7EFF);
            gd.setCornerRadius(10 * getResources().getDisplayMetrics().density);
            iconBox.setBackground(gd);
            final int index = i;
            item.setOnClickListener(v -> onEntryClick(index));
            box.addView(item);
            // 入场：从上方向下依次堆叠展开
            com.wechat.ai.widget.AnimHelper.expand(item, 40L * i);
            if (i < TITLES.length - 1) {
                View line = new View(this);
                line.setBackgroundColor(getResources().getColor(R.color.wx_divider_light));
                android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.leftMargin = (int) (58 * getResources().getDisplayMetrics().density);
                box.addView(line, lp);
            }
        }

        findViewById(R.id.btn_logout).setOnClickListener(v ->
                Toast.makeText(this, "退出登录为界面模拟", Toast.LENGTH_SHORT).show());
    }

    private void onEntryClick(int index) {
        switch (index) {
            case 0:                                     // 个人信息
                startActivity(new Intent(this, ProfileEditActivity.class));
                break;
            case 1:                                     // 模型设置
                startActivity(new Intent(this, ModelSettingsActivity.class));
                break;
            case 2:                                     // 提示词管理
                startActivity(new Intent(this, com.wechat.ai.ui.contacts.PromptManageActivity.class));
                break;
            case 3:                                     // AI 的人生经历
                startActivity(new Intent(this, MemoryActivity.class));
                break;
            case 4:                                     // 表情包管理
                startActivity(new Intent(this, EmojiManageActivity.class));
                break;
            case 5:                                     // AI 相册
                startActivity(new Intent(this, com.wechat.ai.ui.emoji.PhotoManageActivity.class));
                break;
            case 6:                                     // AI 主动消息
                startActivity(new Intent(this, ProactiveActivity.class));
                break;
            case 7:                                     // 通用
                showGeneral();
                break;
            case 8:                                     // 隐私
                showPrivacy();
                break;
            default:                                    // 关于
                showAbout();
                break;
        }
    }

    private void showGeneral() {
        String[] items = {"清空所有聊天记录", "每 N 条回复发一次表情包"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("清空所有聊天记录？")
                                .setPositiveButton("清空", (d, w) -> {
                                    App.db().delete("messages", null, null);
                                    Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        String[] opts = {"每条都发", "每 2 条", "每 3 条", "每 5 条"};
                        int[] values = {1, 2, 3, 5};
                        new AlertDialog.Builder(this)
                                .setTitle("表情包发送频率")
                                .setItems(opts, (d, w) -> {
                                    Prefs.setEmotionFreq(values[w]);
                                    Toast.makeText(this, "已设置：" + opts[w], Toast.LENGTH_SHORT).show();
                                })
                                .show();
                    }
                })
                .show();
    }

    /** 隐私：界面模拟的黑名单入口 */
    private void showPrivacy() {
        String[] items = {"通讯录黑名单", "添加朋友权限"};
        new AlertDialog.Builder(this)
                .setTitle("隐私")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("通讯录黑名单")
                                .setMessage("暂无黑名单成员")
                                .setPositiveButton("知道了", null)
                                .show();
                    } else {
                        String[] opts = {"允许所有人", "需要验证", "不允许任何人"};
                        new AlertDialog.Builder(this)
                                .setTitle("添加朋友权限")
                                .setItems(opts, (d, w) ->
                                        Toast.makeText(this, "已设置：" + opts[w],
                                                Toast.LENGTH_SHORT).show())
                                .show();
                    }
                })
                .show();
    }

    /** 关于：版本与说明 */
    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage("仿微信 AI 聊天工具\n版本 2.3\n"
                        + "本项目仅用于学习交流，UI 为界面模拟，红包与转账不具备真实支付能力。")
                .setPositiveButton("知道了", null)
                .show();
    }
}
