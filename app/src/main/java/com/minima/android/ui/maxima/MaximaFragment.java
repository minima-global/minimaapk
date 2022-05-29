package com.minima.android.ui.maxima;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.minima.android.R;

import java.util.ArrayList;

public class MaximaFragment extends Fragment {

    ListView mMainList;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_maxima, container, false);

        mMainList = root.findViewById(R.id.list_maxima);

        ArrayList<String> items = new ArrayList<>();
        items.add("Hello");
        items.add("You");
        items.add("There");
        String[] it = items.toArray(new String[0]);

        ArrayAdapter<String> itemsAdapter;
        itemsAdapter = new ArrayAdapter<String>(root.getContext(), android.R.layout.simple_list_item_1, it);

        mMainList.setAdapter(itemsAdapter);

        FloatingActionButton fab = root.findViewById(R.id.fab_maxima);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        return root;

//        MaximaViewModel maximaViewModel =
//                new ViewModelProvider(this).get(MaximaViewModel.class);
//
//        binding = FragmentMaximaBinding.inflate(inflater, container, false);
//        View root = binding.getRoot();
//
//        final TextView textView = binding.textMaxima;
//        maximaViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
//        return root;
    }

//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding = null;
//    }
}