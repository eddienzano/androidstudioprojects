package com.aaagrowers.theumribungoma.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.aaagrowers.theumribungoma.R;

import java.util.List;

public class FixtureAdapter extends RecyclerView.Adapter<FixtureAdapter.VH> {

    List<Fixture> list;
    Context context;

    public FixtureAdapter(Context c, List<Fixture> l) {
        context = c;
        list = l;
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView image;

        public VH(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            image = v.findViewById(R.id.image);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_fixture, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int pos) {
        Fixture f = list.get(pos);

        h.title.setText(f.title);

        if (f.file_type.equals("image")) {
            h.image.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(f.file_url)
                    .into(h.image);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}