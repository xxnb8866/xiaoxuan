package com.wechat.ai.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.ui.contacts.AiEditActivity;
import com.wechat.ai.util.TimeUtils;
import com.wechat.ai.widget.AvatarIcons;
import com.wechat.ai.widget.AvatarView;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信 Tab：聊天列表。支持顶部搜索、空状态引导、长按管理。
 */
public class ChatListFragment extends Fragment {

    private RecyclerView recycler;
    private SwipeRefreshLayout refresh;
    private LinearLayout empty;
    private EditText etSearch;
    private ImageView btnClear;

    private final List<Conversation> all = new ArrayList<>();
    private final List<Conversation> shown = new ArrayList<>();
    private Adapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat_list, container, false);
        recycler = v.findViewById(R.id.recycler);
        refresh = v.findViewById(R.id.refresh);
        empty = v.findViewById(R.id.empty);
        etSearch = v.findViewById(R.id.et_search);
        btnClear = v.findViewById(R.id.btn_clear);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        refresh.setOnRefreshListener(() -> {
            refresh();
            refresh.setRefreshing(false);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        btnClear.setOnClickListener(v1 -> etSearch.setText(""));

        v.findViewById(R.id.btn_add_first).setOnClickListener(v1 ->
                startActivity(new Intent(getActivity(), AiEditActivity.class)));

        refresh();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        if (!isAdded()) return;
        all.clear();
        all.addAll(App.dbHelper().getConversations());
        applyFilter(etSearch == null ? "" : etSearch.getText().toString());
    }

    private void applyFilter(String keyword) {
        shown.clear();
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        for (Conversation c : all) {
            String name = c.name == null ? "" : c.name.toLowerCase();
            String last = c.lastMsg == null ? "" : c.lastMsg.toLowerCase();
            if (k.isEmpty() || name.contains(k) || last.contains(k)) shown.add(c);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        boolean isEmpty = shown.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        // 搜索无结果时不显示"去添加"按钮
        View btn = empty.findViewById(R.id.btn_add_first);
        if (btn != null) btn.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conversation, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Conversation c = shown.get(position);
            h.avatar.bind(c.avatarPath, AvatarIcons.resOf(c.avatarIcon), c.firstChar(), c.avatarColor);
            h.name.setText(c.name);
            h.last.setText(c.lastMsg == null ? "" : c.lastMsg);
            h.time.setText(TimeUtils.listTime(c.lastTs));

            h.unread.setVisibility(c.unread > 0 ? View.VISIBLE : View.GONE);
            if (c.unread > 0) h.unread.setText(String.valueOf(c.unread));
            h.mute.setVisibility(c.mute ? View.VISIBLE : View.GONE);

            // 最后一项不画分割线
            h.divider.setVisibility(position == shown.size() - 1 ? View.GONE : View.VISIBLE);

            h.itemView.setOnClickListener(v -> openChat(c));
            h.itemView.setOnLongClickListener(v -> {
                showMenu(c);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return shown.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            AvatarView avatar;
            TextView name;
            TextView last;
            TextView time;
            TextView unread;
            ImageView mute;
            View divider;

            Holder(View v) {
                super(v);
                avatar = v.findViewById(R.id.avatar);
                name = v.findViewById(R.id.name);
                last = v.findViewById(R.id.last);
                time = v.findViewById(R.id.time);
                unread = v.findViewById(R.id.unread);
                mute = v.findViewById(R.id.mute);
                divider = v.findViewById(R.id.divider);
            }
        }
    }

    private void openChat(Conversation c) {
        if (getActivity() == null) return;
        App.dbHelper().markRead(c.id);
        Intent i = new Intent(getActivity(), ChatActivity.class);
        i.putExtra(ChatActivity.EXTRA_CONV_ID, c.id);
        startActivity(i);
        c.unread = 0;
    }

    private void showMenu(Conversation c) {
        if (getActivity() == null) return;
        String[] items = {
                c.sticky ? "取消置顶" : "置顶该聊天",
                c.mute ? "取消免打扰" : "消息免打扰",
                "编辑资料",
                "清空聊天记录",
                "删除该 AI 好友"
        };
        new AlertDialog.Builder(getActivity())
                .setTitle(c.name)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            App.dbHelper().setSticky(c.id, !c.sticky);
                            break;
                        case 1:
                            App.dbHelper().setMute(c.id, !c.mute);
                            break;
                        case 2:
                            startActivity(new Intent(getActivity(), AiEditActivity.class)
                                    .putExtra(AiEditActivity.EXTRA_CONV_ID, c.id));
                            return;
                        case 3:
                            App.dbHelper().clearConversation(c.id);
                            break;
                        case 4:
                            App.dbHelper().deleteConversation(c.id);
                            break;
                    }
                    refresh();
                })
                .show();
    }
}
