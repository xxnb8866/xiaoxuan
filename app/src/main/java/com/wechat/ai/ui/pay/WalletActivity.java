package com.wechat.ai.ui.pay;

import android.view.View;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.R;
import com.wechat.ai.util.Prefs;

/**
 * 零钱：自定义余额（界面模拟，非真实资金）。
 * 快速金额按钮方便一键设定常见数值。
 */
public class WalletActivity extends AppCompatActivity {

    private static final double[] QUICK = {100, 500, 1000, 5000, 10000};

    private TextView balanceView;
    private EditText etAmount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        balanceView = findViewById(R.id.balance);
        etAmount = findViewById(R.id.et_amount);

        renderBalance();
        buildQuick();

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            View sv = findViewById(R.id.btn_save);
            if (sv instanceof com.wechat.ai.widget.ConfirmButton) {
                ((com.wechat.ai.widget.ConfirmButton) sv)
                        .setOnConfirmedListener(() -> save());
                ((com.wechat.ai.widget.ConfirmButton) sv).confirm();
            } else {
                save();
            }
        });
        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            Prefs.setBalance(0);
            renderBalance();
            etAmount.setText("");
            Toast.makeText(this, "余额已清零", Toast.LENGTH_SHORT).show();
        });
    }

    private void renderBalance() {
        balanceView.setText(String.format("¥%.2f", Prefs.getBalance()));
    }

    private void save() {
        String raw = etAmount.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
            return;
        }
        double v;
        try {
            v = Double.parseDouble(raw);
        } catch (Exception e) {
            Toast.makeText(this, "金额格式不对", Toast.LENGTH_SHORT).show();
            return;
        }
        if (v < 0) v = 0;
        Prefs.setBalance(v);
        renderBalance();
        Toast.makeText(this, "余额已更新", Toast.LENGTH_SHORT).show();
    }

    private void buildQuick() {
        LinearLayout box = findViewById(R.id.quick_box);
        box.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (double q : QUICK) {
            TextView t = new TextView(this);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, (int) (34 * density), 1);
            if (box.getChildCount() > 0) lp.leftMargin = (int) (6 * density);
            t.setLayoutParams(lp);
            t.setGravity(Gravity.CENTER);
            t.setText(String.format("%.0f", q));
            t.setTextSize(13);
            t.setIncludeFontPadding(false);
            t.setTextColor(getResources().getColor(R.color.wx_text_secondary));
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(17 * density);
            gd.setColor(getResources().getColor(R.color.wx_bg_gray));
            t.setBackground(gd);
            t.setOnClickListener(v -> {
                Prefs.setBalance(q);
                renderBalance();
                etAmount.setText(String.format("%.0f", q));
                Toast.makeText(this, "已设为 " + String.format("%.0f", q), Toast.LENGTH_SHORT).show();
            });
            box.addView(t);
        }
    }
}
