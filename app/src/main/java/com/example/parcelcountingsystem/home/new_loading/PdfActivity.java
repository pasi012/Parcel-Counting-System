package com.example.parcelcountingsystem.home.new_loading;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.example.parcelcountingsystem.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
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

public class PdfActivity extends AppCompatActivity {

    private PDFView pdfView;
    private String fileName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        pdfView = findViewById(R.id.pdfView);
        FloatingActionButton shareFab = findViewById(R.id.shareFab); // Add this line

        // Retrieve data from Intent
        String vehicleNumber = getIntent().getStringExtra("vehicleNumber");
        String loadingID = getIntent().getStringExtra("loadingID");
        String targetQuantity = getIntent().getStringExtra("targetQuantity");
        String barcodeCount = getIntent().getStringExtra("barcodeCount");
        ArrayList<String> barcodes = getIntent().getStringArrayListExtra("barcodes");
        String countingOfficer = getIntent().getStringExtra("countingOfficer");
        String companyName = getIntent().getStringExtra("companyName");

        // Format the datetime
        String datetimeNow = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

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
        
        // Generate the PDF
        fileName = "loading_report_" + loadingID + ".pdf";
        generatePdf(namePart, companyName, vehicleNumber, loadingID, targetQuantity, barcodeCount, barcodes, countingOfficer, datetimeNow, fileName);

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

        // Set up the share button listener
        shareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePdf();
            }
        });
    }

    private void generatePdf(String namePart, String companyName, String vehicleNumber, String loadingID, String targetQuantity, String barcodeCount, ArrayList<String> barcodes, String countingOfficer, String datetimeNow, String fileName) {
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
            for (String barcode : barcodes) {
                document.add(new Paragraph("• " + barcode)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(20));
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
