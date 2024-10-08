package com.amplitude.exampleapp;

import android.os.Bundle;
import android.widget.TextView;

import com.amplitude.experiment.ExperimentClient;
import com.amplitude.experiment.Variant;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView navView;

    final ExperimentClient client = ExampleApplication.experimentClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Future<ExperimentClient> fetch = client.fetch(null);
        TextView tv = findViewById(R.id.text_home);
        new Thread(() -> {
            try {
                final ExperimentClient client = fetch.get();
                runOnUiThread(() -> {
                    Variant variant = client.variant("android-demo", null);
                    String str = "Variant: " + variant + "\n\nUser: " + client.getUser();
                    tv.setText(str);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
