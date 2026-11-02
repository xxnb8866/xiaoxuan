package com.wechat.ai.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wechat.ai.App;
import com.wechat.ai.R;
import com.wechat.ai.model.Conversation;
import com.wechat.ai.ui.chat.ChatActivity;
import com.wechat.ai.widget.AvatarIcons;
import com.wechat.ai.widget.AvatarView;

import java.util.ArrayList;
import java.util.List;

/**
 * 通讯录 Tab：这里就是用户自己添加的 AI 好友列表。
 */
public class ContactsFragment extends Fragment {

    private RecyclerView recycler;
    private LinearLayout empty;
    private TextView groupCount;
    private final List<Conversation> data = new ArrayList<>();
    private Adapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contacts, container, false);
        recycler = v.findViewById(R.id.recycler);
        empty = v.findViewById(R.id.empty);
        groupCount = v.findViewById(R.id.group_count);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Adapter();
        recycler.setAdapter(adapter);

        View addRow = v.findViewById(R.id.row_add);
        addRow.setOnClickListener(v1 ->
                startActivity(new Intent(getActivity(), AiEditActivity.class)));

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
        data.clear();
        data.addAll(App.dbHelper().getConversations());
        if (adapter != null) adapter.notifyDataSetChanged();
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        if (groupCount != null) groupCount.setText("我的 AI 好友 (" + data.size() + ")");
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Conversation c = data.get(position);
            h.avatar.bind(c.avatarPath, AvatarIcons.resOf(c.avatarIcon), c.firstChar(), c.avatarColor);
            h.name.setText(c.name);
            String sign = c.systemPrompt == null || c.systemPrompt.trim().isEmpty()
                    ? "还没有设置人设" : c.systemPrompt.trim();
            h.sign.setText(sign);
            h.divider.setVisibility(position == data.size() - 1 ? View.GONE : View.VISIBLE);

            h.itemView.setOnClickListener(v -> {
                if (getActivity() == null) return;
                Intent i = new Intent(getActivity(), ChatActivity.class);
                i.putExtra(ChatActivity.EXTRA_CONV_ID, c.id);
                startActivity(i);
            });
            h.itemView.setOnLongClickListener(v -> {
                if (getActivity() == null) return true;
                new AlertDialog.Builder(getActivity())
                        .setTitle(c.name)
                        .setItems(new String[]{"发消息", "编辑资料", "删除好友"}, (d, which) -> {
                            switch (which) {
                                case 0:
                                    startActivity(new Intent(getActivity(), ChatActivity.class)
                                            .putExtra(ChatActivity.EXTRA_CONV_ID, c.id));
                                    break;
                                case 1:
                                    startActivity(new Intent(getActivity(), AiEditActivity.class)
                                            .putExtra(AiEditActivity.EXTRA_CONV_ID, c.id));
                                    break;
                                case 2:
                                    new AlertDialog.Builder(getActivity())
                                            .setTitle("删除「" + c.name + "」？")
                                            .setNegativeButton("取消", null)
                                            .setPositiveButton("删除", (dd, ww) -> {
                                                App.dbHelper().deleteConversation(c.id);
                                                refresh();
                                            })
                                            .show();
                                    break;
                            }
                        })
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            AvatarView avatar;
            TextView name;
            TextView sign;
            View divider;

            Holder(View v) {
                super(v);
                avatar = v.findViewById(R.id.avatar);
                name = v.findViewById(R.id.name);
                sign = v.findViewById(R.id.sign);
                divider = v.findViewById(R.id.divider);
            }
        }
    }
}
