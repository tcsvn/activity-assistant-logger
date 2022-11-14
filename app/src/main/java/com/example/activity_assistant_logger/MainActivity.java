package com.example.activity_assistant_logger;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import static android.Manifest.permission.INTERNET;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity{
    private static final int PERMISSION_REQUEST_CODE=200;
    private Controller controller;
    private BottomNavigationView bottomNavigation;
    final Fragment mHomeFragment = new HomeFragment();
    final Fragment mWeekFragment = new WeekFragment();
    final FragmentManager mFragmentManager = getSupportFragmentManager();
    Fragment mActiveFragment = mHomeFragment;


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
        /** Is called for every opening of the main activity
         *  is used for getting a notification
         *  TODO only works if application was minimized and not completely removed
         */
        createToast("DEBUG: Intent 1");
        super.onNewIntent(intent);
        createToast("DEBUG: Intent 2");
        try{
            if (NotificationHandler.isStartedFromNotification(intent)) {
                createToast("DEBUG: Intent 2");
                controller.openedFromNotification(
                        intent.getStringExtra("currentActivity"));
            }
        }catch (Exception e){
            createToast("Could not recreate HomeFragment ");
            createToast(e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        /** The BarcodeCaptureActivity scanned sth. leads to calling this method
         * also the ActivityFile upon return leads to calling this but nothing is done as
         * the Intent data is null
         * */
        if (resultCode == CommonStatusCodes.SUCCESS && data!= null) {
             try {
                 // case of the activity with intent
                 Barcode barcode = data.getParcelableExtra("barcode");
                 String connectionInformation = barcode.displayValue;
                 try {
                     controller.onDataFromQRCode(
                             new JSONObject(connectionInformation)
                     );

                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
             }catch (Exception e){
                String currentActivity = data.getStringExtra("currentActivity");
                createToast("got from notification: " + currentActivity);
             }
             return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
