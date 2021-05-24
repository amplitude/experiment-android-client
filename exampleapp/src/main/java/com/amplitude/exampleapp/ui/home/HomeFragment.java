package com.amplitude.exampleapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.amplitude.exampleapp.R;
import com.amplitude.skylab.Skylab;
import com.amplitude.skylab.SkylabClient;
import com.amplitude.skylab.SkylabUser;
import com.amplitude.skylab.Variant;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        final Button button = root.findViewById(R.id.button);
        button.setOnClickListener(v -> {
            SkylabUser user = SkylabUser.builder()
                    .copyUser(Skylab.getInstance().getUser())
                    .setUserProperty("newProperty", "value")
                    .build();
            Future<SkylabClient> future = Skylab.getInstance().setUser(user);
            new Thread(() -> {
                try {
                    final SkylabClient client = future.get();
                    v.post(() -> {
                        Variant variant = client.getVariant("android-demo");
                        homeViewModel.setText("Variant: " + variant.toJson() +
                                "\nUser: " + client.getUser().toJSONObject().toString());
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }).start();
        });
        return root;
    }
}
