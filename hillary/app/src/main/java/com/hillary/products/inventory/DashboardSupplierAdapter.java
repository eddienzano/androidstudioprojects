package com.hillary.products.inventory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;

import java.util.List;

public class DashboardSupplierAdapter
        extends RecyclerView.Adapter<DashboardSupplierAdapter.MyHolder> {

    Context context;
    List<DashboardSupplier> list;

    public DashboardSupplierAdapter(Context context,
                                    List<DashboardSupplier> list) {

        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                       int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_dashboard_supplier,
                        parent,
                        false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder h,
                                 int position) {

        DashboardSupplier s = list.get(position);

        h.txtSupplier.setText(s.getSupplier_name());

        h.txtProducts.setText(
                "Products: " + s.getTotal_products()
        );

        h.txtStock.setText(
                "Stock: " + s.getTotal_stock()
        );
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyHolder
            extends RecyclerView.ViewHolder {

        TextView txtSupplier,
                txtProducts,
                txtStock;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            txtSupplier =
                    itemView.findViewById(R.id.txtSupplierName);

            txtProducts =
                    itemView.findViewById(R.id.txtSupplierProducts);

            txtStock =
                    itemView.findViewById(R.id.txtSupplierStock);
        }
    }
}