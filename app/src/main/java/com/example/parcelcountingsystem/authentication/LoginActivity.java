package com.example.parcelcountingsystem.authentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.parcelcountingsystem.MainActivity;
import com.example.parcelcountingsystem.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText nameInput, passwordInput;
    private Button loginButton;
    private TextView registerRedirectText;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        nameInput = findViewById(R.id.nameInput);
        passwordInput = findViewById(R.id.passwordInput);
        progressBar = findViewById(R.id.progressBar);
        loginButton = findViewById(R.id.loginButton);
        registerRedirectText = findViewById(R.id.registerRedirectText);
        mAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()) {
                    loginUser();
                } else {
                    Toast.makeText(LoginActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        registerRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void loginUser() {
        String name = nameInput.getText().toString().trim();
        String email = "@example.com";
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(LoginActivity.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.contains(" ")) {
            Toast.makeText(LoginActivity.this, "Please Enter First Name Only", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // Show the ProgressBar

        mAuth.signInWithEmailAndPassword(name + email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide the ProgressBar

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}