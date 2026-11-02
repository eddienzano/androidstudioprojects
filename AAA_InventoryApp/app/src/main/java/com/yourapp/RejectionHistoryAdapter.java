package com.yourapp;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Callback;
import okhttp3.Call;
import okhttp3.Response;

public class RejectionHistoryAdapter
        extends RecyclerView.Adapter<RejectionHistoryAdapter.VH> {

    private final List<RejectionHistoryItem> data;
    private final Context context;

    private final String BASE_URL =
            "https://www.aaagrowers.co.ke/inventory/rejections/api/";

    private final OkHttpClient client =
            new OkHttpClient();

    public RejectionHistoryAdapter(
            Context context,
            List<RejectionHistoryItem> data
    ) {
        this.context = context;
        this.data = data;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView vVariety;
        TextView vReason;
        TextView vDetails;
        TextView vTime;

        Button btnEdit;
        Button btnCancel;

        VH(View v) {
            super(v);

            vVariety = v.findViewById(R.id.txtVariety);
            vReason = v.findViewById(R.id.txtReason);
            vDetails = v.findViewById(R.id.txtDetails);
            vTime = v.findViewById(R.id.txtTime);

            btnEdit = v.findViewById(R.id.btnEdit);
            btnCancel = v.findViewById(R.id.btnCancel);
        }
    }

    @Override
    public VH onCreateViewHolder(
            ViewGroup parent,
            int viewType
    ) {
        return new VH(
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.row_rejection_history,
                                parent,
                                false
                        )
        );
    }

    @Override
    public void onBindViewHolder(
            VH h,
            int position
    ) {

        RejectionHistoryItem r =
                data.get(position);

        h.vVariety.setText(r.variety);

        h.vReason.setText(
                "Reason: " + r.reason
        );

        h.vDetails.setText(
                r.stems
                        + " stems | "
                        + r.length
                        + "cm | "
                        + r.greenhouse
        );

        h.vTime.setText(r.time);

        h.btnEdit.setOnClickListener(v ->
                showEditDialog(r, position));

        h.btnCancel.setOnClickListener(v ->
                cancelRecord(r, position));
    }

    private void showEditDialog(
            RejectionHistoryItem item,
            int position
    ) {

        EditText edt =
                new EditText(context);

        edt.setText(
                String.valueOf(item.stems)
        );

        new AlertDialog.Builder(context)
                .setTitle("Edit Stems")
                .setView(edt)

                .setPositiveButton(
                        "Save",
                        (dialog, which) -> {

                            int newStems =
                                    Integer.parseInt(
                                            edt.getText()
                                                    .toString()
                                    );

                            updateRejection(
                                    item,
                                    newStems,
                                    position
                            );
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }

    private void updateRejection(
            RejectionHistoryItem item,
            int newStems,
            int position
    ) {

        RequestBody body =
                new FormBody.Builder()
                        .add(
                                "id",
                                String.valueOf(item.id)
                        )
                        .add(
                                "stems",
                                String.valueOf(newStems)
                        )
                        .add(
                                "changed_by",
                                "Android User"
                        )
                        .build();

        Request request =
                new Request.Builder()
                        .url(
                                BASE_URL +
                                        "update_rejection.php"
                        )
                        .post(body)
                        .build();

        client.newCall(request)
                .enqueue(new Callback() {

                    @Override
                    public void onFailure(
                            Call call,
                            IOException e
                    ) {

                        ((android.app.Activity)context)
                                .runOnUiThread(() ->
                                        Toast.makeText(
                                                context,
                                                "Update failed",
                                                Toast.LENGTH_SHORT
                                        ).show());
                    }

                    @Override
                    public void onResponse(
                            Call call,
                            Response response
                    ) throws IOException {

                        try {

                            JSONObject obj =
                                    new JSONObject(
                                            response.body()
                                                    .string()
                                    );

                            if(obj.getBoolean("success")) {

                                item.stems =
                                        newStems;

                                ((android.app.Activity)context)
                                        .runOnUiThread(() -> {

                                            notifyItemChanged(
                                                    position
                                            );

                                            Toast.makeText(
                                                    context,
                                                    "Updated",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void cancelRecord(
            RejectionHistoryItem item,
            int position
    ) {

        new AlertDialog.Builder(context)

                .setTitle("Cancel Rejection")

                .setMessage(
                        "Remove this rejection?"
                )

                .setPositiveButton(
                        "Yes",
                        (dialog, which) -> {

                            RequestBody body =
                                    new FormBody.Builder()
                                            .add(
                                                    "id",
                                                    String.valueOf(
                                                            item.id
                                                    )
                                            )
                                            .add(
                                                    "changed_by",
                                                    "Android User"
                                            )
                                            .build();

                            Request request =
                                    new Request.Builder()
                                            .url(
                                                    BASE_URL +
                                                            "cancel_rejection.php"
                                            )
                                            .post(body)
                                            .build();

                            client.newCall(request)
                                    .enqueue(new Callback() {

                                        @Override
                                        public void onFailure(
                                                Call call,
                                                IOException e
                                        ) {

                                        }

                                        @Override
                                        public void onResponse(
                                                Call call,
                                                Response response
                                        ) {

                                            ((android.app.Activity)context)
                                                    .runOnUiThread(() -> {

                                                        data.remove(
                                                                position
                                                        );

                                                        notifyItemRemoved(
                                                                position
                                                        );

                                                        Toast.makeText(
                                                                context,
                                                                "Cancelled",
                                                                Toast.LENGTH_SHORT
                                                        ).show();
                                                    });
                                        }
                                    });
                        })

                .setNegativeButton(
                        "No",
                        null
                )

                .show();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}