package com.minima.android.ui.maxima;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MaximaViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public MaximaViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the Maxima fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}