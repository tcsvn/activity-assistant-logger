package com.example.activity_assistant_logger;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;
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
import java.util.List;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;
import com.example.activity_assistant_logger.actassistapi.Activity;
import com.example.activity_assistant_logger.actassistapi.Smartphone;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.ResponseBody;

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
            mainact.setReloadSpinnerActivity((ArrayList<String>) actAssist.getActivities());
            mainact.setServerStatus(SERVER_STATUS_OFFLINE);
            mainact.setDeviceStatus(DEVICE_STATUS_REGISTERED);
            deviceState = DEVICE_STATUS_REGISTERED;
        } catch (JSONException e) {
            Toast.makeText(mainact, "couldn't load server config to file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            resetConfig();
        } catch (IOException | ClassNotFoundException e) {
            Toast.makeText(mainact, "couldn't load server config to file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            resetConfig();
        }

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
        //mainact.resetSpinnerLists();
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
        if(deviceState == DEVICE_STATUS_REGISTERED) {
            if (turnedOn) {
                //actAssist.setSmartphoneLogging(true);
                mainact.createNotification();
                if (actAssist.isExperimentConducted()) {
                    //actAssist.putSmartphoneAPI();
                    try {
                        String selectedActivity = mainact.getSelectedActivity();
                        activityFile.createActivity(
                                mainact.getApplicationContext(),
                                selectedActivity
                        );
                        currentActivity = selectedActivity;
                    } catch (Exception e) {
                        Toast.makeText(mainact, "sth went wrong writing activity file",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                //actAssist.setSmartphoneLogging(false);
                mainact.removeNotification();
                if (actAssist.isExperimentConducted()) {
                    //actAssist.putSmartphoneAPI();
                    try {
                        activityFile.finishActivity(
                                mainact.getApplicationContext(),
                                mainact.getSelectedActivity());
                        currentActivity = null;
                    } catch (Exception e) {
                        Toast.makeText(mainact, "sth went wrong writing activity file",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        else{
            mainact.setSwitchLogging(false);
        }
    }

    public void receivedDataFromQRCode(JSONObject jsonObject){
        /** creates an activity assistant api
         * if there is already a smartphone registered on the api
         * copy all data from the smartphone of the api including downloading the activity file
         * if there is no smartphone registered on the api
         * then create an empty smartphone
         * - also get activities
        * */
        try {
            String urlApi = jsonObject.getString(ActAssistApi.URL_API);
            this.actAssist = new ActAssistApi(this,
                    urlApi,
                    jsonObject.getString(ActAssistApi.USERNAME),
                    jsonObject.getString(ActAssistApi.PASSWORD)
                    );


            // in the decoded message there is a smartphone url present
            // if already an instance exists
            boolean smAlreadyCreated = false;
            try{
                String tmp = jsonObject.getString("smartphone_url");
                smAlreadyCreated=true;
            }
            catch(Exception e){
                smAlreadyCreated=false;
            }
            if (smAlreadyCreated){
                // TODO implement
                //actAssist.getSmartphoneAndActivties()
            }
            else {
                Smartphone sm = actAssist.createNewSmartphone(
                    urlApi +
                    jsonObject.getString(ActAssistApi.URL_PERSON));
                actAssist.createSmartphoneAndGetActivties(sm)
                        .subscribe(new SingleObserver<Pair<List<Activity>, Smartphone>>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {

                            }

                            @Override
                            public void onSuccess(@NonNull Pair<List<Activity>, Smartphone> listSmartphonePair) {
                                actAssist.setActivities(listSmartphonePair.first);
                                actAssist.setSmartphone(listSmartphonePair.second);
                                try {
                                    data.dumpActAssistToFile(
                                            mainact.getApplicationContext(),
                                            actAssist);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // TODO do sth if this fails

                                }
                                mainact.setReloadSpinnerActivity((ArrayList<String>) actAssist.getActivities());
                                mainact.setServerStatus(SERVER_STATUS_ONLINE);
                                deviceState = DEVICE_STATUS_REGISTERED;
                                mainact.setDeviceStatus(DEVICE_STATUS_REGISTERED);
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                mainact.setServerStatus(SERVER_STATUS_OFFLINE);
                                deviceState = DEVICE_STATUS_UNCONFIGURED;
                                mainact.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
                            }
                        });
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
                mainact.createToast("sth went wrong writing activity file");
            }
        }
    }

    public void onBtnScanQRCode(){
        /** starts to scanning activityi */
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
         * first gets a smartphone and checks if it is in push mode or not
         * if it is in push mode pushes smartphone and activity file to server
         * if it is not synchronized delete local activity file and
         * request new activities and pull activity file from server
         * */
        if (this.deviceState.equals(DEVICE_STATUS_REGISTERED)){
            actAssist.getSmartphoneAndActivties()
                    .subscribe(new SingleObserver<Pair<List<Activity>, Smartphone>>() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {

                }

                @Override
                public void onSuccess(@NonNull Pair<List<Activity>, Smartphone> listSmartphonePair) {
                    /** check if it is in push mode
                     *
                     */
                    actAssist.setSmartphone(listSmartphonePair.second);

                    // check if the android app is in push mode; if true push the activity file to the server
                    if (actAssist.getSmartphone().getSynchronized()) {
                        actAssist.setActivities(listSmartphonePair.first);
                        try {
                            data.dumpActAssistToFile(
                                    mainact.getApplicationContext(),
                                    actAssist);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // TODO do sth if this fails

                        }
                        if (!mainact.getSwitchChecked()) {
                            mainact.setReloadSpinnerActivity((ArrayList<String>) actAssist.getActivities());
                        }
                        /** put a Smartphone with file to the
                        * */
                        actAssist.putSmartphoneAPI(activityFile.getActivityMultipart(mainact.getApplicationContext()))
                                .subscribe(new SingleObserver<Smartphone>() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {

                                    }

                                    @Override
                                    public void onSuccess(@NonNull Smartphone smartphone) {
                                        mainact.createToast("successfully uploaded activity file");
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        mainact.createToast("sth. went wrong uploading the activity file");
                                    }
                                });
                    }
                    // the smartphone has been marked dirty; get the activity file from server
                    else{
                        actAssist.setActivities(listSmartphonePair.first);
                        mainact.setReloadSpinnerActivity((ArrayList<String>) actAssist.getActivities());
                        String actFileUrl = actAssist.getSmartphone().getActivityFile();
                        if(actFileUrl != null){
                            /** there is an activity file on the server present to download
                            * */
                            actAssist.downloadActivityFile()
                            .subscribe(new SingleObserver<ResponseBody>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onSuccess(@NonNull ResponseBody responseBody) {
                                    try {
                                        activityFile.replaceActivityFile(mainact.getApplicationContext(), responseBody);
                                    }catch(IOException e){
                                        mainact.createToast("successfully downloaded activity file but couldn't save");
                                    }
                                    actAssist.getSmartphone().setSynchronized(true);
                                    actAssist.putSmartphoneAPI(activityFile.getActivityMultipart(mainact.getApplicationContext()))
                                    .subscribe(new SingleObserver<Smartphone>() {
                                                @Override
                                                public void onSubscribe(@NonNull Disposable d) {

                                                }

                                                @Override
                                                public void onSuccess(@NonNull Smartphone smartphone) {
                                                    mainact.createToast("successfully downloaded activity file");
                                                }

                                                @Override
                                                public void onError(@NonNull Throwable e) {
                                                    mainact.createToast("no connection to server");
                                                    mainact.setServerStatus(SERVER_STATUS_OFFLINE);
                                                }
                                            });
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    mainact.createToast("sth. went wrong downloading activity file");
                                }
                        });
                        }
                        else{
                            /** there no activity file on the server but still have to mark it synchronized
                            * */
                            activityFile.deleteActivityFile(mainact.getApplicationContext());
                            actAssist.getSmartphone().setSynchronized(true);
                            actAssist.putSmartphoneAPI(activityFile.getActivityMultipart(mainact.getApplicationContext()))
                            .subscribe(new SingleObserver<Smartphone>() {
                                        @Override
                                        public void onSubscribe(@NonNull Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(@NonNull Smartphone smartphone) {
                                            mainact.createToast("successfully deleted activity file");
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {
                                            mainact.createToast("no connection to server");
                                            mainact.setServerStatus(SERVER_STATUS_OFFLINE);
                                        }
                                    });
                        }
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    mainact.createToast("couldn't connect to server");
                }
            });
        }
    }
}
