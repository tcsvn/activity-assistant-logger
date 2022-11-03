package com.example.activity_assistant_logger;

import android.app.Application;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.activity_assistant_logger.Controller;

public class ControllerFactory implements ViewModelProvider.Factory {
    private Application mApplication;
    private MainActivity mMainActivity;

    public ControllerFactory(Application application, MainActivity mainActivity) {
        mApplication = application;
        mMainActivity = mainActivity;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new Controller(mApplication, mMainActivity);
    }
}
