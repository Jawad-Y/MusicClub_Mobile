package com.music.musicclub;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.host_layout);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

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


            bottomNavigationView.setOnItemSelectedListener(null);
            bottomNavigationView.setSelectedItemId(R.id.nav_more);
            bottomNavigationView.setOnItemSelectedListener(bottomNavListener);

            return true;
        });

        bottomSheetDialog.show();
    }
}
