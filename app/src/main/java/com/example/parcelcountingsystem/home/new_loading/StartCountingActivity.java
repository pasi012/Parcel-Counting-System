package com.example.parcelcountingsystem.home.new_loading;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.parcelcountingsystem.R;
import com.example.parcelcountingsystem.adapters.BarcodeAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StartCountingActivity extends AppCompatActivity {

    TextView vehicleNumber, loadingID, targetQuantity, barcodeCount;
    private StringBuilder barcodeBuilder = new StringBuilder();
    private TextView txtNoData;
    Long barcodeDigits;
    Boolean allowDuplicateBarcode;
    private ListView barcodeList;
    private ArrayList<String> barcodes;
    private BarcodeAdapter adapter;
    private FirebaseFirestore firestore;

    String companyName;
    String vehNumber, loadID, targetQty, countingOfficer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_counting);

        vehicleNumber = findViewById(R.id.vehicleNumber);
        loadingID = findViewById(R.id.loadingID);
        targetQuantity = findViewById(R.id.targetQuantity);
        barcodeCount = findViewById(R.id.barcodeCount);

        vehNumber = getIntent().getStringExtra("vehicleNumber");
        loadID = getIntent().getStringExtra("loadingID");
        targetQty = getIntent().getStringExtra("targetQuantity");
        countingOfficer = getIntent().getStringExtra("countingOfficer");

        vehicleNumber.setText("Veh No: " + vehNumber);
        loadingID.setText("Loading ID: " + loadID);
        targetQuantity.setText("Target Quantity: " + targetQty);
        barcodeCount.setText("Count: 0");

        // Initialize views
        txtNoData = findViewById(R.id.txtNoData);
        barcodeList = findViewById(R.id.barcodeList);

        // Initialize barcode list and adapter
        barcodes = new ArrayList<>();
        adapter = new BarcodeAdapter(this, barcodes); // Implement BarcodeAdapter to display list items
        barcodeList.setAdapter(adapter);

        // Hide txtNoData initially if no data
        txtNoData.setVisibility(barcodes.isEmpty() ? View.VISIBLE : View.GONE);

        // Focus on the main layout to catch the barcode scanner input
        findViewById(R.id.main_layout).setFocusableInTouchMode(true);
        findViewById(R.id.main_layout).requestFocus();

        findViewById(R.id.btnReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (barcodes.size() == 0){

                    Toast.makeText(StartCountingActivity.this, "No Barcodes Please Enter Barcodes", Toast.LENGTH_SHORT).show();

                }else if (barcodes.size() <= Integer.parseInt(targetQty)) {

                    Intent intent = new Intent(StartCountingActivity.this, PdfActivity.class);
                    intent.putExtra("vehicleNumber", vehNumber);
                    intent.putExtra("loadingID", loadID);
                    intent.putExtra("targetQuantity", targetQty);
                    intent.putExtra("barcodeCount", String.valueOf(barcodes.size()));
                    intent.putExtra("barcodes", barcodes);
                    intent.putExtra("countingOfficer", countingOfficer);
                    intent.putExtra("companyName", companyName.toString());
                    startActivity(intent);

                } else {

                    Toast.makeText(StartCountingActivity.this, "Barcode count not equal with Target Quantity", Toast.LENGTH_SHORT).show();

                }

            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (barcodes.size() == 0){

                    Toast.makeText(StartCountingActivity.this, "No Barcodes Please Enter Barcodes", Toast.LENGTH_SHORT).show();

                }else if (barcodes.size() <= Integer.parseInt(targetQty)) {

                    saveDataToFirestore(
                            vehNumber,
                            loadID,
                            barcodes,
                            Integer.parseInt(targetQty),
                            barcodes.size(),
                            countingOfficer
                    );
                    finish();

                }else {

                    Toast.makeText(StartCountingActivity.this, "Barcode count not equal with Target Quantity", Toast.LENGTH_SHORT).show();

                }

            }
        });

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();

        loadData();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (barcodes.size() != 0 && barcodes.size() <= Integer.parseInt(targetQty)) {

            saveDataToFirestore(
                    vehNumber,
                    loadID,
                    barcodes,
                    Integer.parseInt(targetQty),
                    barcodes.size(),
                    countingOfficer
            );

        }

    }

    private void loadData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection(userId).document("settings").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        allowDuplicateBarcode = documentSnapshot.getBoolean("duplicateBarcode");
                        barcodeDigits = documentSnapshot.getLong("digits");
                        companyName = documentSnapshot.getString("companyName");
                    } else {
                        Toast.makeText(StartCountingActivity.this, "No settings found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StartCountingActivity.this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SettingsActivity", "Load failed", e);
                });
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check if the key is ENTER
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            String scannedBarcode = barcodeBuilder.toString();
            if (!scannedBarcode.isEmpty()) {
                // Check if the scanned barcode's digit count matches the expected barcodeDigits
                if (scannedBarcode.length() != barcodeDigits) {
                    Toast.makeText(this, "Digit count not equal", Toast.LENGTH_SHORT).show();
                } else {
                    if (!allowDuplicateBarcode && barcodes.contains(scannedBarcode)) {
                        // Show toast if duplicate barcode is detected
                        Toast.makeText(this, "Duplicate barcode detected", Toast.LENGTH_SHORT).show();
                    } else {
                        barcodes.add(scannedBarcode);
                        adapter.notifyDataSetChanged(); // Update ListView
                        barcodeCount.setText("Count: " + barcodes.size()); // Update barcode count
                        txtNoData.setVisibility(View.GONE); // Hide "Please Start Barcode Scan" text
                    }
                }
            } else {
                Toast.makeText(this, "Invalid barcode", Toast.LENGTH_SHORT).show();
            }
            // Clear the StringBuilder after processing
            barcodeBuilder.setLength(0);
            return true;
        } else {
            // Accumulate the barcode characters
            char inputChar = (char) event.getUnicodeChar();
            barcodeBuilder.append(inputChar);
            return super.onKeyUp(keyCode, event);
        }
    }

    private void saveDataToFirestore(
            String vehicleNumber,
            String loadingId,
            ArrayList<String> barcodes,
            int targetQuantity,
            int scannedBarcodeCount,
            String countingOfficerName) {

        try {
            // Format the datetime
            String datetimeNow = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String uuid = java.util.UUID.randomUUID().toString(); // Generate UUID

            // Create a document reference
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference docRef = firestore.collection(userId).document("loadings");

            // Create a map for the data
            Map<String, Object> data = new HashMap<>();
            data.put(uuid, new HashMap<String, Object>() {{
                put("userId", userId);
                put("id", uuid);
                put("countingOfficerName", countingOfficerName);
                put("vehicleNumber", vehicleNumber);
                put("loadingId", loadingId);
                put("barcodes", barcodes);
                put("targetQuantity", targetQuantity);
                put("count", scannedBarcodeCount);
                put("dateTime", datetimeNow);
            }});

            // Save data to Firestore
            docRef.set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(StartCountingActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(StartCountingActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(StartCountingActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}