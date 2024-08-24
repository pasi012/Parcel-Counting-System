package com.example.parcelcountingsystem.authentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
import com.example.parcelcountingsystem.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, passwordInput;
    private Button registerButton;
    private TextView loginRedirectText;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.nameInput);
        passwordInput = findViewById(R.id.passwordInput);
        registerButton = findViewById(R.id.registerButton);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        progressBar = findViewById(R.id.progressBar); // Initialize the ProgressBar
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser() {
        String name = nameInput.getText().toString().trim();
        String email = "@example.com";
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password)) {
            Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(RegisterActivity.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.contains(" ")) {
            Toast.makeText(RegisterActivity.this, "Please Enter First Name Only", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // Show the ProgressBar

        mAuth.createUserWithEmailAndPassword(name + email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE); // Hide the ProgressBar

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates);

                                User userData = new User(name, password);
                                db.collection("Users").document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            Toast.makeText(RegisterActivity.this, "Successfully Registered", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}