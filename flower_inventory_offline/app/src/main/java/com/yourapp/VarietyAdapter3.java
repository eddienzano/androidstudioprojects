package com.yourapp;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VarietyAdapter3 extends ArrayAdapter<String> {

    private final Context context;
    private List<String> names;
    private List<Integer> ids;

    public VarietyAdapter3(Context ctx, List<String> names, List<Integer> ids) {
        super(ctx, android.R.layout.simple_dropdown_item_1line, names);
        this.context = ctx;
        this.names = names;
        this.ids = ids;
    }

    public void updateData(List<String> newNames, List<Integer> newIds) {
        this.names = newNames;
        this.ids = newIds;
        clear();
        addAll(newNames);
        notifyDataSetChanged();
    }

    public int getIdAt(int position) {
        if (position < 0 || position >= ids.size()) return -1;
        return ids.get(position);
    }
}
