package com.aaagrowers.coldroom.stocks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;

import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.aaagrowers.coldroom.R;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;

public class StockScanActivity extends Activity {

    private static final long DOUBLE_SCAN_LOCK_MS = 2000;
    private static final long SCAN_THRESHOLD = 40;

    private EditText input;
    private Button btnCamera;
    private TextView tvRecent;

    private int totalCount = 0;
    private int validCount = 0;
    private int duplicateCount = 0;

    private TextView tvTotal, tvValid, tvDuplicate;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ToneGenerator tone =
            new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);

    private Vibrator vibrator;

    private long lastCharTime = 0;
    private StringBuilder scanBuffer = new StringBuilder();

    private String lastScannedValue = "";
    private long lastScannedAt = 0L;

    private final HashSet<String> scannedSet = new HashSet<>();

    private final Deque<String> recentDeque = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stock_scan);

        Button btnSend = findViewById(R.id.btnSendDb);

        btnSend.setOnClickListener(v ->
                StockRepository.sendAll(this)
        );

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        input = findViewById(R.id.inputScan);
        btnCamera = findViewById(R.id.btnCameraScan);

        tvRecent = new TextView(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 450));

        scroll.setPadding(0,20,0,0);

        tvRecent.setLineSpacing(6f,1.1f);

        scroll.addView(tvRecent);

        LinearLayout root = findViewById(R.id.rootContainer);
        root.addView(scroll);

        btnCamera.setOnClickListener(v -> {

            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan QR Code");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();

        });

        tvTotal = findViewById(R.id.tvTotal);
        tvValid = findViewById(R.id.tvValid);
        tvDuplicate = findViewById(R.id.tvDuplicate);

        Button btnReset = findViewById(R.id.btnReset);

        btnReset.setOnClickListener(v -> {
            totalCount = 0;
            validCount = 0;
            duplicateCount = 0;
            updateStats();
        });

        input.addTextChangedListener(new TextWatcher() {

            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}

            public void afterTextChanged(Editable s) {

                long now = System.currentTimeMillis();
                String text = s.toString();

                if(now-lastCharTime<SCAN_THRESHOLD)
                    scanBuffer.append(text);
                else{
                    scanBuffer.setLength(0);
                    scanBuffer.append(text);
                }

                lastCharTime=now;

                if(text.endsWith("\n")||text.endsWith("\r")){

                    String cleaned=text.trim();

                    input.setText("");
                    scanBuffer.setLength(0);

                    if(!cleaned.isEmpty())
                        submitIfAllowed(cleaned);

                    return;
                }

                if(scanBuffer.length()>=6){
                    mainHandler.removeCallbacks(bufferFinish);
                    mainHandler.postDelayed(bufferFinish,80);
                }

                if(text.length()>128){
                    input.setText("");
                    scanBuffer.setLength(0);
                }
            }
        });
    }

    private final Runnable bufferFinish = () -> {

        String s=scanBuffer.toString().trim();

        scanBuffer.setLength(0);
        input.setText("");

        if(!s.isEmpty())
            submitIfAllowed(s);
    };

    private void submitIfAllowed(String qr){
        boolean isDuplicate =
                scannedSet.contains(qr) ||
                        StockRepository.alreadyScannedPending(this, qr);

        scannedSet.add(qr);

        StockRepository.saveToPending(this, qr, isDuplicate);

        doScan(qr, isDuplicate);
    }



    private void addRecent(String qr,String status){

        runOnUiThread(()->{

            String line=qr+" — "+status;

            if(recentDeque.size()>=10)
                recentDeque.removeLast();

            recentDeque.addFirst(line);

            StringBuilder sb=new StringBuilder();

            for(String s:recentDeque)
                sb.append(s).append("\n\n");

            tvRecent.setText(sb.toString().trim());

        });
    }

    /* ===============================
       MAIN SCAN LOGIC
    =============================== */

    // ONLY showing changed parts to keep this readable
// (Everything else stays EXACTLY as you sent)

    private void doScan(String qrCode, boolean isDuplicate){

        totalCount++;

        // ALWAYS SAVE FIRST (your requirement ✅)
        StockRepository.saveOffline(this, qrCode, isDuplicate);

        if(isDuplicate){
            duplicateCount++;
            vibrate();
            addRecent(qrCode, "DUPLICATE ⚠");
            updateStats();
            return;
        }

        try{

            JSONObject inv =
                    StockInventoryRepository.getInventory(this,qrCode);

            if(inv==null){
                showFixDialog(qrCode, isDuplicate); // ✅ pass duplicate state
               // updateStats();
                return;
            }

            String variety=inv.getString("variety");
            String intakeTime=inv.getString("intake_time");

            long age=getAgeHours(intakeTime);

            if(age>120){
                showOldStockDialog(qrCode,variety,age, isDuplicate); // ✅ pass duplicate state
            }else{
//                validCount++;
//                addRecent(qrCode,variety+" ✓
                markSuccessfulScan(qrCode, variety + " ✓");
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        updateStats();
    }


    private long getAgeHours(String intake){

        try{

            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date d = sdf.parse(intake);

            long diff = System.currentTimeMillis()-d.getTime();

            return diff/(1000*60*60);

        }catch(Exception e){
            e.printStackTrace();
        }

        return 0;
    }

    private void showToast(String msg){

        mainHandler.post(()->{

            Toast t=Toast.makeText(this,msg,Toast.LENGTH_SHORT);

            t.setGravity(Gravity.CENTER,0,0);

            t.show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        IntentResult result =
                IntentIntegrator.parseActivityResult(requestCode,resultCode,data);

        if(result!=null){

            String content=result.getContents();

            if(content!=null)
                submitIfAllowed(content.trim());
            else
                showToast("Scan cancelled");

        }else{
            super.onActivityResult(requestCode,resultCode,data);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        tone.release();
    }

    private void showFixDialog(String qr, boolean isDuplicate) {

        runOnUiThread(() -> {

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 20, 40, 10);

            Spinner farmSpinner = new Spinner(this);
            String[] farms = {"Simba", "Chui", "Chestnut"};
            ArrayAdapter<String> farmAdapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, farms);
            farmSpinner.setAdapter(farmAdapter);

            AutoCompleteTextView inputVariety = new AutoCompleteTextView(this);
            inputVariety.setHint("Search Variety");

            EditText inputQty = new EditText(this);
            inputQty.setHint("Quantity");

            layout.addView(farmSpinner);
            layout.addView(inputVariety);
            layout.addView(inputQty);

            final int[] selectedVarietyId = {0};
            final String[] selectedFarm = {"Simba"};

            farmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedFarm[0] = farms[position];
                    selectedVarietyId[0] = 0;
                    inputVariety.setText("");
                }
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            inputVariety.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void onTextChanged(CharSequence s, int st, int b, int c) {}

                public void afterTextChanged(Editable s) {

                    if (s.length() < 2) return;

                    Executors.newSingleThreadExecutor().execute(() -> {

                        ArrayList<VarietyDb.VarietyItem> list =
                                VarietyDb.searchVarieties(StockScanActivity.this, s.toString());

                        runOnUiThread(() -> {
                            ArrayAdapter<VarietyDb.VarietyItem> adapter =
                                    new ArrayAdapter<>(StockScanActivity.this,
                                            android.R.layout.simple_dropdown_item_1line, list);

                            inputVariety.setAdapter(adapter);
                            inputVariety.showDropDown();

                            inputVariety.setOnItemClickListener((parent, view, position, id) -> {
                                VarietyDb.VarietyItem selected = adapter.getItem(position);
                                if (selected != null) {
                                    selectedVarietyId[0] = selected.id;
                                    inputVariety.setText(selected.name);
                                }
                            });
                        });
                    });
                }
            });

            new AlertDialog.Builder(this)
                    .setTitle("Unknown QR")
                    .setView(layout)

                    .setPositiveButton("Save", (d, w) -> {

                        String variety = inputVariety.getText().toString().trim();
                        String qty = inputQty.getText().toString().trim();

                        if(variety.isEmpty() || qty.isEmpty() || selectedVarietyId[0]==0){
                            showToast("Select variety and quantity");
                            return;
                        }

                        int quantity = Integer.parseInt(qty);

                        // ✅ ONLY SAVE FIX (pending already saved earlier)
                        StockRepository.saveFixedScan(
                                this, qr, selectedVarietyId[0],
                                variety, quantity, selectedFarm[0]
                        );

                        // ✅ COUNT HERE (since it's valid now)
                        markSuccessfulScan(
                                qr,
                                (isDuplicate ? "DUPLICATE ⚠ " : "FIXED ")
                                        + variety + " (" + qty + ")"
                        );
                    })

                    .setNegativeButton("Cancel", (d, w) -> {

                        // ✅ REMOVE FROM PENDING
                        StockRepository.removeFromPending(this, qr);

                        addRecent(qr, "IGNORED");
                    })

                    .show();
        });
    }

    private void showOldStockDialog(String qr,String variety,long age, boolean isDuplicate){

        runOnUiThread(() -> {

            new AlertDialog.Builder(this)
                    .setTitle("Old Stock Detected")
                    .setMessage(
                            "QR belongs to OLD stock\n\n"+
                                    "Variety: "+variety+
                                    "\nAge: "+age+" hours\n\n"+
                                    "Continue stock take?"
                    )

                    .setPositiveButton("Continue", (d,w) -> {

                        markSuccessfulScan(
                                qr,
                                (isDuplicate ? "DUPLICATE ⚠ " : "OLD ") + variety
                        );
                    })

                    .setNegativeButton("Cancel",(d,w)->{

                        // ✅ REMOVE FROM PENDING
                        StockRepository.removeFromPending(this, qr);

                        addRecent(qr,"OLD CANCELLED");

                    })

                    .show();
        });
    }
    private void vibrate(){
        if(vibrator != null){
            vibrator.vibrate(200); // 200ms
        }
    }

    private void updateStats(){
        runOnUiThread(() -> {
            tvTotal.setText(String.valueOf(totalCount));
            tvValid.setText(String.valueOf(validCount));
            tvDuplicate.setText(String.valueOf(duplicateCount));
        });
    }

    private void markSuccessfulScan(String qr, String label){
        validCount++;
        addRecent(qr, label);
        updateStats();
    }


}