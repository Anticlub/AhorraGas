package com.example.ahorragas;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DistanceListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_distance_list);

        setupBottomNav();

    }

    private void setupBottomNav(){
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavDistance);
        bottomNav.setSelectedItemId(R.id.nav_distance);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_distance) return true;
            if (id == R.id.nav_map) {
                finish();
                return true;
            }
            if (id == R.id.nav_preferences) {
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            }
            return false;
        });
    }
}