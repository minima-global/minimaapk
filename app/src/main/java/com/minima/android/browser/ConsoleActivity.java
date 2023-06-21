package com.minima.android.browser;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.minima.android.R;

public class ConsoleActivity extends AppCompatActivity {

    String mConsoleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_console);

        Toolbar mToolBar = findViewById(R.id.minidapp_toolbar);
        setSupportActionBar(mToolBar);

        setTitle("Console Logs");

        //Now get the text
        mConsoleText = getIntent().getStringExtra("consoletext");

        //Set up TextView
        TextView tv = findViewById(R.id.console_text);
        //tv.setText(mConsoleText);
        tv.setText(Html.fromHtml(mConsoleText,Html.FROM_HTML_MODE_COMPACT));
        tv.setTextIsSelectable(true);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.consolemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.console_share:

                //Share the text
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Console Logs");
                sendIntent.putExtra(Intent.EXTRA_TEXT, mConsoleText);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}