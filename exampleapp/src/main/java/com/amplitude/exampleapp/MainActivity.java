package com.amplitude.exampleapp;

import android.os.Bundle;
import android.widget.TextView;

import com.amplitude.skylab.Skylab;
import com.amplitude.skylab.SkylabClient;
import com.amplitude.skylab.Variant;
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
        Future<SkylabClient> refetchFuture = Skylab.getInstance().refetchAll();
        TextView tv = (TextView)findViewById(R.id.text_home);
        new Thread(() -> {
            try {
                final SkylabClient client = refetchFuture.get();
                runOnUiThread(() -> {
                    Variant variant = client.getVariant("android-demo");
                    tv.setText("Variant: " + variant.toJson() +
                            "\nUser: " + client.getUser().toJSONObject().toString());
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
