package com.example.parcelcountingsystem.home.new_loading;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.parcelcountingsystem.R;

public class NewLoadingActivity extends AppCompatActivity {

    EditText vehicleNumber, loadingID, targetQuantity, countingOfficer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_loading);

        vehicleNumber = findViewById(R.id.editTextVehicleNumber);
        loadingID = findViewById(R.id.editTextLoadingId);
        targetQuantity = findViewById(R.id.editTextTargetQuantity);
        countingOfficer = findViewById(R.id.editTextCountingOfficer);

        // Set input type to number only
        targetQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        findViewById(R.id.buttonSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateFields()) {
                    Intent intent = new Intent(NewLoadingActivity.this, StartCountingActivity.class);
                    intent.putExtra("vehicleNumber", vehicleNumber.getText().toString());
                    intent.putExtra("loadingID", loadingID.getText().toString());
                    intent.putExtra("targetQuantity", targetQuantity.getText().toString());
                    intent.putExtra("countingOfficer", countingOfficer.getText().toString());
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(NewLoadingActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean validateFields() {
        return !TextUtils.isEmpty(vehicleNumber.getText().toString()) &&
                !TextUtils.isEmpty(loadingID.getText().toString()) &&
                !TextUtils.isEmpty(targetQuantity.getText().toString()) &&
                !TextUtils.isEmpty(countingOfficer.getText().toString());
    }
}