package com.wechat.ai.ui.pay;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.R;
import com.wechat.ai.util.Prefs;

/**
 * 转账（前端模拟）：金额 + 说明 + 六位支付密码键盘。
 */
public class TransferActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        String to = getIntent().getStringExtra("to");
        TextView tvTo = findViewById(R.id.to);
        tvTo.setText("转账给 " + (to == null ? "好友" : to));

        EditText etAmount = findViewById(R.id.et_amount);
        EditText etNote = findViewById(R.id.et_note);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_pay).setOnClickListener(v -> {
            String amount = etAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amount)) {
                Toast.makeText(this, "请输入转账金额", Toast.LENGTH_SHORT).show();
                return;
            }
            double value;
            try {
                value = Double.parseDouble(amount);
            } catch (Exception e) {
                Toast.makeText(this, "金额格式不对", Toast.LENGTH_SHORT).show();
                return;
            }
            // 前端界面模拟，不做余额校验、不设金额上限
            if (value <= 0) {
                Toast.makeText(this, "请输入大于 0 的金额", Toast.LENGTH_SHORT).show();
                return;
            }
            showPasswordPad(String.format("%.2f", value),
                    etNote.getText().toString().trim());
        });
    }

    /** 微信风格的六位密码盘 */
    private void showPasswordPad(String amount, String note) {
        View v = getLayoutInflater().inflate(R.layout.dialog_password, null);
        LinearLayout boxes = v.findViewById(R.id.boxes);
        GridLayout keypad = v.findViewById(R.id.keypad);
        TextView tvAmount = v.findViewById(R.id.amount);
        TextView tvNote = v.findViewById(R.id.note);
        tvAmount.setText("¥" + amount);
        tvNote.setText(TextUtils.isEmpty(note) ? "转账" : note);

        final StringBuilder pwd = new StringBuilder();

        // 六个密码格
        int boxW = (int) (52 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(boxW, ViewGroup.LayoutParams.MATCH_PARENT);
        for (int i = 0; i < 6; i++) {
            TextView box = new TextView(this);
            box.setGravity(Gravity.CENTER);
            box.setTextSize(20);
            box.setTextColor(getResources().getColor(R.color.wx_text_primary));
            box.setBackgroundResource(R.drawable.bg_password_boxes);
            if (i > 0) {
                boxLp = new LinearLayout.LayoutParams(boxW, ViewGroup.LayoutParams.MATCH_PARENT);
                boxLp.leftMargin = -1;
            }
            boxes.addView(box, boxLp);
        }

        Runnable[] renderBoxes = new Runnable[1];
        renderBoxes[0] = () -> {
            for (int i = 0; i < 6; i++) {
                TextView box = (TextView) boxes.getChildAt(i);
                box.setText(i < pwd.length() ? "●" : "");
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(true)
                .create();

        // 数字键
        for (int i = 0; i < keypad.getChildCount(); i++) {
            View child = keypad.getChildAt(i);
            if (!(child instanceof TextView)) continue;
            TextView key = (TextView) child;
            String label = key.getText().toString();
            if (label.isEmpty()) continue;
            if ("删除".equals(label)) {
                key.setOnClickListener(vv -> {
                    if (pwd.length() > 0) {
                        pwd.deleteCharAt(pwd.length() - 1);
                        renderBoxes[0].run();
                    }
                });
            } else {
                key.setOnClickListener(vv -> {
                    if (pwd.length() >= 6) return;
                    pwd.append(label);
                    renderBoxes[0].run();
                    if (pwd.length() == 6) {
                        // 模拟：任意 6 位即通过
                        dialog.dismiss();
                        Intent data = new Intent();
                        data.putExtra("amount", amount);
                        data.putExtra("note", note);
                        setResult(RESULT_OK, data);
                        finish();
                    }
                });
            }
        }

        v.findViewById(R.id.btn_close).setOnClickListener(vv -> dialog.dismiss());
        dialog.show();
    }
}
