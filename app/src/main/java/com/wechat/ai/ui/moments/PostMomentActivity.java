package com.wechat.ai.ui.moments;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Moment;
import com.wechat.ai.util.ImageUtils;
import com.wechat.ai.util.Prefs;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * 发表朋友圈动态。
 */
public class PostMomentActivity extends AppCompatActivity {

    private final List<String> paths = new ArrayList<>();

    private final ActivityResultLauncher<String> pick =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                if (paths.size() >= 9) {
                    Toast.makeText(this, "最多 9 张", Toast.LENGTH_SHORT).show();
                    return;
                }
                String p = ImageUtils.copyToPrivate(this, Uri.parse(uri.toString()));
                if (p != null) {
                    paths.add(p);
                    refreshImages();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_moment);

        RecyclerView images = findViewById(R.id.images);
        images.setLayoutManager(new GridLayoutManager(this, 3));
        images.setAdapter(new RecyclerView.Adapter<ImgHolder>() {
            @Override
            public ImgHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                android.widget.ImageView iv = new android.widget.ImageView(PostMomentActivity.this);
                int size = (int) (88 * getResources().getDisplayMetrics().density);
                android.view.ViewGroup.LayoutParams lp =
                        new android.view.ViewGroup.LayoutParams(size, size);
                iv.setLayoutParams(lp);
                iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                iv.setPadding(4, 4, 4, 4);
                return new ImgHolder(iv);
            }

            @Override
            public void onBindViewHolder(ImgHolder holder, int position) {
                Glide.with(PostMomentActivity.this).load(paths.get(position)).into(holder.iv);
            }

            @Override
            public int getItemCount() {
                return paths.size();
            }
        });
        images.setNestedScrollingEnabled(false);

        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_image).setOnClickListener(v -> pick.launch("image/*"));
        findViewById(R.id.btn_post).setOnClickListener(v -> {
            String text = ((android.widget.EditText) findViewById(R.id.et_text))
                    .getText().toString().trim();
            Moment m = new Moment();
            m.author = Prefs.getNickname();
            m.avatarColor = 0xFF5B8FF9;
            m.text = text;
            m.ts = System.currentTimeMillis();
            if (!paths.isEmpty()) {
                try {
                    JSONArray arr = new JSONArray();
                    for (String p : paths) arr.put(p);
                    m.images = arr.toString();
                } catch (Exception ignore) {
                }
            }
            App.dbHelper().addMoment(m);
            Toast.makeText(this, "已发表", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void refreshImages() {
        RecyclerView images = findViewById(R.id.images);
        if (images.getAdapter() != null) images.getAdapter().notifyDataSetChanged();
    }

    static class ImgHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView iv;

        ImgHolder(android.view.View v) {
            super(v);
            iv = (android.widget.ImageView) v;
        }
    }
}
