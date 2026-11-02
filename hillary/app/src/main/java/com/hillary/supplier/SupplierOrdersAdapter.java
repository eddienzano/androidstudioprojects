package com.hillary.supplier;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.network.Constants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupplierOrdersAdapter extends RecyclerView.Adapter<SupplierOrdersAdapter.ViewHolder> {

    Context context;
    ArrayList<SupplierOrderModel> orderList;

    public SupplierOrdersAdapter(Context context, ArrayList<SupplierOrderModel> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_supplier_order, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SupplierOrderModel model = orderList.get(position);

        holder.tvOrderId.setText("Order #" + model.getId());
        holder.tvCustomer.setText(model.getCustomerName());
        holder.tvStatus.setText(model.getStatus());
        holder.tvTotal.setText("KES " + model.getTotalAmount());

        // ================= CLICK TO VIEW DETAILS =================
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SupplierOrderDetailsActivity.class);
            intent.putExtra("order_id", model.getId());
            context.startActivity(intent);
        });

        holder.btnAction.setOnClickListener(v -> {

            String nextStatus = getNextStatus(model.getStatus());

            if (nextStatus == null) {
                Toast.makeText(context, "Order already completed", Toast.LENGTH_SHORT).show();
                return;
            }

            updateStatus(model.getId(), nextStatus);
        });

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(context, SupplierOrderDetailsActivity.class);
            intent.putExtra("order_id", model.getId());
            context.startActivity(intent);
        });
    }

    private String getNextStatus(String status) {

        if (status == null) return "approved";

        switch (status) {
            case "pending": return "approved";
            case "approved": return "dispatched";
            case "dispatched": return "completed";
            default: return null;
        }
    }

    private void updateStatus(int orderId, String status) {

        try {

            JSONObject json = new JSONObject();
            json.put("order_id", orderId);
            json.put("status", status);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(Constants.UPDATE_ORDER_STATUS_URL)
                    .post(body)
                    .build();

            OkHttpClient client = ApiClient.getPublicClient();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) {

                    ((SupplierOrdersActivity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Order updated", Toast.LENGTH_SHORT).show()
                    );
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvOrderId, tvCustomer, tvStatus, tvTotal;
        MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvCustomer = itemView.findViewById(R.id.tvCustomer);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}