package com.unitedselections.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyHolder> {

    Context ctx;
    ArrayList<UserModel> list;

    public UserAdapter(Context ctx, ArrayList<UserModel> list){
        this.ctx = ctx;
        this.list = list;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.user_item, parent, false);

        return new MyHolder(v);
    }

    @Override
    public void onBindViewHolder(MyHolder h, int i) {

        UserModel u = list.get(i);

        h.txtUser.setText(u.username);
        h.txtRole.setText(u.role);

        if(u.status.equals("1")){
            h.txtStatus.setText("ACTIVE");
            h.txtStatus.setTextColor(Color.GREEN);
        } else {
            h.txtStatus.setText("DISABLED");
            h.txtStatus.setTextColor(Color.RED);
        }

        h.btnEdit.setOnClickListener(v -> {

            Intent x = new Intent(ctx, EditUserActivity.class);

            x.putExtra("id", u.id);
            x.putExtra("username", u.username);
            x.putExtra("role", u.role);

            ctx.startActivity(x);

        });

        h.btnDisable.setOnClickListener(v -> {

            new AlertDialog.Builder(ctx)
                    .setTitle("Confirm")
                    .setMessage("Toggle user status?")
                    .setPositiveButton("Yes", (d,w)->toggleUser(u.id))
                    .show();

        });
    }

    private void toggleUser(String id){

        new Thread(() -> {

            try {

                URL url = new URL(
                        "https://unitedselections.supplybridge.co.ke/api/toggle_user.php"
                );

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String post =
                        "id=" + URLEncoder.encode(id,"UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(post.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                ((android.app.Activity) ctx).runOnUiThread(() -> {

                    if(responseCode == 200){

                        Toast.makeText(ctx,
                                "User Updated",
                                Toast.LENGTH_SHORT).show();

                        // refresh list
                        if(ctx instanceof UsersActivity){
                            ((UsersActivity) ctx).reloadUsers();
                        }

                    } else {

                        Toast.makeText(ctx,
                                "Failed",
                                Toast.LENGTH_SHORT).show();
                    }

                });

            } catch (Exception e){
                e.printStackTrace();

                ((android.app.Activity) ctx).runOnUiThread(() ->
                        Toast.makeText(ctx,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }

        }).start();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{

        TextView txtUser, txtRole, txtStatus;
        Button btnEdit, btnDisable;

        public MyHolder(View v){
            super(v);

            txtUser = v.findViewById(R.id.txtUser);
            txtRole = v.findViewById(R.id.txtRole);
            txtStatus = v.findViewById(R.id.txtStatus);

            btnEdit = v.findViewById(R.id.btnEdit);
            btnDisable = v.findViewById(R.id.btnDisable);
        }
    }
}