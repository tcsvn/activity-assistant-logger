package com.example.activity_assistant_logger;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;

/** This is where the apps logic happens
*
*
**/
public class Controller {
    final static String SERVER_STATUS_ONLINE = "online";
    final static String SERVER_STATUS_UNCONFIGURED = "unconfigured";
    final static String SERVER_STATUS_OFFLINE = "not reachable";
    final static String DEVICE_STATUS_REGISTERED = "registered";
    final static String DEVICE_STATUS_UNCONFIGURED = "unconfigured";
    final String STATE_INITIAL = "initial";

    private String deviceState = STATE_INITIAL;
    private String currentActivity;
    private boolean outOfSync=true;
    private ActAssistApi actAssist;
    private ConfigHandler data;
    private ActivityFileHandler activityFile;
    private MainActivity mainact;


    public Controller(MainActivity mainact) {
        this.mainact = mainact;
        this.data = new ConfigHandler();
        this.activityFile = new ActivityFileHandler(mainact.getApplicationContext());

        if (data.configExists(mainact.getApplicationContext())) {
            loadFromConfig();
        } else {
            resetConfig();
        }
    }

//__Methods__---------------------------------------------------------------------------------------
    private void loadFromConfig(){
         try {
            this.actAssist = data.loadActAssistFromFile(mainact.getApplicationContext(), this);
        } catch (JSONException e) {
            Toast.makeText(mainact, "couldn't load server config to file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException e) {
            Toast.makeText(mainact, "couldn't load server config to file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        mainact.setServerStatus(SERVER_STATUS_OFFLINE);
        mainact.setDeviceStatus(DEVICE_STATUS_REGISTERED);
        this.deviceState = DEVICE_STATUS_REGISTERED;
    }

    private void resetConfig(){
        this.actAssist = null;
        mainact.setServerStatus(SERVER_STATUS_UNCONFIGURED);
        mainact.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
        this.deviceState = DEVICE_STATUS_UNCONFIGURED;
        mainact.setSwitchLogging(false);
        mainact.resetSpinnerLists();
    }

//__GETTER/SETTER__---------------------------------------------------------------------------------
   public boolean isActAssistConfigured() {
        return actAssist != null;
   }

   public boolean deviceHasActivity() {
        return false;
        //return actAssist.deviceHasActivity();
   }

   public String getDeviceActivityName() {
        return "debug";
        //return actAssist.getDeviceActivityName();
   }

//___HTTP__Callbacks -------------------------------------------------------------------------------
    public void onFailure(String str){
        Toast.makeText(mainact, str, Toast.LENGTH_SHORT).show();
        mainact.setServerStatus(SERVER_STATUS_OFFLINE);
    }

    public void onSuccess(String str){
        Toast.makeText(mainact, str, Toast.LENGTH_SHORT).show();
        mainact.setServerStatus(SERVER_STATUS_ONLINE);
    }

    public void onGetActivitiesSuccess(ArrayList<String> activities){
        mainact.setReloadSpinnerActivity(activities);
        mainact.setServerStatus(SERVER_STATUS_ONLINE);
    }

//__GUI Callbacks__---------------------------------------------------------------------------------
    public void onCreate(){
        // executed when main activity starts
        mainact.resetSpinnerLists();
        // START DEBUG
        // code to manually delete config file
        //File dir = getFilesDir();
        //File file = new File(dir, CONNECTION_FILE_NAME);
        //boolean deleted = file.delete();
        //System.exit(0);
        // END DEBUG
    }

    public void switchLoggingToggled(boolean turnedOn){
        /** log activity to file if an experiment is conducted
         *
         */
        if(turnedOn){
            actAssist.setSmartphoneLogging(true);
            mainact.createNotification();
            if (actAssist.isExperimentConducted()){
                actAssist.putSmartphoneAPI();
                try {
                    String selectedActivity = mainact.getSelectedActivity();
                    activityFile.createActivity(
                            mainact.getApplicationContext(),
                            selectedActivity
                            );
                    currentActivity = selectedActivity;
                }catch (Exception e){
                    Toast.makeText(mainact, "sth went wrong writing activity file",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        else{
            actAssist.setSmartphoneLogging(false);
            mainact.removeNotification();
            if (actAssist.isExperimentConducted()){
                actAssist.putSmartphoneAPI();
                try {
                    activityFile.finishActivity(
                            mainact.getApplicationContext(),
                            mainact.getSelectedActivity());
                    currentActivity = null;
                }catch (Exception e){
                    Toast.makeText(mainact, "sth went wrong writing activity file",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void receivedDataFromQRCode(JSONObject jsonObject){
        /** try to create an activity assistant api
        * */
        try {
            this.actAssist = new ActAssistApi(this,
                    jsonObject.getString(ActAssistApi.URL_API),
                    jsonObject.getInt(ActAssistApi.SMARTPHONE_ID),
                    jsonObject.getString(ActAssistApi.URL_PERSON),
                    jsonObject.getString(ActAssistApi.USERNAME),
                    jsonObject.getString(ActAssistApi.PASSWORD)
                    );
            this.deviceState = DEVICE_STATUS_REGISTERED;
            mainact.setDeviceStatus(DEVICE_STATUS_REGISTERED);
            mainact.setServerStatus(SERVER_STATUS_OFFLINE);
            try{
                data.dumpActAssistToFile(mainact.getApplicationContext(), actAssist);
            }catch (IOException e){
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onActivitySelected(String selectedActivity){
        /** log activity to file if an experiment is conducted
        * */
        if (mainact.getSwitchChecked() && actAssist.isExperimentConducted()){
            try{
                activityFile.addActivity(
                    mainact.getApplicationContext(),
                    currentActivity,
                    selectedActivity
                    );
                currentActivity = selectedActivity;
            }catch (Exception e) {
                Toast.makeText(mainact, "sth went wrong writing activity file",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onBtnScanQRCode(){
        /** starts to scan
         *
         */
        Intent intent = new Intent(mainact, BarcodeCaptureActivity.class);
        mainact.startActivityForResult(intent, 0);
    }

    public void onBtnShowActFileView(){
        Intent intent = new Intent(mainact, DisplayFileActivity.class);
        mainact.startActivityForResult(intent, 0);
    }

    public void onBtnDecouple(){
        /** first try to delete the device on the server
         * if the server is unreachable wait
         * if the server is reachable post a deleteion'
         *      if the device is already unregistered
         *          proceed with deletion local files
         */
        this.deviceState = DEVICE_STATUS_UNCONFIGURED;
        mainact.setServerStatus(SERVER_STATUS_UNCONFIGURED);
        mainact.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
        mainact.setSwitchLogging(false);
        mainact.resetSpinnerLists();
        this.actAssist = null;

        // wipe data
        this.data.deleteConfigFile(mainact.getApplicationContext());
        this.activityFile.deleteActivityFile(mainact.getApplicationContext());
    }

    public void onBtnSynchronize(){
        /** synchronize cached data files
         * */
        if (this.deviceState.equals(DEVICE_STATUS_REGISTERED)){
            // todo make a ping request
            actAssist.getSmartphoneAPI();
            actAssist.getActivitiesAPI();
            try {
                // TODO do this when the activities where received from server
                data.dumpActAssistToFile(
                        mainact.getApplicationContext(),
                        actAssist);
            }catch (Exception e){
                // TODO do sth if this fails

            }
        }
    }
}
