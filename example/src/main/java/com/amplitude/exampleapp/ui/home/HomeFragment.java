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
import com.amplitude.experiment.Variant;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getVariant().observe(getViewLifecycleOwner(), new Observer<Variant>() {
            @Override
            public void onChanged(@Nullable Variant variant) {
                String text = "Variant: " + variant +
                    "\n\nUser: " + homeViewModel.getUser();
                textView.setText(text);
            }
        });
        final Button button = root.findViewById(R.id.button);
        button.setOnClickListener(v -> {homeViewModel.refresh();});
        return root;
    }
}
