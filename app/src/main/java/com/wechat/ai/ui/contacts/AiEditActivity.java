package com.wechat.ai.ui.contacts;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.model.PromptTemplate;
import com.wechat.ai.util.ImageUtils;
import com.wechat.ai.widget.AvatarIcons;
import com.wechat.ai.widget.AvatarView;

import java.util.List;
import java.util.UUID;

/**
 * 新建 / 编辑 AI 好友：头像、名称、人设提示词、温度、模型。
 */
public class AiEditActivity extends AppCompatActivity {

    public static final String EXTRA_CONV_ID = "conv_id";
    /** 选择提示词模板后回传 */
    public static final String EXTRA_PROMPT_CONTENT = "prompt_content";
    public static final String EXTRA_PROMPT_NAME = "prompt_name";

    private static final int[] COLORS = {
            0xFF5B8FF9, 0xFF07C160, 0xFFF6BD16, 0xFFE8684A,
            0xFF6DC8EC, 0xFF9270CA, 0xFFFF9D8A, 0xFF26A69A,
            0xFF5C6BC0, 0xFF8D6E63
    };

    private String convId = null;
    private Conversation conv;

    private AvatarView avatar;
    private EditText etName;
    private EditText etPrompt;
    private EditText etModel;
    private TextView tvTitle;
    private TextView tvTemp;
    private TextView tvTempDesc;
    private SeekBar seekTemp;
    private SwitchCompat swGlobalModel;
    private LinearLayout rowModel;
    private TextView btnDelete;
    private RecyclerView colors;

    private int avatarColor = COLORS[1];
    private String avatarPath = "";
    private String avatarIcon = "ai";
    private float temperature = 0.7f;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String path = ImageUtils.copyAvatar(this, uri);
                if (path == null) {
                    Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                avatarPath = path;
                renderAvatar();
            });

    private final ActivityResultLauncher<Intent> pickPrompt =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK || r.getData() == null) return;
                String content = r.getData().getStringExtra(EXTRA_PROMPT_CONTENT);
                if (!TextUtils.isEmpty(content)) {
                    etPrompt.setText(content);
                    etPrompt.setSelection(content.length());
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_edit);

        convId = getIntent().getStringExtra(EXTRA_CONV_ID);

        avatar = findViewById(R.id.avatar);
        etName = findViewById(R.id.et_name);
        etPrompt = findViewById(R.id.et_prompt);
        etModel = findViewById(R.id.et_model);
        tvTitle = findViewById(R.id.tv_title);
        tvTemp = findViewById(R.id.tv_temp);
        tvTempDesc = findViewById(R.id.tv_temp_desc);
        seekTemp = findViewById(R.id.seek_temp);
        swGlobalModel = findViewById(R.id.sw_global_model);
        rowModel = findViewById(R.id.row_model);
        btnDelete = findViewById(R.id.btn_delete);
        colors = findViewById(R.id.colors);

        boolean editing = convId != null && !convId.isEmpty();
        tvTitle.setText(editing ? "编辑 AI 好友" : "新建 AI 好友");

        if (editing) {
            conv = App.dbHelper().getConversation(convId);
            etName.setText(conv.name);
            etPrompt.setText(conv.systemPrompt == null ? "" : conv.systemPrompt);
            avatarColor = conv.avatarColor;
            avatarPath = conv.avatarPath == null ? "" : conv.avatarPath;
            avatarIcon = conv.avatarIcon == null ? "ai" : conv.avatarIcon;
            temperature = conv.temperature < 0 ? 0.7f : conv.temperature;
            boolean useGlobal = conv.model == null || conv.model.isEmpty();
            swGlobalModel.setChecked(useGlobal);
            rowModel.setVisibility(useGlobal ? View.GONE : View.VISIBLE);
            etModel.setText(useGlobal ? "" : conv.model);
            btnDelete.setVisibility(View.VISIBLE);
        }

        colors.setLayoutManager(new GridLayoutManager(this, 5));
        colors.setNestedScrollingEnabled(false);
        colors.setAdapter(new ColorAdapter());

        seekTemp.setProgress((int) (temperature * 10));
        renderTemp();
        renderAvatar();

        seekTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                temperature = progress / 10f;
                renderTemp();
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });

        swGlobalModel.setOnCheckedChangeListener((btn, checked) ->
                rowModel.setVisibility(checked ? View.GONE : View.VISIBLE));

        avatar.setOnClickListener(v -> showAvatarMenu());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            View sv = findViewById(R.id.btn_save);
            if (sv instanceof com.wechat.ai.widget.ConfirmButton) {
                ((com.wechat.ai.widget.ConfirmButton) sv).setOnConfirmedListener(() -> save());
                ((com.wechat.ai.widget.ConfirmButton) sv).confirm();
            } else {
                save();
            }
        });
        findViewById(R.id.btn_temp_help).setOnClickListener(v -> showTempHelp());
        findViewById(R.id.btn_pick_prompt).setOnClickListener(v ->
                pickPrompt.launch(new Intent(this, PromptManageActivity.class)
                        .putExtra(PromptManageActivity.EXTRA_PICK_MODE, true)));
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void renderAvatar() {
        avatar.bind(avatarPath, AvatarIcons.resOf(avatarIcon),
                etName.getText().toString().trim(), avatarColor);
        if (colors.getAdapter() != null) colors.getAdapter().notifyDataSetChanged();
    }

    private void renderTemp() {
        tvTemp.setText(String.format("%.1f", temperature));
        tvTempDesc.setText(tempDescription(temperature));
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
        new AlertDialog.Builder(this)
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

    /** 头像来源：内置图标 / 相册 / 移除图片 */
    private void showAvatarMenu() {
        String[] items = {"选择内置图标", "从相册选择", "移除当前图片"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    if (which == 0) showIconPicker();
                    else if (which == 1) pickImage.launch("image/*");
                    else {
                        avatarPath = "";
                        renderAvatar();
                        Toast.makeText(this, "已移除图片头像", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showIconPicker() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_icon_picker, null);
        RecyclerView grid = v.findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        final int[] chosen = {Math.max(0, AvatarIcons.indexOf(avatarIcon))};
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择内置头像")
                .setView(v)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    avatarIcon = AvatarIcons.nameAt(chosen[0]);
                    avatarPath = "";
                    renderAvatar();
                })
                .show();

        grid.setAdapter(new RecyclerView.Adapter<IconHolder>() {
            @NonNull
            @Override
            public IconHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_icon_pick, parent, false);
                return new IconHolder(item);
            }

            @Override
            public void onBindViewHolder(@NonNull IconHolder h, int position) {
                h.avatar.setIcon(AvatarIcons.resAt(position), avatarColor);
                h.check.setVisibility(position == chosen[0] ? View.VISIBLE : View.GONE);
                h.itemView.setOnClickListener(vv -> {
                    int old = chosen[0];
                    chosen[0] = position;
                    notifyItemChanged(old);
                    notifyItemChanged(position);
                });
            }

            @Override
            public int getItemCount() {
                return AvatarIcons.count();
            }
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.wx_green));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.wx_text_secondary));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除「" + conv.name + "」？")
                .setMessage("聊天记录会一并删除，且无法恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    App.dbHelper().deleteConversation(convId);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .show();
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请先给 TA 起个名字", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return;
        }

        if (conv == null) {
            conv = new Conversation();
            conv.id = "ai_" + UUID.randomUUID().toString().substring(0, 8);
            conv.isAi = true;
        }
        conv.name = name;
        conv.avatarColor = avatarColor;
        conv.avatarPath = avatarPath.isEmpty() ? null : avatarPath;
        conv.avatarIcon = avatarIcon;
        conv.systemPrompt = etPrompt.getText().toString().trim();
        conv.temperature = temperature;
        conv.model = swGlobalModel.isChecked() ? null : etModel.getText().toString().trim();

        if (convId == null || convId.isEmpty()) {
            App.dbHelper().addConversation(conv);
        } else {
            App.dbHelper().updateConversation(conv);
        }
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ---------------- 配色选择 ----------------

    class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_color_pick, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            int color = COLORS[position];
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(color);
            gd.setShape(GradientDrawable.OVAL);
            h.dot.setBackground(gd);
            h.check.setVisibility(color == avatarColor ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> {
                avatarColor = color;
                notifyDataSetChanged();
                renderAvatar();
            });
        }

        @Override
        public int getItemCount() {
            return COLORS.length;
        }

        class Holder extends RecyclerView.ViewHolder {
            View dot;
            ImageView check;

            Holder(View v) {
                super(v);
                dot = v.findViewById(R.id.dot);
                check = v.findViewById(R.id.check);
            }
        }
    }

    static class IconHolder extends RecyclerView.ViewHolder {
        AvatarView avatar;
        ImageView check;

        IconHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.avatar);
            check = v.findViewById(R.id.check);
        }
    }
}
