package com.minima.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.dynamite.OnboardingOne;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if we need to display our Onboarding
        if (!sharedPreferences.getBoolean("FIRST_RUN", true)) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

        }else{

//            SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
//            sharedPreferencesEditor.putBoolean("FIRST_RUN", false);
//            sharedPreferencesEditor.apply();

            Intent intent = new Intent(this, OnboardingOne.class);
            startActivity(intent);
        }

        finish();
    }
}