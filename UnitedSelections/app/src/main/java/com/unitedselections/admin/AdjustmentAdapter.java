package com.unitedselections.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONObject;

import java.util.List;

public class AdjustmentAdapter
        extends RecyclerView.Adapter<AdjustmentAdapter.MyHolder> {

    Context context;
    List<JSONObject> list;

    public AdjustmentAdapter(Context context,
                             List<JSONObject> list){

        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                       int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(
                        R.layout.row_adjustment,
                        parent,
                        false
                );

        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder h,
                                 int position) {

        JSONObject obj = list.get(position);

        try{

            h.variety.setText(
                    obj.getString("variety")
            );

            h.qty.setText(
                    "Qty: " + obj.getInt("quantity")
            );

            h.btnEdit.setOnClickListener(v -> {

                EditText input = new EditText(context);

                input.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                );

                try{
                    input.setText(
                            String.valueOf(
                                    obj.getInt("quantity")
                            )
                    );
                }catch (Exception ignored){}

                new AlertDialog.Builder(context)
                        .setTitle("Edit Quantity")
                        .setView(input)

                        .setPositiveButton("Save",
                                (dialog, which) -> {

                                    try{

                                        int q = Integer.parseInt(
                                                input.getText()
                                                        .toString()
                                        );

                                        obj.put("quantity", q);

                                        notifyItemChanged(position);

                                    }catch (Exception ignored){}
                                })

                        .setNegativeButton("Cancel", null)
                        .show();
            });

        }catch (Exception ignored){}
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder{

        TextView variety, qty;

        ImageButton btnEdit;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            variety = itemView.findViewById(R.id.variety);
            qty = itemView.findViewById(R.id.qty);

            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}