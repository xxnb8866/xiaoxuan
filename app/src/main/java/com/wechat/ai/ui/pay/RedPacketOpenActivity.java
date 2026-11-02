package com.wechat.ai.ui.pay;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.R;

/**
 * 开红包（前端模拟）：展示金额、主题、标语。
 */
public class RedPacketOpenActivity extends AppCompatActivity {

    private long msgId = -1;
    private String amount = "0.00";
    private String wish = "";
    private String theme = "";
    private String from = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redpacket_open);

        msgId = getIntent().getLongExtra("msg_id", -1);
        amount = getIntent().getStringExtra("amount");
        wish = getIntent().getStringExtra("wish");
        theme = getIntent().getStringExtra("theme");
        from = getIntent().getStringExtra("from");

        TextView tvFrom = findViewById(R.id.from);
        TextView tvWish = findViewById(R.id.wish);
        TextView tvTheme = findViewById(R.id.theme);
        TextView btnOpen = findViewById(R.id.btn_open);
        TextView tvAmount = findViewById(R.id.amount);
        TextView tip = findViewById(R.id.tip);

        tvFrom.setText("来自 " + from + " 的红包");
        tvWish.setText(wish);
        tvTheme.setText(theme);

        btnOpen.setOnClickListener(v -> {
            RotateAnimation anim = new RotateAnimation(0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(600);
            btnOpen.startAnimation(anim);
            btnOpen.postDelayed(() -> {
                btnOpen.setVisibility(View.GONE);
                tvAmount.setVisibility(View.VISIBLE);
                tvAmount.setText("¥ " + amount);
                tip.setText("已存入零钱");
                Intent data = new Intent();
                data.putExtra("msg_id", msgId);
                data.putExtra("amount", amount);
                data.putExtra("wish", wish);
                data.putExtra("theme", theme);
                setResult(RESULT_OK, data);
            }, 600);
        });

        findViewById(R.id.iv_close).setOnClickListener(v -> finish());
        findViewById(R.id.tv_detail).setOnClickListener(v ->
                Toast.makeText(this, "金额 ¥" + amount + " · " + theme, Toast.LENGTH_SHORT).show());
    }
}
