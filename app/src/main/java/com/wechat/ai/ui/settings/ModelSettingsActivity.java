package com.wechat.ai.ui.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wechat.ai.R;
import com.wechat.ai.ai.AiEngine;
import com.wechat.ai.ai.ModelPreset;
import com.wechat.ai.ai.ModelRegistry;
import com.wechat.ai.util.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型设置：内置各家厂商，选一个填 Key 就能用。
 */
public class ModelSettingsActivity extends AppCompatActivity {

    private String providerId = Prefs.getProvider();
    private RecyclerView providers;
    private Spinner spinnerModel;
    private EditText etModel;
    private EditText etKey;
    private EditText etBaseUrl;
    private EditText etSystem;
    private TextView tvTemp;
    private TextView tvTempDesc;
    private SeekBar seekTemp;
    private TextView keySite;
    private SwitchCompat swStream;
    private SwitchCompat swEmotion;
    private float temperature = Prefs.getTemperature();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_settings);

        providers = findViewById(R.id.providers);
        spinnerModel = findViewById(R.id.spinner_model);
        etModel = findViewById(R.id.et_model);
        etKey = findViewById(R.id.et_key);
        etBaseUrl = findViewById(R.id.et_base_url);
        etSystem = findViewById(R.id.et_system);
        tvTemp = findViewById(R.id.tv_temp);
        tvTempDesc = findViewById(R.id.tv_temp_desc);
        seekTemp = findViewById(R.id.seek_temp);
        keySite = findViewById(R.id.key_site);
        swStream = findViewById(R.id.sw_stream);
        swEmotion = findViewById(R.id.sw_emotion);

        etKey.setText(Prefs.getApiKey());
        etBaseUrl.setText(Prefs.getBaseUrl());
        etSystem.setText(Prefs.getSystemPrompt());
        etModel.setText("");
        swStream.setChecked(Prefs.isStream());
        swEmotion.setChecked(Prefs.isEmotionEnabled());
        renderTemp();
        seekTemp.setProgress((int) (temperature * 10));

        seekTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                temperature = progress / 10f;
                renderTemp();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        providers.setLayoutManager(new LinearLayoutManager(this));
        providers.setNestedScrollingEnabled(false);
        providers.setAdapter(new ProviderAdapter());

        bindProvider(ModelRegistry.get(providerId), true);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        View tempHelp = findViewById(R.id.btn_temp_help);
        if (tempHelp != null) tempHelp.setOnClickListener(v -> showTempHelp());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            View sv = findViewById(R.id.btn_save);
            if (sv instanceof com.wechat.ai.widget.ConfirmButton) {
                ((com.wechat.ai.widget.ConfirmButton) sv)
                        .setOnConfirmedListener(() -> save(true));
                ((com.wechat.ai.widget.ConfirmButton) sv).confirm();
            } else {
                save(true);
            }
        });
        findViewById(R.id.btn_test).setOnClickListener(v -> {
            save(false);
            Toast.makeText(this, "正在测试连接…", Toast.LENGTH_SHORT).show();
            AiEngine.test(new AiEngine.Callback() {
                @Override
                public void onDelta(String text) {
                }

                @Override
                public void onDone(String full, String emotion) {
                    Toast.makeText(ModelSettingsActivity.this,
                            "连接成功：" + full.trim(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(ModelSettingsActivity.this,
                            "连接失败：" + message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void bindProvider(ModelPreset p, boolean initSpinner) {
        providerId = p.id;
        keySite.setText("申请地址：" + p.keySite);
        if (initSpinner) {
            etBaseUrl.setText(p.baseUrl);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
            for (String m : p.models) adapter.add(m);
            adapter.notifyDataSetChanged();
            spinnerModel.setAdapter(adapter);
            int pos = 0;
            for (int i = 0; i < p.models.length; i++) {
                if (p.models[i].equals(Prefs.getModel())) pos = i;
            }
            spinnerModel.setSelection(pos);
        }
        providers.getAdapter().notifyDataSetChanged();
    }

    private void save(boolean toast) {
        String model = etModel.getText().toString().trim();
        if (model.isEmpty()) model = String.valueOf(spinnerModel.getSelectedItem());
        Prefs.putString(Prefs.KEY_PROVIDER, providerId);
        Prefs.putString(Prefs.KEY_API_KEY, etKey.getText().toString().trim());
        Prefs.putString(Prefs.KEY_MODEL, model);
        String url = etBaseUrl.getText().toString().trim();
        Prefs.putString(Prefs.KEY_BASE_URL, url.isEmpty() ? ModelRegistry.get(providerId).baseUrl : url);
        Prefs.putString(Prefs.KEY_SYSTEM_PROMPT, etSystem.getText().toString().trim());
        Prefs.putFloat(Prefs.KEY_TEMPERATURE, temperature);
        Prefs.putBoolean(Prefs.KEY_STREAM, swStream.isChecked());
        Prefs.putBoolean(Prefs.KEY_EMOTION, swEmotion.isChecked());
        if (toast) Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    private void renderTemp() {
        tvTemp.setText(String.format("%.1f", temperature));
        if (tvTempDesc != null) tvTempDesc.setText(tempDescription(temperature));
    }

    /** 温度说明：随数值变化给出直观解释 */
    private String tempDescription(float t) {
        if (t < 0.35f) {
            return "很低：回答稳定严谨，几乎每次都一样。适合问答、查资料、写代码。";
        } else if (t < 0.75f) {
            return "偏低：表达自然但不乱发挥，日常聊天最省心的档位。";
        } else if (t < 1.15f) {
            return "默认：自然、有变化，聊天与创作都能兼顾。";
        } else if (t < 1.6f) {
            return "偏高：更有想象力和情绪起伏，聊起来更有「人味」。";
        }
        return "很高：天马行空、脑洞大开，但容易跑题或胡编，慎用。";
    }

    private void showTempHelp() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("什么是温度 Temperature？")
                .setMessage("温度是模型生成文字时的「随机程度」旋钮，取值 0 ~ 2。\n\n"
                        + "· 调低（0 ~ 0.6）：模型更倾向于选最稳妥的词，回答严谨、重复问也基本一致，"
                        + "适合查资料、写代码、需要准确度的场景。\n\n"
                        + "· 适中（0.7 ~ 1.0）：日常聊天的默认区间，说话自然又不至于乱跑。\n\n"
                        + "· 调高（1.0 ~ 2.0）：模型更敢选冷门词，更有创意和情绪，"
                        + "但也更容易跑题、编造内容。\n\n"
                        + "简单记：想要靠谱就调低，想要有趣就调高。")
                .setPositiveButton("知道了", null)
                .show();
    }

    class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.Holder> {

        private final List<ModelPreset> list = ModelRegistry.all();

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_model_preset, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder h, int position) {
            ModelPreset p = list.get(position);
            h.name.setText(p.name);
            h.url.setText(p.baseUrl.replace("https://", "").replace("http://", ""));
            h.check.setVisibility(p.id.equals(providerId) ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> bindProvider(p, true));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView name;
            TextView url;
            ImageView check;

            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.name);
                url = v.findViewById(R.id.url);
                check = v.findViewById(R.id.check);
            }
        }
    }
}
