package com.example.ellibrodelcuco.interfaz_usuario.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.repository.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authRepository = AuthRepository.getInstance();

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show();
                return;
            }
            btnLogin.setEnabled(false);
            authRepository.signIn(email, password, new com.example.ellibrodelcuco.data.callback.Callback<com.google.firebase.auth.FirebaseUser>() {
                @Override
                public void onSuccess(com.google.firebase.auth.FirebaseUser result) {
                    Log.d("AUTH", "Login correcto: " + result.getUid());
                    redirigirSegunRol(result.getUid());
                }
                @Override
                public void onError(String errorMessage) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        tvRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // comprobamos el usuario y su rol
        com.google.firebase.auth.FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            redirigirSegunRol(user.getUid());
        }
    }

    private void redirigirSegunRol(String uid) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                String rol = doc.getString("rol");
                if ("admin".equals(rol)) {
                    startActivity(new Intent(this, AdminActivity.class));
                } else {
                    startActivity(new Intent(this, MainActivity.class));
                }
                finish();
            })
            .addOnFailureListener(e -> {
                // Si Firestore falla, navegar a MainActivity por defecto
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
    }

}
