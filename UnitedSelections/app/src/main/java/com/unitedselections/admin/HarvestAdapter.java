package com.unitedselections.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import java.util.List;

public class HarvestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Object> items;

    public HarvestAdapter(List<Object> list){ items=list; }

    @Override
    public int getItemViewType(int position){
        return items.get(position) instanceof FieldHarvestReportActivity.GreenhouseItem ? 0 : 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        if(viewType==0){
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_greenhouse_card,parent,false);
            return new GreenhouseHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_variety_card,parent,false);
            return new VarietyHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder,int position){
        if(holder instanceof GreenhouseHolder){
            FieldHarvestReportActivity.GreenhouseItem gh =
                    (FieldHarvestReportActivity.GreenhouseItem) items.get(position);
            ((GreenhouseHolder)holder).txtGreenhouse.setText(gh.greenhouseName + " (" + gh.totalStems + ")");
        } else {
            FieldHarvestReportActivity.VarietyItem v =
                    (FieldHarvestReportActivity.VarietyItem) items.get(position);
            ((VarietyHolder)holder).txtVariety.setText(v.varietyName);
            ((VarietyHolder)holder).txtStems.setText("Stems: " + v.stems);
        }
    }

    @Override
    public int getItemCount(){ return items.size(); }

    static class GreenhouseHolder extends RecyclerView.ViewHolder{
        TextView txtGreenhouse;
        GreenhouseHolder(View v){ super(v); txtGreenhouse=v.findViewById(R.id.txtGreenhouse);}
    }

    static class VarietyHolder extends RecyclerView.ViewHolder{
        TextView txtVariety, txtStems;
        VarietyHolder(View v){
            super(v);
            txtVariety=v.findViewById(R.id.txtVariety);
            txtStems=v.findViewById(R.id.txtStems);
        }
    }
}