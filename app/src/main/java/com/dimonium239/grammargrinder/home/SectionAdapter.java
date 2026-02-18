package com.dimonium239.grammargrinder.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimonium239.grammargrinder.R;

import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.VH> {

    public interface OnSectionClick {
        void onClick(SectionMeta section);
    }

    private final List<SectionMeta> items;
    private final OnSectionClick listener;

    public SectionAdapter(List<SectionMeta> items, OnSectionClick listener) {
        this.items = items;
        this.listener = listener;
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;

        public VH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            subtitle = v.findViewById(R.id.tv_subtitle);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        SectionMeta s = items.get(position);
        holder.title.setText(s.title);
        holder.subtitle.setText(s.subtitle);
        holder.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
