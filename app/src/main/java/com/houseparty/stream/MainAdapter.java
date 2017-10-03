package com.houseparty.stream;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

class MainAdapter extends RecyclerView.Adapter {
    private final LinkedList<Item> list = new LinkedList<>();

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView fromNameView;
        private TextView toNameView;
        private TextView timestampView;

        ItemViewHolder(View view) {
            super(view);
            fromNameView = view.findViewById(R.id.fromName);
            toNameView = view.findViewById(R.id.toName);
            timestampView = view.findViewById(R.id.timestamp);
        }
    }

    void addToTopOfList(List<Item> batch) {
        for (Item item : batch) {
            list.push(item);
            if (list.size() >= 2000) list.removeLast();
        }
        this.notifyItemRangeChanged(0, batch.size());
    }

    void clearNonFriends() {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).areFriends) {
                list.remove(i);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemView = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_view, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < list.size()) {
            Item item = list.get(position);
            ((ItemViewHolder) holder).fromNameView.setText(item.from);
            ((ItemViewHolder) holder).toNameView.setText(item.to);
            ((ItemViewHolder) holder).timestampView.setText(
                    new SimpleDateFormat("MM/dd/yyyy HH:mm:SS", Locale.US)
                            .format(new Date(item.timestamp)));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
