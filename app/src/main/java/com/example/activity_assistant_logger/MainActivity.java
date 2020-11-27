package com.example.activity_assistant_logger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
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
import com.google.gson.JsonObject;

import static android.Manifest.permission.INTERNET;
import static com.example.activity_assistant_logger.Controller.SERVER_STATUS_ONLINE;

public class MainActivity extends AppCompatActivity implements MySpinner.OnItemSelectedListener {
    Button qrcode_scan;
    TextView qrcode_text;
    TextView deviceStatus;
    TextView serverStatus;
    MySpinner mySpinner_activity;
    private final Boolean DEBUG = true;
    private Switch switch_logging;
    private String device_status;
    private String CHANNEL_ID = "0";
    private String server_status;
    private Runnable runnable_check_server_status;
    private static final int PERMISSION_REQUEST_CODE=200;
    public static final String SPINNER_INITIAL_VAL = "unconfigured";
    private Controller controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        qrcode_scan = findViewById(R.id.btn_scan_qrcode);
        qrcode_text = findViewById(R.id.textView_qrcode);
        deviceStatus =findViewById(R.id.device_status);
        serverStatus =findViewById(R.id.server_status);

        switch_logging = findViewById(R.id.switch_logging);
        mySpinner_activity = findViewById(R.id.spinner_activity);
        mySpinner_activity.setOnItemSelectedListener(this);

        createNotificationChannel();
        this.requestPermissions();
        controller = new Controller(MainActivity.this, getIntent());
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
    public void createNotification(){
        // define click behaviour
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("currentActivity", getSelectedActivity());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
                //intent, PendingIntent.FLAG_IMMUTABLE);
        // define notification
        String temp_text = getSelectedActivity() + " " + getString(R.string.notification_text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_account_circle_24)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(temp_text)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent);
        builder.setOngoing(true);
        // publish notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0,builder.build());
    }

    public void createNotificationChannel(){
        // create notificationchannel
        CharSequence name = getString(R.string.channel_name);
        String desc = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(desc);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public void removeNotification(){
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(0);
        }
        catch (Exception e){ }
    }

    public void createToast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNewIntent(Intent intent){
        /** is called for every opening of the main activity
         *  is used for getting a notification
         */
        super.onNewIntent(intent);
        try{
            if (isStartedFromNotification(intent)) {
                controller.openedFromNotification(
                        intent.getStringExtra("currentActivity"));
            }
        }catch (Exception e){

        }
    }

    public boolean isStartedFromNotification(Intent intent){
        /** returns whether the main acitivity was started by pressing
         * on a new notification
         * */
        String curAct = intent.getStringExtra("currentActivity");
        return curAct != null;
    }

//__GETTER/SETTER----------------------------------------------------------------------------------
    public String getSelectedActivity(){
        return mySpinner_activity.getSelectedItem().toString();
    }

    public void setServerStatus(String new_status){
        /* sets the text and color of the server entry in status
         * */
        serverStatus.setText(new_status);

        // set the according color
        if(new_status == SERVER_STATUS_ONLINE){
            serverStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor));
        }
        else {
            serverStatus.setTextColor(Color.parseColor("#d3d3d3"));
        }
    }

    public void setSwitchLogging(Boolean val){
        if (val){
            createNotification();
        }
        else{
            removeNotification();
        }
        switch_logging.setChecked(val);
    }

    public void setDeviceStatus(String new_status){
        device_status = new_status;
        deviceStatus.setText(new_status);
        // set the color
        if(new_status == Controller.DEVICE_STATUS_REGISTERED){
            deviceStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor));
            qrcode_text.setText("");
            qrcode_scan.setText("decouple");
        }
        else if (new_status == Controller.DEVICE_STATUS_UNCONFIGURED){
            // set color to orange
            deviceStatus.setTextColor(Color.parseColor("#d3d3d3"));
            //deviceStatus.setTextColor(Color.parseColor("#ff5722"));
            qrcode_scan.setText("scan");
            qrcode_text.setText("qrcode");
        }
    }

    public boolean getSwitchChecked(){
        return switch_logging.isChecked();
    }

//__Spinner__--------------------------------------------------------------------------------------
    public void reloadSpinners(){
        setReloadSpinnerActivity(mySpinner_activity.getItemList());
    }

    public void setReloadSpinnerActivity(ArrayList<String> spinnerArray){
         ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerArray
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner_activity.setAdapter(adapter);
        // TODO check what this does
        //      I think this was to set the activity to from the server. Is this still needed though?
        //if(controller.isActAssistConfigured()
        //        && controller.deviceHasActivity()
        //        && containsElement(adapter, controller.getDeviceActivityName())){
        //    int pos = adapter.getPosition(controller.getDeviceActivityName());
        //    mySpinner_activity.programmaticallySetPosition(pos);
        //}
    }

    public void setSpinnerActivity(ArrayList<String> activityArray, String currentActivity){
          ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                  activityArray
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner_activity.setAdapter(adapter);
        int pos = adapter.getPosition(currentActivity);
        mySpinner_activity.programmaticallySetPosition(pos);
    }

    public boolean containsElement(ArrayAdapter<String> adapter, String element){
        for(int i =0; i < adapter.getCount(); i++){
            if (adapter.getItem(i).equals(element)){
                return true;
            }
        }
        return false;
    }

    public void resetSpinnerLists(){
        /** deletes all items in the spinner and sets it to unconfigured
        * */
        setReloadSpinnerActivity(new ArrayList<String>(){{ add(SPINNER_INITIAL_VAL); }});
    }


//__Callbacks__--------------------------------------------------------------------------------
    public void btnShowActFileView(View view){
        controller.onBtnShowActFileView();
    }

    public void btnSynchronize(View view){
        controller.onBtnSynchronize();
    }

    public void btnScanQrCode(View view) {
        String btntext = (String) qrcode_scan.getText();
        if( btntext.equals("scan")){
            if (!DEBUG){
                controller.onBtnScanQRCode();
            }
            else {
                // DEBUG FROM HERE
                JSONObject obj = new JSONObject();
                try {
                    obj.put(ActAssistApi.URL_API,
                            "http://192.168.178.47:8000/api/v1/");
                    obj.put(ActAssistApi.SMARTPHONE_ID,
                            1);
                    obj.put(ActAssistApi.URL_PERSON,
                            "persons/1/");
                    obj.put(ActAssistApi.USERNAME,
                            "admin");
                    obj.put(ActAssistApi.PASSWORD,
                            "asdf");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                controller.receivedDataFromQRCode(obj);
                // DEBUG END
            }
        }
        else{
            controller.onBtnDecouple();
        }
    }

    public void switchLogging(View view){
        controller.switchLoggingToggled(switch_logging.isChecked());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
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
                     controller.receivedDataFromQRCode(
                             new JSONObject(connectionInformation)
                     );

                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
             }catch (Exception e){
                String currentActivity = data.getStringExtra("currentActivity");
                createToast("got forom notification: " + currentActivity);
             }
         }
         else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id, boolean userSelected) {
        // only update the model if the user selected the stuff
        String selectedActivity = mySpinner_activity.getSelectedItem().toString();
        if (userSelected && selectedActivity != SPINNER_INITIAL_VAL) {
            controller.onActivitySelected(selectedActivity);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
