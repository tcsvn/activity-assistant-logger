package com.example.activity_assistant_logger;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import static android.Manifest.permission.INTERNET;
import static com.example.activity_assistant_logger.Controller.SERVER_STATUS_ONLINE;

public class MainActivity extends AppCompatActivity{
    Button qrcode_scan;
    TextView qrcode_text;
    TextView deviceStatus;
    TextView serverStatus;
    MySpinner mySpinner_activity;
    private final Boolean DEBUG = false;
    private Switch switch_logging;
    private String device_status;
    private String CHANNEL_ID = "0";
    private String server_status;
    private Runnable runnable_check_server_status;
    private static final int PERMISSION_REQUEST_CODE=200;
    public static final String SPINNER_INITIAL_VAL = "unconfigured";
    private Controller controller;
    private BottomNavigationView bottomNavigation;
    private static final int TYPE_DAY_VIEW = 1;
    private static final int TYPE_THREE_DAY_VIEW = 2;

    public void openFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener(){
                    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item){
                        switch (item.getItemId()){
                            case R.id.navigation_home:
                                openFragment(HomeFragment.newInstance("", ""));
                                return true;
                            case R.id.navigation_weekview:
                                openFragment(WeekFragment.newInstance("", ""));
                                return true;
                        }
                        return false;
                    }
                }
        );

        //createNotificationChannel();
        this.requestPermissions();
        controller = new ViewModelProvider(this, new ControllerFactory(
                this.getApplication(), MainActivity.this)).get(Controller.class);
        //controller = new ViewModelProvider(this).get(Controller.class);
        //controller = new Controller(MainActivity.this, getIntent());

        openFragment(HomeFragment.newInstance("", ""));
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

//__Notification__----------------------------------------------------------------------------------
    //public void createNotification(){
    //    // define click behaviour
    //    Intent intent = new Intent(this, MainActivity.class);
    //    intent.putExtra("currentActivity", getSelectedActivity());
    //    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
    //            intent, PendingIntent.FLAG_UPDATE_CURRENT);
    //            //intent, PendingIntent.FLAG_IMMUTABLE);
    //    // define notification
    //    String temp_text = getSelectedActivity() + " " + getString(R.string.notification_text);
    //    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
    //            .setSmallIcon(R.drawable.ic_baseline_account_circle_24)
    //            .setContentTitle(getString(R.string.notification_title))
    //            .setContentText(temp_text)
    //            .setOngoing(true)
    //            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    //            .setContentIntent(contentIntent);
    //    builder.setOngoing(true);
    //    // publish notification
    //    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    //    notificationManager = NotificationManagerCompat.from(this);
    //    notificationManager.notify(0,builder.build());
    //}

    //public void createNotificationChannel(){
    //    // create notificationchannel
    //    CharSequence name = getString(R.string.channel_name);
    //    String desc = getString(R.string.channel_description);
    //    int importance = NotificationManager.IMPORTANCE_LOW;
    //    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
    //    channel.setDescription(desc);
    //    NotificationManager notificationManager = getSystemService(NotificationManager.class);
    //    notificationManager.createNotificationChannel(channel);
    //}

    //public void removeNotification(){
    //    try {
    //        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    //        notificationManager.cancel(0);
    //    }
    //    catch (Exception e){ }
    //}

    public void createToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    //@Override
    //public void onNewIntent(Intent intent){
    //    /** is called for every opening of the main activity
    //     *  is used for getting a notification
    //     */
    //    super.onNewIntent(intent);
    //    try{
    //        if (isStartedFromNotification(intent)) {
    //            controller.openedFromNotification(
    //                    intent.getStringExtra("currentActivity"));
    //        }
    //    }catch (Exception e){

    //    }
    //}

    //public boolean isStartedFromNotification(Intent intent){
    //    /** returns whether the main acitivity was started by pressing
    //     * on a new notification
    //     * */
    //    String curAct = intent.getStringExtra("currentActivity");
    //    return curAct != null;
    //}


}
