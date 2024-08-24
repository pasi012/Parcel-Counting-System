package com.example.parcelcountingsystem.home.reports;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parcelcountingsystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {

    private List<Map<String, Object>> reports = new ArrayList<>();
    private List<Map<String, Object>> filteredReports = new ArrayList<>();
    private EditText searchEditText;
    private GridView gridView;
    private ReportAdapter reportAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        searchEditText = findViewById(R.id.searchEditText);
        gridView = findViewById(R.id.gridView);

        fetchReports();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchReports() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection(uid)
                .document("loadings")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (Object value : data.values()) {
                                if (value instanceof Map) {
                                    reports.add((Map<String, Object>) value);
                                }
                            }
                            filteredReports = new ArrayList<>(reports);
                            updateGridView();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ReportsActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterReports(String query) {
        query = query.toLowerCase();
        filteredReports.clear();
        for (Map<String, Object> loading : reports) {
            String countingOfficerName = String.valueOf(loading.get("countingOfficerName")).toLowerCase();
            String vehicleNumber = String.valueOf(loading.get("vehicleNumber")).toLowerCase();
            String loadingId = String.valueOf(loading.get("loadingId")).toLowerCase();

            if (countingOfficerName.contains(query) || vehicleNumber.contains(query) || loadingId.contains(query)) {
                filteredReports.add(loading);
            }
        }
        updateGridView();
    }

    private void updateGridView() {
        if (reportAdapter == null) {
            reportAdapter = new ReportAdapter(this, filteredReports);
            gridView.setAdapter(reportAdapter);
        } else {
            reportAdapter.updateReports(filteredReports);
        }

        if (filteredReports.isEmpty()) {
            ImageView noDataImageView = findViewById(R.id.noDataImageView);
            TextView noDataTextView = findViewById(R.id.noDataTextView);
            noDataImageView.setVisibility(View.VISIBLE);
            noDataTextView.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        } else {
            gridView.setVisibility(View.VISIBLE);
            findViewById(R.id.noDataImageView).setVisibility(View.GONE);
            findViewById(R.id.noDataTextView).setVisibility(View.GONE);
        }
    }

    private class ReportAdapter extends BaseAdapter {
        private List<Map<String, Object>> reports;

        public ReportAdapter(Context context, List<Map<String, Object>> reports) {
            this.reports = reports;
        }

        @Override
        public int getCount() {
            return reports.size();
        }

        @Override
        public Object getItem(int position) {
            return reports.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reports, parent, false);
            }

            Map<String, Object> report = reports.get(position);
            TextView reportInfoTextView = convertView.findViewById(R.id.loadingInfoTextView);
            reportInfoTextView.setText(
                    "Loading ID: " + report.get("loadingId") + "\n" +
                            "Vehicle No: " + report.get("vehicleNumber") + "\n" +
                            "Count Officer: " + report.get("countingOfficerName")
            );

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(ReportsActivity.this, ReportsPDFActivity.class);
                intent.putExtra("report", (Serializable) report);
                startActivity(intent);
            });

            return convertView;
        }

        public void updateReports(List<Map<String, Object>> newReports) {
            this.reports = newReports;
            notifyDataSetChanged();
        }
    }
}