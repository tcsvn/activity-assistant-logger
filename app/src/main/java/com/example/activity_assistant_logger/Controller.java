package com.example.activity_assistant_logger;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;
import android.widget.Toast;

import androidx.lifecycle.ViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;
import com.example.activity_assistant_logger.actassistapi.Activity;
import com.example.activity_assistant_logger.actassistapi.Person;
import com.example.activity_assistant_logger.actassistapi.Smartphone;
import com.example.activity_assistant_logger.weekview.WeekViewEvent;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import okhttp3.ResponseBody;


public class Controller extends ViewModel {
    final static String SERVER_STATUS_ONLINE = "online";
    final static String SERVER_STATUS_UNCONFIGURED = "unconfigured";
    final static String SERVER_STATUS_OFFLINE = "not reachable";

    final String STATE_INITIAL = "initial";
    final static String DEVICE_STATUS_REGISTERED = "registered";
    final static String DEVICE_STATUS_UNCONFIGURED = "unconfigured";

    private String mDeviceState = STATE_INITIAL;
    private String mServerState = SERVER_STATUS_UNCONFIGURED;

    private boolean mIsLogging = false;
    private boolean mOutOfSync = true;

    // Selected activity represents the HomeFragment selected value whereas
    // currentActivity is only equal to the selectedActivity when an experiment
    // is conducted and the user logs the activity
    private String mSelectedActivity;

    private ActAssistApi actAssist;
    private ConfigHandler config;
    private ActivityFileHandler activityFile;

    private HomeFragment homeFragment;
    private WeekFragment weekFragment;
    private MainActivity mMainActivity;


    public Controller(){
        System.out.println("");
    }
    public Controller(Application app, MainActivity mainActivity){
        this.setup(mainActivity);
    }

    public Controller(MainActivity mainActivity) {
        this.setup(mainActivity);
    }

    private void setup(MainActivity mainActivity){
        mMainActivity = mainActivity;
        this.config = new ConfigHandler();
        this.activityFile = new ActivityFileHandler(mainActivity.getApplicationContext());

        if (config.configExists(mainActivity.getApplicationContext())) {
            loadFromConfig(mainActivity);
        } else {
            resetConfig();
        }
        if (activityFile.activityFileExists(mainActivity.getApplicationContext())) {
            try {
                activityFile.cleanupActivityFile(mainActivity.getApplicationContext(),
                        false, this.actAssist.getActivities());
            } catch (IOException e) {
                mainActivity.createToast("sth. went wrong cleaning up activity file");
            }
        }

        // No OP error handler. If some error occurs in flows that aren't handled, this
        // thing catches it and does nothing (TODO do I want this?)
        RxJavaPlugins.setErrorHandler(e -> {
            //mainact.createToast(e.getMessage());
            System.out.println("asdf");
        });
    }

    public void setHomeFragment(HomeFragment homeFragment) {
        this.homeFragment = homeFragment;
    }

    public void setWeekFragment(WeekFragment weekFragment) {
        this.weekFragment = weekFragment;
    }

    //__Methods__---------------------------------------------------------------------------------------
    private void loadFromConfig(MainActivity mainAct) {
        try {
            this.actAssist = config.loadActAssistFromFile(mainAct.getApplicationContext(), this);
            setDeviceState(DEVICE_STATUS_REGISTERED);
        } catch (JSONException | IOException | ClassNotFoundException e) {
            Toast.makeText(mainAct, "Could not load server config from file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            resetConfig();
        }
    }

    public void restoreActivityState(){
        try{
            Context con = this.mMainActivity.getApplicationContext();
            if(this.activityFile.isLastActivityUnfinished(con)){
                String currentActivity = this.activityFile.getLastActivityName(con);
                homeFragment.setSpinnerActivities((ArrayList<String>) actAssist.getActivities(), currentActivity);
                setSwitchLogging(true);
            }
            else{
                homeFragment.setSpinnerActivities((ArrayList<String>) actAssist.getActivities());
            }
        }catch (IOException e){
            Toast.makeText(this.mMainActivity, "Could not load last activity from file", Toast.LENGTH_SHORT).show();
        };
    }

    public TimeZone getTimeZone(){
        return actAssist.getLocalTimeZone();
    }

    private void resetConfig() {
        this.actAssist = null;
        setServerState(SERVER_STATUS_UNCONFIGURED);
        setDeviceState(DEVICE_STATUS_UNCONFIGURED);
        try {
            setSwitchLogging(false);
            homeFragment.resetSpinnerLists();
        }catch (Exception e){};

    }
    public ArrayList<? extends WeekViewEvent> getActivitiesAsEvents(){
        ArrayList<WeekViewEvent> events = (ArrayList<WeekViewEvent>) this.activityFile.getActivitiesAsEvents(
                getApplicationContext(),
                this.getActivities()
        );
        return events;
    }

    public boolean isActAssistConfigured() {
        return actAssist != null;
    }

     public void createNotification(){
        NotificationHandler.createNotification(mMainActivity, getSelectedActivity());
    }

    public void removeNotification(){
        NotificationHandler.removeNotification(mMainActivity);
    }


    //__GETTER/SETTER__---------------------------------------------------------------------------------

//    public boolean deviceHasActivity() {
//        // TODO
//        return false;
//        //return actAssist.deviceHasActivity();
//    }
//
//    public String getDeviceActivityName() {
//        return "debug";
//        //return actAssist.getDeviceActivityName();
//    }

    public boolean getLogging() {
        try {
            return homeFragment.getSwitchChecked();
        } catch (NullPointerException ignored){
            return mIsLogging;
        }
    }

    public void setDeviceState(String state){
        mDeviceState = state;
        try {
            homeFragment.setDeviceStatus(state);
        } catch (NullPointerException ignored){};
    }

    public String getServerState(){
        return mServerState;
    }

    public void setServerState(String state){
        mServerState = state;
        try {
            homeFragment.setServerStatus(state);
        } catch (NullPointerException ignored){};
    }

    public String getDeviceState(){
        return mDeviceState;
    }

    public void setSwitchLogging(Boolean val){
        homeFragment.setSwitchLogging(val);
        mIsLogging = val;
        if (val){
            createNotification();
        }
        else{
            removeNotification();
        }
    }

    public String getDeviceName() {
        return Build.MODEL.substring(0, 1).toUpperCase() + Build.MODEL.substring(1);
    }

    public List<String> getActivities(){
        return this.actAssist.getActivities();
    }

    public String getSelectedActivity(){
        return homeFragment.getSelectedActivity();
    }



    //___HTTP__Callbacks -------------------------------------------------------------------------------
    public void onFailure(String str) {
        Toast.makeText(mMainActivity, str, Toast.LENGTH_SHORT).show();
        setServerState(SERVER_STATUS_OFFLINE);
    }

    public void onSuccess(String str) {
        Toast.makeText(mMainActivity, str, Toast.LENGTH_SHORT).show();
        setServerState(SERVER_STATUS_ONLINE);
    }

    public void onGetActivitiesSuccess(ArrayList<String> activities) {
        homeFragment.setSpinnerActivities(activities);
        setServerState(SERVER_STATUS_ONLINE);
    }


    //__GUI Callbacks__---------------------------------------------------------------------------------

    public void openedFromNotification(String currentActivity) {
        /** tries to reset the state of the app to before it was minimized
         */
        try {
            assert (currentActivity.equals(this.activityFile.getLastActivityName(getApplicationContext())));
        }catch (IOException e){};
    }

    public void createToast(String s){
        mMainActivity.createToast(s);
    }
    public Context getApplicationContext(){
        return mMainActivity.getApplicationContext();
    }
    public void onDataFromQRCode(JSONObject jsonObject) {
        /** creates an activity assistant api
         * if there is already a smartphone registered on the api
         * copy all data from the smartphone of the api including downloading the activity file
         * if there is no smartphone registered on the api
         * then create an empty smartphone
         * - also get activities
         * create SM and get Activities
         *   -> Succ:
         *      * and Get Person
         *          -> Succ:
         *              * put smartphone
         *                  -> Succ:
         *                  -> Error:
         *              * download activity file
         *                  -> succ:
         *              -> Error:
         *          -> Error:
         *   -> Error:
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
            boolean actFileAlreadyCreated = false;
            try {
                String tmp = jsonObject.getString("activity_file_url");
                actFileAlreadyCreated = true;
            } catch (Exception e) {
                actFileAlreadyCreated = false;
            }

            String personUrl = urlApi + jsonObject.getString(ActAssistApi.URL_PERSON);
            Smartphone sm = actAssist.createNewLocalSmartphone(personUrl);

            // Get timezone from remote
            actAssist.updateLocalTimeZoneFromRemote();

            actAssist.createSmartphoneAndGetActivities(sm)
                    .subscribe(new SingleObserver<Pair<List<Activity>, Smartphone>>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {}

                        @Override
                        public void onSuccess(@NonNull Pair<List<Activity>, Smartphone> listSmartphonePair) {
                            actAssist.updateLocalActivities(listSmartphonePair.first);
                            actAssist.updateLocalSmartphone(listSmartphonePair.second);
                            try {
                                config.dumpActAssistToFile(getApplicationContext(), actAssist);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // TODO do sth if this fails
                            }
                            homeFragment.setSpinnerActivities((ArrayList<String>) actAssist.getActivities());
                            setServerState(SERVER_STATUS_ONLINE);
                            setDeviceState(DEVICE_STATUS_REGISTERED);
                            actAssist.getPerson().subscribe(
                                   person -> {
                                       actAssist.setActivityFileUrl(person.getActivityFile());
                                       actAssist.localSmartphone().setSynchronized(true);
                                       actAssist.putRemoteSmartphone(true);
                                       if (!person.getActivityFile().equals("")){
                                            actAssist.downloadActivityFile(person.getActivityFile(), activityFile);
                                       }
                                   },
                                    throwable -> {
                                       createToast("Could not get Person -> Can not create Smartphone or get Activity File.");
                                    }
                            );
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if (e instanceof ConnectException){
                                setServerState(SERVER_STATUS_OFFLINE);
                                setDeviceState(DEVICE_STATUS_REGISTERED);
                                createToast("can't connect to server. Recheck servers IP settings.");
                            }
                            setServerState(SERVER_STATUS_OFFLINE);
                            setDeviceState(DEVICE_STATUS_UNCONFIGURED);
                            createToast("Can't connect to server.");
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onActivitySelected(String selectedActivity) {
        /** log activity to file
         * */

        // Update controller representation
        mSelectedActivity = selectedActivity;

        MainActivity mainAct = (MainActivity) homeFragment.requireActivity();

        if (mIsLogging) {
            try {
                activityFile.addActivity(
                        mainAct.getApplicationContext(),
                        mSelectedActivity,
                        getTimeZone()
                );
            } catch (Exception e) {
                createToast("sth went wrong writing activity file");
            }
            // update the notification with the current activity
            NotificationHandler.removeNotification(mainAct);
            NotificationHandler.createNotification(mainAct, getSelectedActivity());

            // update external representation
            actAssist.getSmartphoneAndActivties().flatMap(pair -> {
                actAssist.updateLocalSmartphone(pair.second);
                actAssist.localSmartphone().setLogging(true);
                actAssist.localSmartphone().setLoggedActivity(
                        actAssist.getActivityUrl(mSelectedActivity, pair.first));
                return actAssist.putRemoteSmartphone();
            }).subscribe(smartphone -> actAssist.updateLocalSmartphone(smartphone),
                    throwable -> {
                        actAssist.localSmartphone().setLogging(true);
                    }
            );
        }
    }

    public void onScan() {
        /** starts to scanning activity */
        Intent intent = new Intent(mMainActivity, BarcodeCaptureActivity.class);
        mMainActivity.startActivityForResult(intent, 0);
    }

    public void onDecouple() {
        /** first try to delete the device on the server
         * if the server is unreachable wait
         * if the server is reachable post a deleteion'
         *      if the device is already unregistered
         *          proceed with deletion local files
         */

        MainActivity mainAct = (MainActivity) homeFragment.requireActivity();
        actAssist.getRemoteSmartphone().flatMap(smartphone -> actAssist.deleteRemoteSmartphone(smartphone))
                .subscribe(responseBody -> {
                            setServerState(SERVER_STATUS_UNCONFIGURED);
                            setDeviceState(DEVICE_STATUS_UNCONFIGURED);
                            setSwitchLogging(false);
                            homeFragment.resetSpinnerLists();
                            actAssist = null;

                            // wipe data
                            config.deleteConfigFile(mainAct.getApplicationContext());
                            activityFile.deleteActivityFile(mainAct.getApplicationContext());
                            createToast("deleted everything");
                        },
                        throwable -> {
                            // TODO this is a dirty hack to respond to the
                            // inadequate 204 No Content response from server after deleting
                            // the object which results in an error but should have been a success
                            if (throwable instanceof java.util.NoSuchElementException) {
                                setServerState(SERVER_STATUS_UNCONFIGURED);
                                setDeviceState(DEVICE_STATUS_UNCONFIGURED);
                                setSwitchLogging(false);

                                homeFragment.resetSpinnerLists();
                                actAssist = null;

                                // wipe data
                                config.deleteConfigFile(mainAct.getApplicationContext());
                                activityFile.deleteActivityFile(mainAct.getApplicationContext());
                                createToast("deleted everything");
                            } else {
                                mainAct.createToast("couldn't delete smartphone on api");
                            }
                        }
                );
    }

    public void onBtnSynchronize() {
        /** synchronize cached data files
         * first gets a smartphone and checks if it is in push mode or not
         * if it is in push mode pushes smartphone and activity file to server
         * if it is not synchronized delete local activity file and
         * request new activities and pull activity file from server
         * */

        if (this.mDeviceState.equals(DEVICE_STATUS_REGISTERED)) {
            actAssist.getSmartphoneAndActivties().flatMap(pair -> {
                // update activities and smartphone with relevant information from the server
                Smartphone sm = pair.second;
                List<Activity> acts = pair.first;
                actAssist.updateLocalActivities(acts);
                if (mIsLogging) {
                    homeFragment.setSpinnerActivities(
                            (ArrayList<String>) actAssist.getActivities(),
                            this.getSelectedActivity()
                            );
                }
                else{
                    homeFragment.setSpinnerActivities((ArrayList<String>) actAssist.getActivities());
                }
                actAssist.updateLocalSmartphone(sm);
                return actAssist.getPerson();

            }).flatMap(person -> {
                // collected all data so it is proper to save it
                actAssist.setActivityFileUrl(person.getActivityFile());
                try {
                    config.dumpActAssistToFile(
                            mMainActivity.getApplicationContext(),
                            actAssist);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (actAssist.localSmartphone().getSynchronized()) {
                    // Smartphone is in sync -> upload activity file
                    return actAssist.uploadActivityFile(person,
                            activityFile.getActivityMultipart(
                                    mMainActivity.getApplicationContext(),
                                    actAssist.getActivities())
                    );
                } else if (actAssist.getActivityFileUrl() == null) {
                    // Smartphone is out of sync and the server has no activity file
                    // -> delete local activity file
                    actAssist.updateLocalTimeZoneFromRemote();
                    activityFile.deleteActivityFile(mMainActivity.getApplicationContext());
                    return actAssist.putRemoteSmartphone();
                } else {
                    // Smartphone is out of sync and the server has an activity file
                    // -> download activity file
                    actAssist.updateLocalTimeZoneFromRemote();
                    return actAssist.downloadActivityFile(person.getActivityFile());
                }
            }).flatMap(object -> {
                if (object instanceof ResponseBody) {
                    try {
                        activityFile.replaceActivityFile(
                                mMainActivity.getApplicationContext(), ((ResponseBody) object));
                    } catch (IOException e) {
                        createToast("successfully downloaded activity file but could not save");
                    }
                } else if (object instanceof Person) {
                    // case if in previous step the file was uploaded
                    actAssist.setActivityFileUrl(((Person) object).getActivityFile());
                }
                actAssist.localSmartphone().setSynchronized(true);
                // everything worked out
                return actAssist.putRemoteSmartphone();
            }).subscribe(sm -> {
                        createToast("successfully synchronized");
                        setServerState(SERVER_STATUS_ONLINE);
                    }, throwable -> {
                        createToast("couldn't synchronize");
                        if (throwable instanceof IOException) {
                            setServerState(SERVER_STATUS_OFFLINE);
                        }
                    }
            );//.dispose();
        } else {
            if (getLogging()) {
                createToast("sync is not allowed while logging");
            }
        }
    }

     public void onSwitchToggled(boolean turnedOn) {
        /** log activity to file if an experiment is conducted
         *
         */

        if (!getDeviceState().equals(DEVICE_STATUS_REGISTERED)){
            setSwitchLogging(false);
            createToast("First connect to Activity Assistant API in order to start logging");
            return;
        }

        if (turnedOn) {
            NotificationHandler.createNotification(mMainActivity, getSelectedActivity());
            try {
                activityFile.createNewActivity(getApplicationContext(),  mSelectedActivity, getTimeZone());
            } catch (Exception e) {
                createToast("sth went wrong writing activity file");
            }

            mIsLogging = true;
            // update external representation of smartphone
            actAssist.getSmartphoneAndActivties().flatMap(pair -> {
                actAssist.updateLocalSmartphone(pair.second);
                actAssist.localSmartphone().setLogging(true);
                actAssist.localSmartphone().setLoggedActivity(
                        actAssist.getActivityUrl(mSelectedActivity, pair.first));
                return actAssist.putRemoteSmartphone();
            }).subscribe(
                    smartphone -> {
                        actAssist.updateLocalSmartphone(smartphone);
                        setServerState(SERVER_STATUS_ONLINE);
                    },
                    throwable -> {
                        if (throwable instanceof ConnectException) {
                            setServerState(SERVER_STATUS_OFFLINE);
                        }
                        actAssist.localSmartphone().setLogging(true);
                    }
            );
        } else {
            NotificationHandler.removeNotification(mMainActivity);
            try {
                activityFile.finishLastActivity(
                        mMainActivity.getApplicationContext(),
                        getTimeZone()
                        );
            } catch (Exception e) {
                createToast("sth went wrong writing activity file");
            }

            mIsLogging = false;
            // update external representation of smartphone
            actAssist.getRemoteSmartphone().flatMap(smartphone -> {
                actAssist.updateLocalSmartphone(smartphone);
                actAssist.localSmartphone().setLogging(false);
                actAssist.localSmartphone().setLoggedActivity(null);
                return actAssist.putRemoteSmartphone();
            }).subscribe(
                    smartphone -> {
                        actAssist.updateLocalSmartphone(smartphone);
                        setServerState(SERVER_STATUS_ONLINE);
                    },
                    throwable -> {
                        actAssist.localSmartphone().setLogging(false);
                        actAssist.localSmartphone().setLoggedActivity(null);
                        if (throwable instanceof ConnectException) {
                            setServerState(SERVER_STATUS_OFFLINE);
                        }
                    });
        }
    }



}
