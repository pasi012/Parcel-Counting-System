package com.example.parcelcountingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parcelcountingsystem.authentication.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay to show splash screen
        new Handler().postDelayed(() -> {
            // Start main activity
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
