package com.minima.android.dynamite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.MainActivity;

public class OnboardingTwo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(com.minima.android.R.layout.activity_onboarding_two);

        Button btn = (Button) findViewById(com.minima.android.R.id.get_started);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), MainActivity.class);
                view.getContext().startActivity(intent);

                finish();
            }
        });
    }
}