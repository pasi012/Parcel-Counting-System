package com.example.parcelcountingsystem.home.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.parcelcountingsystem.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri logoUri;
    private ImageView imageViewLogo;
    private EditText editTextCompanyName;
    private Switch switchDuplicateBarcode;
    private EditText editTextBarcodeDigits;

    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firebaseStorage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = firebaseStorage.getReference();

        editTextCompanyName = findViewById(R.id.editTextCompanyName);
        progressBar = findViewById(R.id.progressBar);
        imageViewLogo = findViewById(R.id.imageViewLogo);
        switchDuplicateBarcode = findViewById(R.id.switchDuplicateBarcode);
        editTextBarcodeDigits = findViewById(R.id.editTextBarcodeDigits);
        Button buttonPickImage = findViewById(R.id.buttonPickImage);
        Button buttonSave = findViewById(R.id.buttonSave);

        buttonPickImage.setOnClickListener(v -> openGallery());

        buttonSave.setOnClickListener(v -> saveSettings());

        // Load data when the activity is created
        loadData();
    }

    private void loadData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection(userId).document("settings").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String companyName = documentSnapshot.getString("companyName");
                        Boolean allowDuplicateBarcode = documentSnapshot.getBoolean("duplicateBarcode");
                        Long barcodeDigits = documentSnapshot.getLong("digits");
                        String logoUrl = documentSnapshot.getString("logo");

                        // Set data to UI components
                        editTextCompanyName.setText(companyName != null ? companyName : "");
                        switchDuplicateBarcode.setChecked(allowDuplicateBarcode != null ? allowDuplicateBarcode : false);
                        editTextBarcodeDigits.setText(barcodeDigits != null ? barcodeDigits.toString() : "");

                        // Load logo image if URL is available
                        if (logoUrl != null) {
                            // Use a library like Glide or Picasso to load the image from the URL
                            Glide.with(this)
                                    .load(logoUrl)
                                    .into(imageViewLogo);
                        }
                    } else {
                        Toast.makeText(SettingsActivity.this, "No settings found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SettingsActivity", "Load failed", e);
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            logoUri = data.getData();
            imageViewLogo.setImageURI(logoUri);
        }
    }

    private void saveSettings() {
        // Get values from input fields
        String companyName = editTextCompanyName.getText().toString().trim();
        String barcodeDigitsStr = editTextBarcodeDigits.getText().toString().trim();

        // Check if company name is empty
        if (companyName.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Company name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if barcode digits is empty or not a number
        int barcodeDigits = 0;
        if (barcodeDigitsStr.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Barcode digits cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        } else {
            try {
                barcodeDigits = Integer.parseInt(barcodeDigitsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(SettingsActivity.this, "Invalid barcode digits", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Proceed with saving settings
        if (logoUri != null) {
            uploadLogoToFirebase();
        } else {
            saveSettingsToFirestore(null);
        }
    }

    private void uploadLogoToFirebase() {
        StorageReference fileReference = storageReference.child("logos/" + System.currentTimeMillis() + ".jpg");

        fileReference.putFile(logoUri)
                .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String logoUrl = uri.toString();
                    saveSettingsToFirestore(logoUrl);
                }))
                .addOnFailureListener(exception -> {
                    Toast.makeText(SettingsActivity.this, "Upload failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SettingsActivity", "Upload failed", exception);
                });
    }

    private void saveSettingsToFirestore(String logoUrl) {

        progressBar.setVisibility(View.VISIBLE);

        String companyName = editTextCompanyName.getText().toString().trim();
        boolean allowDuplicateBarcode = switchDuplicateBarcode.isChecked();
        int barcodeDigits = Integer.parseInt(editTextBarcodeDigits.getText().toString().trim());

        // Create a data map
        Map<String, Object> settings = new HashMap<>();
        settings.put("companyName", companyName);
        settings.put("duplicateBarcode", allowDuplicateBarcode);
        settings.put("digits", barcodeDigits);
        if (logoUrl != null) {
            settings.put("logo", logoUrl);
        }

        // Save to Firestore
        firestore.collection(FirebaseAuth.getInstance().getCurrentUser().getUid()).document("settings")
                .set(settings)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Failed to save settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SettingsActivity", "Save failed", e);
                });
    }
}