package com.hillary.products.inventory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hillary.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InventoryAdapter
        extends RecyclerView.Adapter<InventoryAdapter.MyHolder>
        implements Filterable {

    Context context;

    List<ProductInventory> list;
    List<ProductInventory> filteredList;

    String token;
    String BASE_URL;

    public InventoryAdapter(Context context,
                            List<ProductInventory> list,
                            String token,
                            String BASE_URL) {

        this.context = context;

        this.list = list;

        this.filteredList = new ArrayList<>(list);

        this.token = token;

        this.BASE_URL = BASE_URL;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View v = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_inventory,
                        parent,
                        false
                );

        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MyHolder h,
            int position) {

        ProductInventory p = filteredList.get(position);

        String productName =
                String.valueOf(p.getProduct_name());

        String supplier =
                String.valueOf(p.getSupplier_name());

        String category =
                String.valueOf(p.getCategory());

        String sku =
                String.valueOf(p.getSku());

        String priceStr =
                String.valueOf(p.getPrice());

        String stockStr =
                String.valueOf(p.getStock());

        h.txtName.setText(productName);

        h.txtSupplier.setText(supplier);

        h.txtCategory.setText(category);

        h.txtSku.setText("SKU: " + sku);

        h.txtPrice.setText("KES " + priceStr);

        /* SAFE VALUE CALCULATION */

        double inventoryValue = 0;

        try {

            double stock =
                    Double.parseDouble(
                            stockStr.trim()
                    );

            double price =
                    Double.parseDouble(
                            priceStr.trim()
                    );

            inventoryValue = stock * price;

        } catch (Exception ignored) {
        }

        h.txtValue.setText(
                "Value: KES "
                        + String.format(
                        "%.2f",
                        inventoryValue
                )
        );

        /* SAFE STOCK PARSING */

        double stock = 0;

        try {

            stock = Double.parseDouble(
                    stockStr.trim()
            );

        } catch (Exception ignored) {
        }

        h.txtStock.setText(
                "Stock: " + stock
        );

        /* STOCK COLORS */

        if(stock <= 0){

            h.txtStock.setBackgroundColor(
                    Color.RED
            );

        }
        else if(stock < 10){

            h.txtStock.setBackgroundColor(
                    Color.parseColor("#FF9800")
            );

        }
        else{

            h.txtStock.setBackgroundColor(
                    Color.parseColor("#4CAF50")
            );
        }

        /* IMAGE */

        Glide.with(context)
                .load(BASE_URL + p.getThumbnail())
                .placeholder(
                        R.drawable.ic_launcher_background
                )
                .error(
                        R.drawable.ic_launcher_background
                )
                .into(h.imgProduct);

        /* OPEN DETAILS */

        h.cardView.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            context,
                            InventoryDetailsActivity.class
                    );

            intent.putExtra(
                    "product_id",
                    p.getId()
            );

            context.startActivity(intent);
        });

        /* STOCK IN */

        h.btnStockIn.setOnClickListener(v -> {

            showStockDialog(p, true);
        });

        /* STOCK OUT */

        h.btnStockOut.setOnClickListener(v -> {

            showStockDialog(p, false);
        });
    }

    private void showStockDialog(
            ProductInventory product,
            boolean isStockIn){

        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);

        builder.setTitle(
                isStockIn
                        ? "Stock In"
                        : "Stock Out"
        );

        LinearLayout layout =
                new LinearLayout(context);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                20,
                40,
                20
        );

        EditText qty =
                new EditText(context);

        qty.setHint("Quantity");

        qty.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        EditText notes =
                new EditText(context);

        notes.setHint("Notes");

        layout.addView(qty);

        layout.addView(notes);

        builder.setView(layout);

        builder.setPositiveButton(
                "SAVE",
                (d,w)->{

                    String quantity =
                            qty.getText().toString().trim();

                    if(quantity.isEmpty()){

                        Toast.makeText(
                                context,
                                "Enter quantity",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    sendStock(
                            product.getId(),
                            quantity,
                            notes.getText().toString(),
                            isStockIn
                    );
                });

        builder.setNegativeButton(
                "CANCEL",
                null
        );

        builder.show();
    }

    private void sendStock(
            String productId,
            String quantity,
            String notes,
            boolean isStockIn){

        new Thread(() -> {

            try{

                String endpoint;

                if(isStockIn){

                    endpoint =
                            "inventory/stock_in.php";

                }else{

                    endpoint =
                            "inventory/stock_out.php";
                }

                OkHttpClient client =
                        new OkHttpClient();

                RequestBody body =
                        new FormBody.Builder()
                                .add(
                                        "product_id",
                                        productId
                                )
                                .add(
                                        "quantity",
                                        quantity
                                )
                                .add(
                                        "notes",
                                        notes
                                )
                                .build();

                Request request =
                        new Request.Builder()
                                .url(BASE_URL + endpoint)
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .post(body)
                                .build();

                Response response =
                        client.newCall(request)
                                .execute();

                if(response.body() == null){
                    throw new Exception("Empty response");
                }

                String res =
                        response.body().string();

                JSONObject obj =
                        new JSONObject(res);

                ((android.app.Activity)context)
                        .runOnUiThread(() -> {

                            Toast.makeText(
                                    context,
                                    obj.optString(
                                            "message"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        });

            }catch(Exception e){

                ((android.app.Activity)context)
                        .runOnUiThread(() -> {

                            Toast.makeText(
                                    context,
                                    e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            }

        }).start();
    }

    @Override
    public int getItemCount() {

        return filteredList.size();
    }

    @Override
    public Filter getFilter() {

        return new Filter() {

            @Override
            protected FilterResults performFiltering(
                    CharSequence constraint) {

                String search =
                        constraint.toString()
                                .toLowerCase()
                                .trim();

                if(search.isEmpty()){

                    filteredList =
                            new ArrayList<>(list);

                }else{

                    List<ProductInventory> temp =
                            new ArrayList<>();

                    for(ProductInventory p : list){

                        String product =
                                String.valueOf(
                                        p.getProduct_name()
                                ).toLowerCase();

                        String supplier =
                                String.valueOf(
                                        p.getSupplier_name()
                                ).toLowerCase();

                        String category =
                                String.valueOf(
                                        p.getCategory()
                                ).toLowerCase();

                        if(product.contains(search)

                                ||

                                supplier.contains(search)

                                ||

                                category.contains(search)
                        ){

                            temp.add(p);
                        }
                    }

                    filteredList = temp;
                }

                FilterResults results =
                        new FilterResults();

                results.values =
                        filteredList;

                return results;
            }

            @Override
            protected void publishResults(
                    CharSequence constraint,
                    FilterResults results) {

                filteredList =
                        (List<ProductInventory>)
                                results.values;

                notifyDataSetChanged();
            }
        };
    }

    public static class MyHolder
            extends RecyclerView.ViewHolder {

        CardView cardView;

        ImageView imgProduct;

        TextView txtName,
                txtSupplier,
                txtCategory,
                txtSku,
                txtPrice,
                txtValue,
                txtStock;

        Button btnStockIn,
                btnStockOut;

        public MyHolder(
                @NonNull View itemView) {

            super(itemView);

            cardView =
                    itemView.findViewById(
                            R.id.cardView
                    );

            imgProduct =
                    itemView.findViewById(
                            R.id.imgProduct
                    );

            txtName =
                    itemView.findViewById(
                            R.id.txtName
                    );

            txtSupplier =
                    itemView.findViewById(
                            R.id.txtSupplier
                    );

            txtCategory =
                    itemView.findViewById(
                            R.id.txtCategory
                    );

            txtSku =
                    itemView.findViewById(
                            R.id.txtSku
                    );

            txtPrice =
                    itemView.findViewById(
                            R.id.txtPrice
                    );

            txtValue =
                    itemView.findViewById(
                            R.id.txtValue
                    );

            txtStock =
                    itemView.findViewById(
                            R.id.txtStock
                    );

            btnStockIn =
                    itemView.findViewById(
                            R.id.btnStockIn
                    );

            btnStockOut =
                    itemView.findViewById(
                            R.id.btnStockOut
                    );
        }
    }
    public void updateList(List<ProductInventory> newList){

        list.clear();
        list.addAll(newList);

        filteredList.clear();
        filteredList.addAll(newList);

        notifyDataSetChanged();
    }
}