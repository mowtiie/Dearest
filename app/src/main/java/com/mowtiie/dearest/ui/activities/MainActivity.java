package com.mowtiie.dearest.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.mowtiie.dearest.DearestApp;
import com.mowtiie.dearest.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends DearestActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = host.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);
        appBarConfiguration = new AppBarConfiguration.Builder(bottomNav.getMenu()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        applyInsets(bottomNav);

        DearestApp.from(this).lockState().observe(this, locked -> {
            if (Boolean.TRUE.equals(locked)) {
                startActivity(new Intent(this, UnlockActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }
        });
    }

    private void applyInsets(View bottomNav) {
        View root = findViewById(R.id.main_root);
        View appBar = findViewById(R.id.app_bar);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, wi) -> {
            Insets bars = wi.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(appBar.getPaddingLeft(), bars.top, appBar.getPaddingRight(), appBar.getPaddingBottom());
            bottomNav.setPadding(bottomNav.getPaddingLeft(), bottomNav.getPaddingTop(), bottomNav.getPaddingRight(), bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}