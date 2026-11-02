package com.yourapp.gradedstock;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.yourapp.R;

import java.util.ArrayList;

public class GradedQrAdapter
        extends RecyclerView.Adapter<GradedQrAdapter.ViewHolder> {

    private final ArrayList<QrItem> list;
    private final OnBucketRemovedListener listener;

    public interface OnBucketRemovedListener{
        void onBucketRemoved(int newCount);
    }

    public GradedQrAdapter(
            ArrayList<QrItem> list,
            OnBucketRemovedListener listener
    ){
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ){

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_graded_bucket,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ){

        QrItem item = list.get(position);

        holder.txtBucket.setText(item.bucket_name);

        holder.txtLength.setText(
                item.length + " cm"
        );

        if(holder.bunchWatcher != null)
            holder.etBunches.removeTextChangedListener(
                    holder.bunchWatcher
            );

        if(holder.stemWatcher != null)
            holder.etStems.removeTextChangedListener(
                    holder.stemWatcher
            );

        holder.etBunches.setText(
                String.valueOf(item.bunches)
        );

        holder.etStems.setText(
                String.valueOf(item.stems)
        );

        holder.bunchWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ){}

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ){}

            @Override
            public void afterTextChanged(
                    Editable s
            ){

                try{

                    item.bunches =
                            Integer.parseInt(
                                    s.toString()
                            );

                }catch(Exception e){

                    item.bunches = 0;
                }
            }
        };

        holder.stemWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ){}

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ){}

            @Override
            public void afterTextChanged(
                    Editable s
            ){

                try{

                    item.stems =
                            Integer.parseInt(
                                    s.toString()
                            );

                }catch(Exception e){

                    item.stems = 0;
                }
            }
        };

        holder.etBunches.addTextChangedListener(
                holder.bunchWatcher
        );

        holder.etStems.addTextChangedListener(
                holder.stemWatcher
        );

        holder.btnDelete.setOnClickListener(v -> {

            int pos =
                    holder.getAdapterPosition();

            if(pos == RecyclerView.NO_POSITION)
                return;

            list.remove(pos);

            notifyItemRemoved(pos);

            if(listener != null){

                listener.onBucketRemoved(
                        list.size()
                );
            }
        });
    }

    @Override
    public int getItemCount(){
        return list.size();
    }

    public ArrayList<QrItem> getItems(){
        return list;
    }

    static class ViewHolder
            extends RecyclerView.ViewHolder {

        CardView cardRoot;

        TextView txtBucket;
        TextView txtLength;

        EditText etBunches;
        EditText etStems;

        ImageButton btnDelete;

        TextWatcher bunchWatcher;
        TextWatcher stemWatcher;

        ViewHolder(@NonNull View itemView){

            super(itemView);

            cardRoot =
                    itemView.findViewById(R.id.cardRoot);

            txtBucket =
                    itemView.findViewById(R.id.txtBucket);

            txtLength =
                    itemView.findViewById(R.id.txtLength);

            etBunches =
                    itemView.findViewById(R.id.etBunches);

            etStems =
                    itemView.findViewById(R.id.etStems);

            btnDelete =
                    itemView.findViewById(R.id.btnDelete);
        }
    }
}