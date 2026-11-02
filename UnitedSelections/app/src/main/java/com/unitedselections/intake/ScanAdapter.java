package com.unitedselections.intake;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import java.util.List;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ViewHolder> {

    List<ScanItem> list;

    public ScanAdapter(List<ScanItem> l){
        list = l;
    }

    public List<ScanItem> getItems(){
        return list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_scan_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int i) {

        ScanItem item = list.get(i);

        h.variety.setText(item.variety);
        h.balance.setText("Balance: " + item.maxQty);

        h.qty.setText("");

        h.qty.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(Editable s){}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                try {
                    item.enteredQty = Integer.parseInt(s.toString());
                } catch (Exception e){
                    item.enteredQty = 0;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView variety, balance;
        EditText qty;

        public ViewHolder(View v) {
            super(v);
            variety = v.findViewById(R.id.tvVariety);
            balance = v.findViewById(R.id.tvBalance);
            qty = v.findViewById(R.id.etQty);
        }
    }
}