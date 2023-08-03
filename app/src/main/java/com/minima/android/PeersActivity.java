package com.minima.android;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class PeersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_peers);

        Toolbar mToolBar = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(mToolBar);

        setTitle("Minima Peers");
    }
}