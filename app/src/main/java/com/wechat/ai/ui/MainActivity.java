package com.wechat.ai.ui;

import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.wechat.ai.R;
import com.wechat.ai.ui.chat.ChatListFragment;
import com.wechat.ai.ui.contacts.AiEditActivity;
import com.wechat.ai.ui.contacts.ContactsFragment;
import com.wechat.ai.ui.discover.DiscoverFragment;
import com.wechat.ai.ui.moments.PostMomentActivity;
import com.wechat.ai.ui.me.MeFragment;

/**
 * 主界面：底部四个 Tab + 顶部仿微信导航栏。
 */
public class MainActivity extends AppCompatActivity {

    private ViewPager2 pager;
    private ImageView[] icons = new ImageView[4];
    private TextView[] texts = new TextView[4];
    private View[] tabs = new View[4];
    private View indicator;
    private TextView title;
    private ImageView actionPlus;
    private FrameLayout actionbar;
    private int current = -1;   // -1 保证首次 select(0) 能正常执行动画

    private static final String[] TITLES = {"微信", "AI 好友", "发现", "我"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNotifyPermission();
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        title = findViewById(R.id.title);
        actionPlus = findViewById(R.id.action_plus);
        actionbar = findViewById(R.id.actionbar);

        // 支持左右滑动切换页面
        pager.setUserInputEnabled(true);
        pager.setAdapter(new MainAdapter(this));

        LinearLayout navChat = findViewById(R.id.nav_chat);
        LinearLayout navContacts = findViewById(R.id.nav_contacts);
        LinearLayout navDiscover = findViewById(R.id.nav_discover);
        LinearLayout navMe = findViewById(R.id.nav_me);

        indicator = findViewById(R.id.nav_indicator);
        tabs[0] = navChat;
        tabs[1] = navContacts;
        tabs[2] = navDiscover;
        tabs[3] = navMe;

        icons[0] = findViewById(R.id.nav_chat_icon);
        icons[1] = findViewById(R.id.nav_contacts_icon);
        icons[2] = findViewById(R.id.nav_discover_icon);
        icons[3] = findViewById(R.id.nav_me_icon);
        texts[0] = findViewById(R.id.nav_chat_text);
        texts[1] = findViewById(R.id.nav_contacts_text);
        texts[2] = findViewById(R.id.nav_discover_text);
        texts[3] = findViewById(R.id.nav_me_text);

        navChat.setOnClickListener(v -> select(0));
        navContacts.setOnClickListener(v -> select(1));
        navDiscover.setOnClickListener(v -> select(2));
        navMe.setOnClickListener(v -> select(3));

        actionPlus.setOnClickListener(v -> onPlusClick());

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                select(position);
            }
        });

        select(0);
    }

    /** 右上角的加号：不同 Tab 有不同行为 */
    /** Android 13+ 需要运行时申请通知权限，否则系统通知弹不出来 */
    private void requestNotifyPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return;
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
    }

    /**
     * 酷安那种"液态果冻"式切换：
     *   ① 指示器：横向滑过去的同时被"拉长 → 回弹"（中间最扁最长，两头正常），
     *      像一坨有弹性的胶在移动，而不是方块平移
     *   ② 图标：跳起来 → 落地压扁 → 弹回（真实的弹跳落地感）
     *   ③ 文字：跟着轻微上浮 + 加粗
     * 全部由一条 ValueAnimator 按进度驱动，不做嵌套 withEndAction，
     * 所以不会出现"跳几下"的断续感。
     */
    private void animateTab(int index, boolean selected) {
        ImageView icon = icons[index];
        TextView text = texts[index];
        if (icon == null) return;

        int green = getResources().getColor(R.color.wx_nav_selected);
        int gray = getResources().getColor(R.color.wx_nav_normal);
        icon.setColorFilter(selected ? green : gray);
        if (text != null) {
            text.setTextColor(selected ? green : gray);
            text.setTypeface(null, selected ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
        if (!selected) {
            // 未选中：平滑回到静止态，不弹
            icon.animate().cancel();
            icon.animate().translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(180).start();
            if (text != null) {
                text.animate().cancel();
                text.animate().translationY(0f).setDuration(180).start();
            }
            return;
        }

        // ② 图标弹跳：跳起 → 落地压扁 → 回弹到 1.12
        final float jump = -11f * getResources().getDisplayMetrics().density;
        ValueAnimator bounce = ValueAnimator.ofFloat(0f, 1f);
        bounce.setDuration(460);
        bounce.setInterpolator(new LinearInterpolator());
        bounce.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            float ty;
            float sx, sy;
            if (t < 0.35f) {                    // 起跳：向上加速
                float k = t / 0.35f;
                ty = jump * k;
                sx = 1f + 0.10f * k;
                sy = 1f + 0.10f * k;
            } else if (t < 0.62f) {             // 下落
                float k = (t - 0.35f) / 0.27f;
                ty = jump * (1f - k);
                sx = 1.10f - 0.10f * k;
                sy = 1.10f - 0.10f * k;
            } else if (t < 0.80f) {             // 落地压扁
                float k = (t - 0.62f) / 0.18f;
                ty = 0f;
                sx = 1f + 0.26f * k;
                sy = 1f - 0.20f * k;
            } else {                            // 回弹稳定
                float k = (t - 0.80f) / 0.20f;
                ty = 0f;
                sx = 1.26f - 0.14f * k;
                sy = 0.80f + 0.32f * k;
            }
            icon.setTranslationY(ty);
            icon.setScaleX(sx);
            icon.setScaleY(sy);
        });
        bounce.start();

        // ③ 文字轻微上浮
        if (text != null) {
            text.animate().cancel();
            text.animate().translationY(-2f).setDuration(220).start();
        }
    }

    /**
     * 移动指示器：只用 Tab 相对胶囊内部的 getLeft()，不碰屏幕坐标。
     * 移动过程中做拉伸/挤压，形成液态果冻感。
     */
    private void moveIndicator(int index, boolean animate) {
        if (indicator == null || tabs[index] == null) return;
        View nav = findViewById(R.id.nav);
        indicator.setVisibility(View.VISIBLE);
        nav.post(() -> {
            View tab = tabs[index];
            float base = 10f * getResources().getDisplayMetrics().density;
            float target = tab.getLeft() + (tab.getWidth() - indicator.getWidth()) / 2f - base;
            float from = indicator.getTranslationX();
            if (!animate || Math.abs(target - from) < 0.5f) {
                indicator.setTranslationX(target);
                indicator.setScaleX(1f);
                indicator.setScaleY(1f);
                return;
            }
            // ① 一条动画同时驱动位移 + 拉伸挤压
            ValueAnimator va = ValueAnimator.ofFloat(from, target);
            va.setDuration(420);
            va.setInterpolator(new DecelerateInterpolator(1.5f));
            va.addUpdateListener(an -> {
                float t = an.getAnimatedFraction();
                // 位移
                indicator.setTranslationX(from + (target - from) * t);
                // 液态：中途被拉长压扁，两头恢复正常
                float s = (float) Math.sin(Math.PI * t);
                indicator.setScaleX(1f + 0.42f * s);
                indicator.setScaleY(1f - 0.16f * s);
            });
            va.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    indicator.setScaleX(1f);
                    indicator.setScaleY(1f);
                }
            });
            va.start();
        });
    }

    private void onPlusClick() {
        switch (current) {
            case 0: // 微信：发起相关操作
                showPlusMenu(new String[]{"添加 AI 好友", "发朋友圈"},
                        new Runnable[]{this::addAi, this::postMoment});
                break;
            case 1: // AI 好友：添加
                addAi();
                break;
            case 2: // 发现：发朋友圈
                postMoment();
                break;
            default: // 我：无操作入口
                break;
        }
    }

    private void showPlusMenu(String[] labels, Runnable[] actions) {
        new android.app.AlertDialog.Builder(this)
                .setItems(labels, (dialog, which) -> actions[which].run())
                .show();
    }

    private void addAi() {
        startActivity(new Intent(this, AiEditActivity.class));
    }

    private void postMoment() {
        startActivity(new Intent(this, PostMomentActivity.class));
    }

    private void select(int index) {
        if (index == current) {
            // 同一个 Tab 重复触发时不再重放动画，避免出现"乱跳"
            return;
        }
        current = index;
        pager.setCurrentItem(index, false);
        for (int i = 0; i < 4; i++) {
            animateTab(i, i == index);
        }
        moveIndicator(index, true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Fragment f = getSupportFragmentManager().findFragmentByTag("f" + current);
        if (f instanceof ChatListFragment) ((ChatListFragment) f).refresh();
        else if (f instanceof ContactsFragment) ((ContactsFragment) f).refresh();
        else if (f instanceof MeFragment) ((MeFragment) f).refresh();
    }

    static class MainAdapter extends FragmentStateAdapter {

        MainAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new ContactsFragment();
                case 2:
                    return new DiscoverFragment();
                case 3:
                    return new MeFragment();
                default:
                    return new ChatListFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
