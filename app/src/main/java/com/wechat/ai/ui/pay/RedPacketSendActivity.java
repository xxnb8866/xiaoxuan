package com.wechat.ai.ui.pay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.R;
import com.wechat.ai.util.Prefs;

/**
 * 发红包（前端模拟）：金额 / 主题 / 标语。
 */
public class RedPacketSendActivity extends AppCompatActivity {

    private static final String[] THEMES = {
            "普通红包", "恭喜发财", "生日快乐", "谢谢老板", "新年快乐", "一点心意"
    };

    private String theme = THEMES[0];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redpacket_send);

        String to = getIntent().getStringExtra("to");
        EditText etAmount = findViewById(R.id.et_amount);
        EditText etCount = findViewById(R.id.et_count);
        EditText etWish = findViewById(R.id.et_wish);
        LinearLayout themeBox = findViewById(R.id.theme_box);
        TextView balance = findViewById(R.id.balance);

        balance.setText(String.format("零钱余额 ¥%.2f", Prefs.getBalance()));

        buildThemes(themeBox);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_put).setOnClickListener(v -> {
            String amount = etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amount)) {
                Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
                return;
            }
            double value;
            try {
                value = Double.parseDouble(amount);
            } catch (Exception e) {
                Toast.makeText(this, "金额格式不对", Toast.LENGTH_SHORT).show();
                return;
            }
            if (value <= 0) {
                Toast.makeText(this, "金额要大于 0", Toast.LENGTH_SHORT).show();
                return;
            }
            String wish = etWish.getText().toString().trim();
            if (wish.isEmpty()) wish = getString(R.string.rp_wish_default);
            String count = etCount.getText().toString().trim();
            if (count.isEmpty()) count = "1";

            Intent data = new Intent();
            data.putExtra("amount", String.format("%.2f", value));
            data.putExtra("wish", wish);
            data.putExtra("theme", theme + " × " + count);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void buildThemes(LinearLayout box) {
        for (int i = 0; i < THEMES.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(THEMES[i]);
            tv.setTextSize(13);
            tv.setTextColor(getResources().getColor(R.color.wx_text_primary));
            tv.setPadding(28, 12, 28, 12);
            tv.setBackgroundResource(i == 0 ? R.drawable.bg_tag_chip_selected : R.drawable.bg_tag_chip);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = 16;
            final int index = i;
            tv.setOnClickListener(v -> {
                theme = THEMES[index];
                for (int j = 0; j < box.getChildCount(); j++) {
                    box.getChildAt(j).setBackgroundResource(
                            j == index ? R.drawable.bg_tag_chip_selected : R.drawable.bg_tag_chip);
                }
            });
            box.addView(tv, lp);
        }
    }
}
