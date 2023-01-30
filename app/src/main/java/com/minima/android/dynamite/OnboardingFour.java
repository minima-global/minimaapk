package com.minima.android.dynamite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;

import com.minima.android.R;

public class OnboardingFour extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_four);

        CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);
        Button btn = (Button) findViewById(R.id.get_started);

        btn.setEnabled(false);
        btn.setBackgroundColor(getResources().getColor(R.color.disabledBlack));

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), OnboardingFive.class);
                view.getContext().startActivity(intent);
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btn.setEnabled(isChecked);

                if (isChecked) {
                    btn.setBackgroundColor(getResources().getColor(R.color.black));
                } else {
                    btn.setBackgroundColor(getResources().getColor(R.color.disabledBlack));
                }
            }
        });
    }
}
