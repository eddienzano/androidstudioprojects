package com.hillary.transporter.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hillary.R;
import com.hillary.network.ApiClient;
import com.hillary.transporter.model.Delivery;
import com.hillary.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.VH> {

    Context context;
    ArrayList<Delivery> list;
    SessionManager session;

    public DeliveryAdapter(Context context, ArrayList<Delivery> list) {
        this.context = context;
        this.list = list;
        this.session = new SessionManager(context);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_delivery, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        Delivery d = list.get(position);

        h.tvOrderNo.setText("ORDER #" + d.order_number);
        h.tvAddress.setText(d.delivery_address);

        String status = d.order_status != null ? d.order_status : "";
        h.tvStatus.setText(status.replace("_", " ").toUpperCase());

        switch (status) {

            case "out_for_delivery":
                h.tvStatus.setBackgroundColor(0xFFF59E0B);
                break;

            case "delivered":
                h.tvStatus.setBackgroundColor(0xFF22C55E);
                break;

            case "ready":
                h.tvStatus.setBackgroundColor(0xFF3B82F6);
                break;

            default:
                h.tvStatus.setBackgroundColor(0xFF9CA3AF);
                break;
        }

        h.tvTime.setText("Assigned: " + d.assigned_at);

        // OPEN GOOGLE MAPS
        h.itemView.setOnClickListener(v -> {

            if (d.latitude != null && !d.latitude.isEmpty()) {

                String uri = "google.navigation:q=" +
                        d.latitude + "," + d.longitude;

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");

                context.startActivity(intent);
            }
        });

        // START DELIVERY BUTTON
        h.btnStartDelivery.setOnClickListener(v -> {

            startDelivery(d.id);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class VH extends RecyclerView.ViewHolder {

        TextView tvOrderNo, tvAddress, tvStatus, tvTime;
        Button btnStartDelivery;

        public VH(@NonNull View itemView) {
            super(itemView);

            tvOrderNo = itemView.findViewById(R.id.tvOrderNo);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnStartDelivery = itemView.findViewById(R.id.btnStartDelivery);
        }
    }

    private void startDelivery(String orderId) {

        OkHttpClient client = ApiClient.getPublicClient();

        String transporterId = String.valueOf(session.getUserId()); // IMPORTANT FIX

        RequestBody body = new FormBody.Builder()
                .add("order_id", orderId)
                .add("transporter_id", transporterId)
                .add("status", "out_for_delivery")
                .build();

        Request request = new Request.Builder()
                .url("https://www.supplybridge.co.ke/fillers/api/transporter/update_delivery_status.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context,
                                "Network error",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {

                String res = response.body().string();

                ((Activity) context).runOnUiThread(() -> {

                    try {

                        JSONObject obj = new JSONObject(res);

                        if (obj.optBoolean("success")) {

                            Toast.makeText(context,
                                    "Delivery Started",
                                    Toast.LENGTH_SHORT).show();

                            // optionally refresh list here

                        } else {

                            Toast.makeText(context,
                                    obj.optString("message"),
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(context,
                                "Invalid response",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}