package com.amplitude.exampleapp.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.amplitude.exampleapp.ExampleApplication;
import com.amplitude.experiment.ExperimentClient;
import com.amplitude.experiment.ExperimentUser;
import com.amplitude.experiment.Variant;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HomeViewModel extends ViewModel {

    private static final String KEY = "android-demo";

    private final MutableLiveData<Variant> mVariant;

    private final ExperimentClient client = ExampleApplication.experimentClient;

    public HomeViewModel() {
        mVariant = new MutableLiveData<>();
        setVariant(client.variant(KEY, null));
    }

    public LiveData<Variant> getVariant() {
        return mVariant;
    }

    public void setVariant(Variant text) {
        mVariant.postValue(text);
    }

    public ExperimentUser getUser() {
        return client.getUser();
    }

    public void refresh() {
        ExperimentUser user = client.getUser().copyToBuilder()
            .userProperty("newProperty", "value")
            .build();
        final Future<ExperimentClient> future = client.fetch(user);
        new Thread(() -> {
            try {
                final ExperimentClient client = future.get();
                final Variant variant = client.variant(KEY, null);
                setVariant(variant);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
