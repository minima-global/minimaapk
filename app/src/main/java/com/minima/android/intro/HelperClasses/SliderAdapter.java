package com.minima.android.intro.HelperClasses;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.minima.android.R;

public class SliderAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;

    public SliderAdapter(Context context) {
        this.context = context;
    }

    int images[] = {
            R.drawable.ic_minima,
            R.drawable.ic_network,
            R.drawable.ic_dapps,
            R.drawable.ic_node,
            R.drawable.ic_freedom,
            R.drawable.ic_transfer,
    };

    int descriptions[] = {
            R.string.onboarding_slide_description_one,
            R.string.onboarding_slide_description_two,
            R.string.onboarding_slide_description_three,
            R.string.onboarding_slide_description_four,
            R.string.onboarding_slide_description_five,
            R.string.onboarding_slide_description_six,
    };

    @Override
    public int getCount() {
        return descriptions.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (ConstraintLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slides_layout,container,false);

        ImageView imageView = view.findViewById(R.id.image);
        TextView textView = view.findViewById(R.id.description);

        imageView.setImageResource(images[position]);
        textView.setText(descriptions[position]);

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout)object);
    }
}
