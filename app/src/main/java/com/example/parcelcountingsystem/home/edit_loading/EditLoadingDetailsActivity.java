package com.example.parcelcountingsystem.home.edit_loading;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parcelcountingsystem.R;
import com.example.parcelcountingsystem.adapters.BarcodeAdapter;
import com.example.parcelcountingsystem.home.new_loading.PdfActivity;
import com.example.parcelcountingsystem.home.new_loading.StartCountingActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditLoadingDetailsActivity extends AppCompatActivity {

    String uuid;
    private StringBuilder barcodeBuilder = new StringBuilder();
    private TextView vehicleNumber, loadingID, targetQuantity, barcodeCount, txtNoData;
    private ListView barcodeList;
    private ArrayList<String> barcodes;
    private BarcodeAdapter adapter;
    private FirebaseFirestore firestore;
    Long barcodeDigits;
    Boolean allowDuplicateBarcode;
    String companyName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_loading_details);

        // Initialize views
        vehicleNumber = findViewById(R.id.vehicleNumber);
        loadingID = findViewById(R.id.loadingID);
        targetQuantity = findViewById(R.id.targetQuantity);
        barcodeCount = findViewById(R.id.barcodeCount);
        barcodeList = findViewById(R.id.barcodeList);
        txtNoData = findViewById(R.id.txtNoData);

        // Initialize barcode list and adapter
        barcodes = new ArrayList<>();
        adapter = new BarcodeAdapter(this, barcodes); // Implement BarcodeAdapter to display list items
        barcodeList.setAdapter(adapter);

        // Get data from the intent
        Intent intent = getIntent();
        Map<String, Object> loading = (Map<String, Object>) intent.getSerializableExtra("loading");
        if (loading != null) {
            uuid = loading.get("id").toString();
            loadingID.setText("Loading ID: " + loading.get("loadingId"));
            vehicleNumber.setText("Veh No: " + loading.get("vehicleNumber"));
            targetQuantity.setText("Target Quantity: " + loading.get("targetQuantity"));

            // Retrieve barcode count and add barcodes
            barcodeCount.setText("Count: " + loading.get("count"));
            List<String> barcodeListData = (List<String>) loading.get("barcodes");
            if (barcodeListData != null) {
                barcodes.addAll(barcodeListData);
            }
            adapter.notifyDataSetChanged(); // Update ListView

            // Hide or show no data message
            txtNoData.setVisibility(barcodes.isEmpty() ? View.VISIBLE : View.GONE);
        }

        // Focus on the main layout to catch the barcode scanner input
        findViewById(R.id.main_layout).setFocusableInTouchMode(true);
        findViewById(R.id.main_layout).requestFocus();

        findViewById(R.id.btnReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (barcodes.size() <= Integer.parseInt(loading.get("targetQuantity").toString())) {
                    Intent intent = new Intent(EditLoadingDetailsActivity.this, PdfActivity.class);
                    intent.putExtra("vehicleNumber", loading.get("vehicleNumber").toString());
                    intent.putExtra("loadingID", loading.get("loadingId").toString());
                    intent.putExtra("targetQuantity", loading.get("targetQuantity").toString());
                    intent.putExtra("barcodeCount", String.valueOf(barcodes.size()));
                    intent.putExtra("barcodes", barcodes);
                    intent.putExtra("countingOfficer", loading.get("countingOfficerName").toString());
                    intent.putExtra("companyName", companyName.toString());
                    startActivity(intent);
                } else {
                    Toast.makeText(EditLoadingDetailsActivity.this, "Barcode count not equal with Target Quantity", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btnUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (barcodes.size() <= Integer.parseInt(loading.get("targetQuantity").toString())) {

                    updateDataToFirestore(
                            loading.get("vehicleNumber").toString(),
                            loading.get("loadingId").toString(),
                            barcodes,
                            Integer.parseInt(loading.get("targetQuantity").toString()),
                            barcodes.size(),
                            loading.get("countingOfficerName").toString()
                    );
                    finish();

                }else {

                    Toast.makeText(EditLoadingDetailsActivity.this, "Barcode count not equal with Target Quantity", Toast.LENGTH_SHORT).show();

                }

            }
        });

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();

        loadData();

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
                        Toast.makeText(EditLoadingDetailsActivity.this, "No settings found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditLoadingDetailsActivity.this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void updateDataToFirestore(
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
                        Toast.makeText(EditLoadingDetailsActivity.this, "Data updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(EditLoadingDetailsActivity.this, "Failed to update data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(EditLoadingDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
