package com.wechat.ai.ui.me;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wechat.ai.R;
import com.wechat.ai.ui.emoji.EmojiManageActivity;
import com.wechat.ai.ui.settings.SettingsActivity;
import com.wechat.ai.util.Prefs;
import com.wechat.ai.widget.AvatarView;

/**
 * 「我」Tab：个人信息 + 支付 / 收藏 / 表情 / 设置 等入口。
 */
public class MeFragment extends Fragment {

    private static final int[] ICONS = {
            R.drawable.ic_pay_wallet, R.drawable.ic_life, R.drawable.ic_emoji_smile,
            R.drawable.ic_settings
    };
    private static final String[] TITLES = {"支付", "AI 的人生经历", "表情", "设置"};
    private static final int[] COLORS = {
            0xFF1AAD19, 0xFFE8544E, 0xFFF5A623, 0xFF3D7EFF
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_me, container, false);
        AvatarView avatar = v.findViewById(R.id.avatar);
        TextView nickname = v.findViewById(R.id.nickname);
        TextView wxid = v.findViewById(R.id.wxid);
        TextView signature = v.findViewById(R.id.signature);
        LinearLayout entries = v.findViewById(R.id.entries);

        avatar.bind(com.wechat.ai.util.MyAvatar.path(),
                com.wechat.ai.util.MyAvatar.iconRes(),
                com.wechat.ai.util.MyAvatar.text(),
                com.wechat.ai.util.MyAvatar.color());
        nickname.setText(Prefs.getNickname());
        wxid.setText("微信号：" + Prefs.getWxid());
        signature.setText(Prefs.getSignature());

        for (int i = 0; i < TITLES.length; i++) {
            View item = LayoutInflater.from(getContext()).inflate(R.layout.item_discover, entries, false);
            ImageView icon = item.findViewById(R.id.icon);
            TextView title = item.findViewById(R.id.title);
            View box = item.findViewById(R.id.icon_box);
            icon.setImageResource(ICONS[i]);
            icon.setColorFilter(0xFFFFFFFF);
            title.setText(TITLES[i]);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(COLORS[i]);
            gd.setCornerRadius(10 * getResources().getDisplayMetrics().density);
            box.setBackground(gd);
            final int index = i;
            item.setOnClickListener(view -> onEntryClick(index));
            entries.addView(item);
            if (i < TITLES.length - 1) {
                View line = new View(getContext());
                line.setBackgroundColor(getResources().getColor(R.color.wx_divider_light));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.leftMargin = (int) (58 * getResources().getDisplayMetrics().density);
                entries.addView(line, lp);
            }
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /** 供 MainActivity 切回本页时刷新资料 */
    public void refresh() {
        View root = getView();
        if (root == null) return;
        String nick = Prefs.getNickname();
        ((AvatarView) root.findViewById(R.id.avatar)).bind(
                com.wechat.ai.util.MyAvatar.path(),
                com.wechat.ai.util.MyAvatar.iconRes(),
                com.wechat.ai.util.MyAvatar.text(),
                com.wechat.ai.util.MyAvatar.color());
        ((TextView) root.findViewById(R.id.nickname)).setText(nick);
        ((TextView) root.findViewById(R.id.wxid)).setText("微信号：" + Prefs.getWxid());
        ((TextView) root.findViewById(R.id.signature)).setText(Prefs.getSignature());
    }

    private int avatarColor() {
        try {
            return Integer.parseInt(Prefs.getString("avatar_color", "-10872839"));
        } catch (Exception e) {
            return 0xFF5B8FF9;
        }
    }

    private void onEntryClick(int index) {
        switch (index) {
            case 0:                                  // 支付（零钱）
                startActivity(new Intent(getContext(),
                        com.wechat.ai.ui.pay.WalletActivity.class));
                break;
            case 1:                                  // AI 的人生经历
                startActivity(new Intent(getContext(),
                        com.wechat.ai.ui.settings.MemoryActivity.class));
                break;
            case 2:                                  // 表情
                startActivity(new Intent(getContext(), EmojiManageActivity.class));
                break;
            case 3:                                  // 设置
                startActivity(new Intent(getContext(), SettingsActivity.class));
                break;
            default:
                Toast.makeText(getContext(), TITLES[index] + "（界面模拟）", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
