package com.aaagrowers.coldroom.stocks;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class VarietyDb {

    public static ArrayList<VarietyItem> searchVarieties(Context ctx, String term) {
        ArrayList<VarietyItem> results = new ArrayList<>();

        VarietyDbHelper helper = new VarietyDbHelper(ctx);
        SQLiteDatabase db = helper.getReadableDatabase();

        // Remove farm_name filter
        String sql = "SELECT variety_id, variety_name FROM variety WHERE variety_name LIKE ?";
        Cursor c = db.rawQuery(sql, new String[]{"%" + term + "%"});

        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            results.add(new VarietyItem(id, name));
        }

        c.close();
        db.close();

        return results;
    }

    public static class VarietyItem {
        public int id;
        public String name;
        public VarietyItem(int id, String name) {
            this.id = id;
            this.name = name;
        }


        @Override
        public String toString() {
            return name;
        }
    }
}