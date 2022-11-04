package com.example.activity_assistant_logger;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import static android.Manifest.permission.INTERNET;

public class MainActivity extends AppCompatActivity{
    private static final int PERMISSION_REQUEST_CODE=200;
    private Controller controller;
    private BottomNavigationView bottomNavigation;
    // debug below
    final Fragment mHomeFragment = new HomeFragment();
    final Fragment mWeekFragment = new WeekFragment();
    final FragmentManager mFragmentManager = getSupportFragmentManager();
    Fragment mActiveFragment = mHomeFragment;

//    public void openFragment(Fragment fragment) {
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.replace(R.id.container, fragment);
//        transaction.addToBackStack(null);
//        transaction.commit();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener(){
                    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item){
                        switch (item.getItemId()){
                            case R.id.navigation_home:
                                //openFragment(HomeFragment.newInstance("", ""));
                                mFragmentManager.beginTransaction()
                                                .hide(mActiveFragment)
                                                .show(mHomeFragment)
                                                .commit();
                                mActiveFragment = mHomeFragment;
                                return true;
                            case R.id.navigation_weekview:
                                mFragmentManager.beginTransaction()
                                                .hide(mActiveFragment)
                                                .show(mWeekFragment)
                                                .commit();
                                mActiveFragment = mWeekFragment;
                                return true;
                        }
                        return false;
                    }
                }
        );

        NotificationHandler.createNotificationChannel(this);
        this.requestPermissions();

        controller = new ViewModelProvider(this, new ControllerFactory(
                this.getApplication(), MainActivity.this)).get(Controller.class);

        // Show home fragment and hide all others
        mFragmentManager.beginTransaction()
                        .add(R.id.container, mWeekFragment, "week")
                        .hide(mWeekFragment)
                        .commit();
        mFragmentManager.beginTransaction()
                        .add(R.id.container, mHomeFragment, "home")
                        .commit();
        //openFragment(HomeFragment.newInstance("", ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void requestPermissions(){
        if(ActivityCompat.checkSelfPermission(MainActivity.this, INTERNET)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{INTERNET},
                    PERMISSION_REQUEST_CODE);
        }

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{INTERNET},
                PERMISSION_REQUEST_CODE);
    }


    public void createToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNewIntent(Intent intent){
        /** is called for every opening of the main activity
         *  is used for getting a notification
         *  TODO only works if application was totally minimized
         */
        super.onNewIntent(intent);
        try{
            if (NotificationHandler.isStartedFromNotification(intent)) {
                controller.openedFromNotification(
                        intent.getStringExtra("currentActivity"));
            }
        }catch (Exception e){
            createToast("Could not recreate HomeFragment ");
            createToast(e.toString());
        }
    }


}
