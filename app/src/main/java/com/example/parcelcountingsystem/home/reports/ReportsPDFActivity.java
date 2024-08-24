package com.example.parcelcountingsystem.home.reports;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.example.parcelcountingsystem.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ReportsPDFActivity extends AppCompatActivity {

    private PDFView pdfView;
    private String fileName;
    private Map<String, Object> report;
    String companyName;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        pdfView = findViewById(R.id.pdfView);
        FloatingActionButton shareFab = findViewById(R.id.shareFab); // Add this line

        // Retrieve data from Intent
        report = (Map<String, Object>) getIntent().getSerializableExtra("report");
        if (report == null) {
            Toast.makeText(this, "No report data found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String vehicleNumber = (String) report.get("vehicleNumber");
        String loadingID = (String) report.get("loadingId");
        Long targetQuantity = (Long) report.get("targetQuantity");
        Long barcodeCount = (Long) report.get("barcodeCount");
        String countingOfficer = (String) report.get("countingOfficerName");

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String namePart = "";
        if (email != null && email.contains("@")) {
            // Split the email at '@' and take the first part
            namePart = email.split("@")[0];
            // Capitalize the first letter of the name part
            if (namePart.length() > 0) {
                namePart = namePart.substring(0, 1).toUpperCase() + namePart.substring(1).toLowerCase();
            }
        }

        // Format the datetime
        String datetimeNow = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();

        // Load data and generate PDF after data is retrieved
        loadData(vehicleNumber, loadingID, targetQuantity, barcodeCount, countingOfficer, namePart, datetimeNow);

        // Set up the share button listener
        shareFab.setOnClickListener(v -> sharePdf());

    }

    private void loadData(String vehicleNumber, String loadingID, Long targetQuantity, Long barcodeCount, String countingOfficer, String namePart, String datetimeNow) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection(userId).document("settings").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        companyName = documentSnapshot.getString("companyName");
                        if (companyName != null) {
                            // Generate the PDF after companyName is retrieved
                            fileName = "loading_report_" + loadingID + ".pdf";
                            generatePdf(namePart, companyName, vehicleNumber, loadingID, String.valueOf(targetQuantity), String.valueOf(barcodeCount), countingOfficer, datetimeNow, fileName);

                            // Load the generated PDF into PDFView
                            File pdfFile = new File(getExternalFilesDir(null), fileName);
                            if (pdfFile.exists()) {
                                pdfView.fromFile(pdfFile)
                                        .defaultPage(0)
                                        .enableSwipe(true)
                                        .swipeHorizontal(false)
                                        .enableDoubletap(true)
                                        .load();
                            } else {
                                Toast.makeText(this, "PDF file not found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ReportsPDFActivity.this, "Company name not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ReportsPDFActivity.this, "No settings found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ReportsPDFActivity.this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SettingsActivity", "Load failed", e);
                });
    }

    private void generatePdf(String namePart, String companyName, String vehicleNumber, String loadingID, String targetQuantity, String barcodeCount, String countingOfficer, String datetimeNow, String fileName) {
        try {
            // Create a PdfWriter instance
            PdfWriter writer = new PdfWriter(new File(getExternalFilesDir(null), fileName).getAbsolutePath());

            // Create a PdfDocument instance
            PdfDocument pdf = new PdfDocument(writer);

            // Create a Document instance
            Document document = new Document(pdf);

            // Add datetime at the top, centered
            document.add(new Paragraph("Datetime: " + datetimeNow)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBold()
                    .setFontSize(20));

            // Add title, centered
            document.add(new Paragraph(companyName + "\nParcel / Package Loading report")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(30));

            // Add a separator line
            document.add(new Paragraph("====================================")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Add details, centered
            document.add(new Paragraph("Vehicle Number: " + vehicleNumber + "\tLoading ID: " + loadingID)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));
            document.add(new Paragraph("Target Quantity: " + targetQuantity + "\tBarcode Count: " + barcodeCount)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Add barcodes section
            document.add(new Paragraph("Barcodes:")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Add each barcode, centered
            if (report.get("barcodes") instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<String> barcodes = (ArrayList<String>) report.get("barcodes");
                for (String barcode : barcodes) {
                    document.add(new Paragraph("• " + barcode)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(20));
                }
            }

            document.add(new Paragraph("Counting Officer: " + countingOfficer + "\tAuthorized Officer: " + namePart)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Add a footer or additional notes, centered
            document.add(new Paragraph("Thank you!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Close the document
            document.close();

            Toast.makeText(this, "PDF generated successfully.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdf() {
        File pdfFile = new File(getExternalFilesDir(null), fileName);
        if (!pdfFile.exists()) {
            Toast.makeText(this, "PDF file not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, "com.example.parcelcountingsystem.provider", pdfFile));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share PDF using"));
    }
}