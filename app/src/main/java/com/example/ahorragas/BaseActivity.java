package com.example.ahorragas;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected void navigateToMap() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    protected void navigateToPreferences() {
        startActivity(new Intent(this, PreferencesActivity.class));
    }

    protected void navigateToDistanceList() {
        startActivity(new Intent(this, DistanceListActivity.class));
    }
}