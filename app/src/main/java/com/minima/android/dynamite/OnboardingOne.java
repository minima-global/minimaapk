package com.minima.android.dynamite;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.transition.Slide;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.minima.android.MainActivity;
import com.minima.android.R;
import com.minima.android.dynamite.HelperClasses.SliderAdapter;
import com.minima.android.service.MinimaService;

import org.minima.utils.MinimaLogger;

public class OnboardingOne extends AppCompatActivity implements ServiceConnection {

    ViewPager viewPager;
    LinearLayout dotsLayout;
    SliderAdapter sliderAdaptor;
    TextView[] dots;

    boolean mFromBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_one);

        //Is this from Boot
        mFromBoot = getIntent().getBooleanExtra("FROMBOOT",true);

        MinimaLogger.log("INTRO FROMBOOT : "+mFromBoot);

        //Start the Minima Service..
        if(mFromBoot) {
            Intent minimaintent = new Intent(getBaseContext(), MinimaService.class);
            startForegroundService(minimaintent);
            bindService(minimaintent, this, Context.BIND_AUTO_CREATE);
        }

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

                if(mFromBoot) {
                    Intent intent = new Intent(view.getContext(), MainActivity.class);
                    view.getContext().startActivity(intent);
                }

                //Close the old app..
                finish();
            }
        });
    }

    public void addDots(int position) {
        dots = new TextView[6];
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

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {}

    @Override
    public void onServiceDisconnected(ComponentName componentName) {}
}