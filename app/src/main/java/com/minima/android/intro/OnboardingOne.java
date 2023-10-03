package com.minima.android.intro;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.minima.android.R;
import com.minima.android.StartMinimaActivity;
import com.minima.android.intro.HelperClasses.SliderAdapter;

public class OnboardingOne extends AppCompatActivity {

    ViewPager viewPager;
    LinearLayout dotsLayout;
    SliderAdapter sliderAdaptor;
    TextView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_one);

        viewPager = findViewById(R.id.slider);
        dotsLayout = findViewById(R.id.dots);

        sliderAdaptor = new SliderAdapter(this);
        viewPager.setAdapter(sliderAdaptor);

        addDots(0);
        viewPager.addOnPageChangeListener(changeListener);

        Button btn = (Button) findViewById(R.id.get_started);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(view.getContext(), StartMinimaActivity.class);
                view.getContext().startActivity(intent);

                //Close the old app..
                finish();
            }
        });

        //Get Files Permission
        String[] perms = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS
        };
        checkPermission(perms,99);
    }

    // Function to check and request permission
    public void checkPermission(String[] permissions, int requestCode){

        //Check all the requested permissions
        boolean allok = true;
        for(String perm : permissions){
            if(ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED){
                allok = false;
                break;
            }
        }

        //Ask for all the permissions
        if(!allok){
            ActivityCompat.requestPermissions(this, permissions , requestCode);
        }
    }

    public void addDots(int position) {
        dots = new TextView[7];
        dotsLayout.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#9724;"));
            dots[i].setPadding(15, 0, 15, 0);
            dots[i].setTextColor(getResources().getColor(R.color.secondaryGrey));
            dots[i].setTextSize(16);

            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[position].setTextColor(getResources().getColor(R.color.grey));
        }
    }

    ViewPager.OnPageChangeListener changeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageSelected(int position) {
            addDots(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}