package com.minima.android.dynamite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.R;

public class OnboardingThree extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_three);

        Button btn = (Button) findViewById(R.id.get_started);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), OnboardingFour.class);
                view.getContext().startActivity(intent);
            }
        });
    }
}
