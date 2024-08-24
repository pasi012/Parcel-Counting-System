package com.example.parcelcountingsystem.home.edit_loading;

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

public class EditLoadingActivity extends AppCompatActivity {

    private List<Map<String, Object>> loadings = new ArrayList<>();
    private List<Map<String, Object>> filteredLoadings = new ArrayList<>();
    private EditText searchEditText;
    private GridView gridView;
    private LoadingAdapter loadingAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_loading);

        searchEditText = findViewById(R.id.searchEditText);
        gridView = findViewById(R.id.gridView);

        fetchLoadings();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLoadings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchLoadings() {
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
                                    loadings.add((Map<String, Object>) value);
                                }
                            }
                            filteredLoadings = new ArrayList<>(loadings);
                            updateGridView();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditLoadingActivity.this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterLoadings(String query) {
        query = query.toLowerCase();
        filteredLoadings.clear();
        for (Map<String, Object> loading : loadings) {
            String countingOfficerName = String.valueOf(loading.get("countingOfficerName")).toLowerCase();
            String vehicleNumber = String.valueOf(loading.get("vehicleNumber")).toLowerCase();
            String loadingId = String.valueOf(loading.get("loadingId")).toLowerCase();

            if (countingOfficerName.contains(query) || vehicleNumber.contains(query) || loadingId.contains(query)) {
                filteredLoadings.add(loading);
            }
        }
        updateGridView();
    }

    private void updateGridView() {
        if (loadingAdapter == null) {
            loadingAdapter = new LoadingAdapter(this, filteredLoadings);
            gridView.setAdapter(loadingAdapter);
        } else {
            loadingAdapter.updateLoadings(filteredLoadings);
        }

        if (filteredLoadings.isEmpty()) {
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

    private class LoadingAdapter extends BaseAdapter {
        private List<Map<String, Object>> loadings;

        public LoadingAdapter(Context context, List<Map<String, Object>> loadings) {
            this.loadings = loadings;
        }

        @Override
        public int getCount() {
            return loadings.size();
        }

        @Override
        public Object getItem(int position) {
            return loadings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            }

            Map<String, Object> loading = loadings.get(position);
            TextView loadingInfoTextView = convertView.findViewById(R.id.loadingInfoTextView);
            loadingInfoTextView.setText(
                    "Loading ID: " + loading.get("loadingId") + "\n" +
                            "Vehicle No: " + loading.get("vehicleNumber") + "\n" +
                            "Count Officer: " + loading.get("countingOfficerName")
            );

            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(EditLoadingActivity.this, EditLoadingDetailsActivity.class);
                intent.putExtra("loading", (Serializable) loading);
                startActivity(intent);
            });

            return convertView;
        }

        public void updateLoadings(List<Map<String, Object>> newLoadings) {
            this.loadings = newLoadings;
            notifyDataSetChanged();
        }
    }
}