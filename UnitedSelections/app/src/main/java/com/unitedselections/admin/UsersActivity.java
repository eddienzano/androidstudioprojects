package com.unitedselections.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.unitedselections.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class UsersActivity extends AppCompatActivity {

    RecyclerView recyclerUsers;
    ArrayList<UserModel> users = new ArrayList<>();
    UserAdapter adapter;

    String API_URL = "https://unitedselections.supplybridge.co.ke/api/get_users.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        recyclerUsers = findViewById(R.id.recyclerUsers);

        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UserAdapter(this, users);
        recyclerUsers.setAdapter(adapter);

        ImageView btnAdd = findViewById(R.id.btnAddUser);

        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateUserActivity.class));
        });

        loadUsers();
    }

    private void loadUsers(){

        new Thread(() -> {

            try {

                URL url = new URL(API_URL);

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(conn.getInputStream())
                        );

                StringBuilder sb = new StringBuilder();

                String line;

                while((line = reader.readLine()) != null){
                    sb.append(line);
                }

                JSONArray arr = new JSONArray(sb.toString());

                users.clear();

                for(int i=0;i<arr.length();i++){

                    JSONObject obj = arr.getJSONObject(i);

                    UserModel u = new UserModel();

                    u.id = obj.getString("id");
                    u.username = obj.getString("username");
                    u.role = obj.getString("role");
                    u.status = obj.getString("status");

                    users.add(u);
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e){
                e.printStackTrace();
            }

        }).start();
    }
    public void reloadUsers(){
        loadUsers(); // your API call that reloads RecyclerView
    }
}