package com.wechat.ai.ui.discover;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wechat.ai.R;

/**
 * 扫一扫（界面模拟，不做真实识别）。
 */
public class ScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        Toast.makeText(this, "扫一扫为界面模拟", Toast.LENGTH_SHORT).show();
    }
}
