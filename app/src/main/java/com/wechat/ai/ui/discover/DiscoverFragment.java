package com.wechat.ai.ui.discover;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.ui.moments.MomentsActivity;

/**
 * 发现 Tab：朋友圈 / 扫一扫 / 摇一摇 ...
 * 朋友圈入口会在有新动态时显示红点。
 */
public class DiscoverFragment extends Fragment {

    private static final int[] ICONS = {
            R.drawable.ic_moments_new
    };
    private static final String[] TITLES = {
            "朋友圈"
    };
    private static final int[] COLORS = {
            0xFF45C01A
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_discover, container, false);
        RecyclerView recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(new RecyclerView.Adapter<Holder>() {
            @NonNull
            @Override
            public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View item = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_discover, parent, false);
                return new Holder(item);
            }

            @Override
            public void onBindViewHolder(@NonNull Holder h, int position) {
                h.icon.setImageResource(ICONS[position]);
                h.icon.setColorFilter(0xFFFFFFFF);
                h.title.setText(TITLES[position]);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(COLORS[position]);
                gd.setCornerRadius(6 * getResources().getDisplayMetrics().density);
                h.box.setBackground(gd);

                // 朋友圈有内容时显示提示红点
                boolean hasMoments = position == 0 && !App.dbHelper().allMoments().isEmpty();
                h.badge.setVisibility(hasMoments ? View.VISIBLE : View.GONE);
                h.divider.setVisibility(position == TITLES.length - 1 ? View.GONE : View.VISIBLE);

                h.itemView.setOnClickListener(view -> onItemClick(position));
            }

            @Override
            public int getItemCount() {
                return TITLES.length;
            }
        });
        return v;
    }

    private void onItemClick(int position) {
        switch (position) {
            case 0:
                startActivity(new Intent(getActivity(), MomentsActivity.class));
                break;
            case 1:
                startActivity(new Intent(getActivity(), ScanActivity.class));
                break;
            default:
                Toast.makeText(getActivity(), TITLES[position] + "（界面模拟）", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        View box;
        View badge;
        View divider;

        Holder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            title = v.findViewById(R.id.title);
            box = v.findViewById(R.id.icon_box);
            badge = v.findViewById(R.id.badge_box);
            divider = v.findViewById(R.id.divider);
        }
    }
}
