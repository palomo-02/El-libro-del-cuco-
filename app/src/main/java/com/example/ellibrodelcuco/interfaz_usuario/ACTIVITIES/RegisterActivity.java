package com.example.ellibrodelcuco.interfaz_usuario.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.callback.Callback;
import com.example.ellibrodelcuco.data.repository.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
/**
 * Pantalla de registro para el inicio de un perfil
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPass, etPassConfirm;
    private MaterialButton btnRegister;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        authRepository = AuthRepository.getInstance();

        etEmail       = findViewById(R.id.etEmailReg);
        etPass        = findViewById(R.id.etPassReg);
        etPassConfirm = findViewById(R.id.etPassConfirm);
        btnRegister   = findViewById(R.id.btnRegister);

        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String email   = etEmail.getText().toString().trim();
            String pass    = etPass.getText().toString().trim();
            String confirm = etPassConfirm.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(this, getString(R.string.error_campos_vacios), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!validarContrasena(pass)) return;
            if (!pass.equals(confirm)) {
                etPassConfirm.setError(getString(R.string.error_passwords_no_coinciden));
                return;
            }

            btnRegister.setEnabled(false);
            authRepository.register(email, pass, new Callback<FirebaseUser>() {
                @Override
                public void onSuccess(FirebaseUser result) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.cuenta_creada), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                @Override
                public void onError(String errorMessage) {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
// comprobaciones de las contraseñas
    private boolean validarContrasena(String pass) {
        if (pass.length() < 8) {
            etPass.setError(getString(R.string.error_password_min_length));
            return false;
        }
        if (!pass.matches(".*[A-Z].*")) {
            etPass.setError(getString(R.string.error_password_mayuscula));
            return false;
        }
        if (!pass.matches(".*[0-9].*")) {
            etPass.setError(getString(R.string.error_password_numero));
            return false;
        }
        return true;
    }
}
