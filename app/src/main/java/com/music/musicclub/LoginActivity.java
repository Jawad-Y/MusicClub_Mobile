package com.music.musicclub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError(getString(R.string.error_email_is_required));
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError(getString(R.string.error_password_required));
                return;
            }

            /*
            if(!auth(email,password)){
             etPassword.setError(getString(R.string.incorrect_email_or_password));
             return;
            }
            */


            Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this,HostActivity.class);
            startActivity(intent);

        });
    }

}
