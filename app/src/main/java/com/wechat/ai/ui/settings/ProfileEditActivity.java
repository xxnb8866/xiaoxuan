package com.wechat.ai.ui.settings;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wechat.ai.R;
import com.wechat.ai.util.ImageUtils;
import com.wechat.ai.util.Prefs;
import com.wechat.ai.widget.AvatarIcons;
import com.wechat.ai.widget.AvatarView;

/**
 * 个人信息编辑：昵称 / 微信号 / 签名 / 头像（相册图片、内置图标、底色）。
 */
public class ProfileEditActivity extends AppCompatActivity {

    private static final int[] COLORS = {
            0xFF5B8FF9, 0xFF07C160, 0xFFF6BD16, 0xFFE8684A, 0xFF6DC8EC,
            0xFF9270CA, 0xFFFF9D8A, 0xFF26A69A, 0xFF5C6BC0, 0xFF8D6E63
    };

    private int color = 0xFF5B8FF9;
    private String avatarPath = "";
    private String avatarIcon = "";

    private AvatarView avatar;
    private EditText etNick;
    private RecyclerView colors;
    private ColorAdapter colorAdapter;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String path = ImageUtils.copyAvatar(this, uri);
                if (path == null) {
                    Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                avatarPath = path;
                avatarIcon = "";
                renderAvatar();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        avatar = findViewById(R.id.avatar);
        etNick = findViewById(R.id.et_nick);
        EditText etWxid = findViewById(R.id.et_wxid);
        EditText etSign = findViewById(R.id.et_sign);
        colors = findViewById(R.id.colors);

        etNick.setText(Prefs.getNickname());
        etWxid.setText(Prefs.getWxid());
        etSign.setText(Prefs.getSignature());
        color = Prefs.getAvatarColor();
        avatarPath = Prefs.getAvatarPath();
        avatarIcon = Prefs.getAvatarIcon();
        renderAvatar();

        colors.setLayoutManager(new GridLayoutManager(this, 5));
        colors.setNestedScrollingEnabled(false);
        colorAdapter = new ColorAdapter();
        colors.setAdapter(colorAdapter);

        etNick.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderAvatar();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        avatar.setOnClickListener(v -> showAvatarMenu());
        findViewById(R.id.btn_pick_photo).setOnClickListener(v -> pickImage.launch("image/*"));
        findViewById(R.id.btn_pick_icon).setOnClickListener(v -> showIconPicker());
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            View sv = findViewById(R.id.btn_save);
            if (sv instanceof com.wechat.ai.widget.ConfirmButton) {
                ((com.wechat.ai.widget.ConfirmButton) sv)
                        .setOnConfirmedListener(() -> save(etWxid, etSign));
                ((com.wechat.ai.widget.ConfirmButton) sv).confirm();
            } else {
                save(etWxid, etSign);
            }
        });
    }

    private void renderAvatar() {
        String n = etNick.getText().toString().trim();
        avatar.bind(avatarPath, AvatarIcons.resOf(avatarIcon), n.isEmpty() ? "我" : n.substring(0, 1), color);
        if (colorAdapter != null) colorAdapter.notifyDataSetChanged();
    }

    private void showAvatarMenu() {
        String[] items = {"从相册选一张", "用内置头像", "移除当前图片"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    if (which == 0) pickImage.launch("image/*");
                    else if (which == 1) showIconPicker();
                    else {
                        avatarPath = "";
                        renderAvatar();
                    }
                })
                .show();
    }

    private void showIconPicker() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_icon_picker, null);
        RecyclerView grid = v.findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        final int[] chosen = {Math.max(0, AvatarIcons.indexOf(avatarIcon))};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择内置头像")
                .setView(v)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    avatarIcon = AvatarIcons.nameAt(chosen[0]);
                    avatarPath = "";
                    renderAvatar();
                })
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.wx_green));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.wx_text_secondary));

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
                h.avatar.setIcon(AvatarIcons.resAt(position), color);
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
    }

    private void save(EditText etWxid, EditText etSign) {
        String nick = etNick.getText().toString().trim();
        if (TextUtils.isEmpty(nick)) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        String wxid = etWxid.getText().toString().trim();
        Prefs.putString(Prefs.KEY_NICKNAME, nick);
        Prefs.putString(Prefs.KEY_WXID, wxid.isEmpty() ? "wxid_ai8888" : wxid);
        Prefs.putString(Prefs.KEY_SIGNATURE, etSign.getText().toString().trim());
        Prefs.putString(Prefs.KEY_AVATAR_PATH, avatarPath);
        Prefs.putString(Prefs.KEY_AVATAR_ICON, avatarIcon);
        Prefs.putString("avatar_color", String.valueOf(color));
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

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
            int c = COLORS[position];
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(c);
            gd.setShape(GradientDrawable.OVAL);
            h.dot.setBackground(gd);
            h.check.setVisibility(c == color ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> {
                color = c;
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
