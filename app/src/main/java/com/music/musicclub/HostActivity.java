package com.music.musicclub;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

public class HostActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private NavigationBarView.OnItemSelectedListener bottomNavListener;

    private LoginManager loginManager;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_layout);

        loginManager = new LoginManager(this);
        token = loginManager.getToken();

        // 1) Hard gate: no token -> back to login
        if (token == null || token.trim().isEmpty()) {
            goToLogin();
            return;
        }

        // 2) Validate token once (ME). Only init UI after success (or non-401 error if you prefer).
        ApiService api = new ApiService(this, token);
        api.me(
                response -> {
                    // token valid -> continue normal setup
                    runOnUiThread(this::initUi);
                },
                error -> {
                    if (error != null && error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        loginManager.clear();
                        goToLogin();
                    } else {
                        // Non-auth error (timeout/no internet/etc).
                        // Minimal-change decision: allow app to continue.
                        runOnUiThread(this::initUi);
                    }
                }
        );
    }

    private void initUi() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        // Safety: avoid crash if layout id is wrong/missing
        if (navHostFragment == null) {
            finish();
            return;
        }

        navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        bottomNavListener = new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (item.getItemId() == R.id.nav_more) {
                    showBottomSheet();
                    return false;
                }

                return NavigationUI.onNavDestinationSelected(item, navController);
            }
        };

        bottomNavigationView.setOnItemSelectedListener(bottomNavListener);
    }

    private void goToLogin() {
        Intent i = new Intent(HostActivity.this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView =
                getLayoutInflater().inflate(R.layout.bottom_sheet_overflow, null);

        bottomSheetDialog.setContentView(bottomSheetView);

        NavigationView moreNavigationView =
                bottomSheetView.findViewById(R.id.more_navigation_view);

        moreNavigationView.setNavigationItemSelectedListener(item -> {

            bottomSheetDialog.dismiss();

            NavigationUI.onNavDestinationSelected(item, navController);

            // Keep your current behavior (minimal change)
            bottomNavigationView.setOnItemSelectedListener(null);
            bottomNavigationView.setSelectedItemId(R.id.nav_more);
            bottomNavigationView.setOnItemSelectedListener(bottomNavListener);

            return true;
        });

        bottomSheetDialog.show();
    }
}
