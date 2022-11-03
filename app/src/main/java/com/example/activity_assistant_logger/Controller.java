package com.example.activity_assistant_logger;

import android.app.Application;
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

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;
import com.example.activity_assistant_logger.actassistapi.Activity;
import com.example.activity_assistant_logger.actassistapi.Person;
import com.example.activity_assistant_logger.actassistapi.Smartphone;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import okhttp3.ResponseBody;


public class Controller extends ViewModel {
    final static String SERVER_STATUS_ONLINE = "online";
    final static String SERVER_STATUS_UNCONFIGURED = "unconfigured";
    final static String SERVER_STATUS_OFFLINE = "not reachable";
    final static String DEVICE_STATUS_REGISTERED = "registered";
    final static String DEVICE_STATUS_UNCONFIGURED = "unconfigured";
    final String STATE_INITIAL = "initial";
    private boolean isLogging = false;

    private String deviceState = STATE_INITIAL;

    // Selected activity represents the HomeFragment selected value whereas
    // currentActivity is only equal to the selectedActivity when an experiment
    // is conducted and the user logs the activity
    private String mCurrentActivity;
    private String mSelectedActivity;
    private boolean outOfSync = true;
    private ActAssistApi actAssist;
    private ConfigHandler data;
    private ActivityFileHandler activityFile;

    private HomeFragment homeFragment;
    private WeekFragment weekFragment;


    public Controller(){
        System.out.println("");
    }
    public Controller(Application app, MainActivity mainActivity){
        this.setup(mainActivity);

    }

    public Controller(MainActivity mainact) {
        this.setup(mainact);
    }

    private void setup(MainActivity mainact){
        this.data = new ConfigHandler();
        this.activityFile = new ActivityFileHandler(mainact.getApplicationContext());

        if (data.configExists(mainact.getApplicationContext())) {
            loadFromConfig(mainact);
        } else {
            resetConfig();
        }
        //if (mainact.isStartedFromNotification(intent)) {
        if (false){
            //openedFromNotification(intent.getStringExtra("currentActivity"));
        } else {
            if (activityFile.activityFileExists(mainact.getApplicationContext())) {
                // if there is a line from logging before. remove that line
                try {
                    activityFile.cleanupActivityFile(mainact.getApplicationContext());
                } catch (IOException e) {
                    mainact.createToast("sth. went wrong cleaning up activity file");
                }
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
    public void setLogging(boolean state){
       isLogging = state;
    }

    //__Methods__---------------------------------------------------------------------------------------
    private void loadFromConfig(MainActivity mainAct) {
        try {
            this.actAssist = data.loadActAssistFromFile(mainAct.getApplicationContext(), this);
            deviceState = DEVICE_STATUS_REGISTERED;
        } catch (JSONException e) {
            Toast.makeText(mainAct, "couldn't load server config to file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            resetConfig();
        } catch (IOException | ClassNotFoundException e) {
            Toast.makeText(mainAct, "couldn't load server config to file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            resetConfig();
        }
    }

    private void resetConfig() {
        this.actAssist = null;
        // TODO update view somehow
        // mainact.setServerStatus(SERVER_STATUS_UNCONFIGURED);
        // mainact.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
        this.deviceState = DEVICE_STATUS_UNCONFIGURED;
        // mainact.setSwitchLogging(false);
        // mainact.resetSpinnerLists();
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

    public String getDeviceName() {
        return Build.MODEL.substring(0, 1).toUpperCase() + Build.MODEL.substring(1);
    }
    public List<String> getActivities(){
        return this.actAssist.getActivities();
    }
    public boolean getLogging() {
        return isLogging;
    }

    public String getLoggedActivity() {
        return mCurrentActivity;
    }

    //___HTTP__Callbacks -------------------------------------------------------------------------------
    public void onFailure(String str) {
        Toast.makeText(homeFragment.requireActivity(), str, Toast.LENGTH_SHORT).show();
        homeFragment.setServerStatus(SERVER_STATUS_OFFLINE);
    }

    public void onSuccess(String str) {
        Toast.makeText(homeFragment.requireActivity(), str, Toast.LENGTH_SHORT).show();
        homeFragment.setServerStatus(SERVER_STATUS_ONLINE);
    }

    public void onGetActivitiesSuccess(ArrayList<String> activities) {
        homeFragment.setReloadSpinnerActivity(activities);
        homeFragment.setServerStatus(SERVER_STATUS_ONLINE);
    }

    //__GUI Callbacks__---------------------------------------------------------------------------------

    //public void openedFromNotification(String currentActivity) {
    //    /** tries to reset the state of the app to before it was minimized
    //     *
    //     */
    //    mainact.setSwitchLogging(true);
    //    this.currentActivity = currentActivity;
    //    mainact.setSpinnerActivity((ArrayList<String>) actAssist.getActivities(), currentActivity);
    //}

    public void createToast(String s){
        ((MainActivity) homeFragment.requireActivity()).createToast(s);
    }


    public void receivedDataFromQRCode(JSONObject jsonObject) {
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
            boolean actFileAlreadyCreated = false;
            try {
                String tmp = jsonObject.getString("activity_file_url");
                actFileAlreadyCreated = true;
            } catch (Exception e) {
                actFileAlreadyCreated = false;
            }
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
                                        homeFragment.requireActivity().getApplicationContext(),
                                        actAssist);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // TODO do sth if this fails

                            }
                            homeFragment.setReloadSpinnerActivity((ArrayList<String>) actAssist.getActivities());
                            homeFragment.setServerStatus(SERVER_STATUS_ONLINE);
                            deviceState = DEVICE_STATUS_REGISTERED;
                            homeFragment.setDeviceStatus(DEVICE_STATUS_REGISTERED);
                            actAssist.getPerson().subscribe(new SingleObserver<Person>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onSuccess(@NonNull Person person) {
                                    actAssist.setActivityFileUrl(person.getActivityFile());
                                    actAssist.getSmartphone().setSynchronized(true);
                                    actAssist.putSmartphoneAPI().subscribe(new SingleObserver<Smartphone>() {
                                        @Override
                                        public void onSubscribe(@NonNull Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(@NonNull Smartphone smartphone) {
                                            actAssist.setSmartphone(smartphone);
                                        }

                                        @Override
                                        public void onError(@NonNull Throwable e) {

                                        }
                                    });
                                    try {
                                        // check if there is an activity file on server
                                        person.getActivityFile().equals("");
                                        // than download it
                                        actAssist.downloadActivityFile(person.getActivityFile()).subscribe(new SingleObserver<ResponseBody>() {
                                            @Override
                                            public void onSubscribe(@NonNull Disposable d) {

                                            }

                                            @Override
                                            public void onSuccess(@NonNull ResponseBody responseBody) {
                                                try {
                                                    activityFile.replaceActivityFile(homeFragment.requireActivity().getApplicationContext(), responseBody);
                                                } catch (IOException e) {
                                                    // TODO very ugly
                                                    createToast("successfully downloaded activity file but couldn't save");
                                                }
                                            }

                                            @Override
                                            public void onError(@NonNull Throwable e) {
                                                System.out.println("asdf");
                                            }
                                        });
                                    } catch (NullPointerException e) {
                                    }
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    System.out.println("asdf");
                                }
                            });

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if (e instanceof ConnectException){
                                homeFragment.setServerStatus(SERVER_STATUS_OFFLINE);
                                deviceState = DEVICE_STATUS_UNCONFIGURED;
                                homeFragment.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
                                createToast("can't connect to server. Recheck servers IP settings.");
                            }
                            homeFragment.setServerStatus(SERVER_STATUS_OFFLINE);
                            deviceState = DEVICE_STATUS_UNCONFIGURED;
                            homeFragment.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
                            createToast("Can't connect to server.");
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onActivitySelected(String selectedActivity) {
        /** log activity to file if an experiment is conducted
         * */
        // Update controller representation
        mSelectedActivity = selectedActivity;

        MainActivity mainAct = (MainActivity) homeFragment.requireActivity();

        if (isLogging && actAssist.isExperimentConducted()) {
            try {
                activityFile.addActivity(
                        mainAct.getApplicationContext(),
                        mCurrentActivity,
                        mSelectedActivity
                );
                mCurrentActivity = mSelectedActivity;
            } catch (Exception e) {
                createToast("sth went wrong writing activity file");
            }
            // update the notification with the current activity
            // mainAct.removeNotification();
            // mainAct.createNotification();

            // update external representation
            actAssist.getSmartphoneAndActivties().flatMap(pair -> {
                actAssist.setSmartphone(pair.second);
                actAssist.getSmartphone().setLogging(true);
                actAssist.getSmartphone().setLoggedActivity(
                        actAssist.getActivityUrl(mCurrentActivity, pair.first));
                return actAssist.putSmartphoneAPI();
            }).subscribe(smartphone -> actAssist.setSmartphone(smartphone),
                    throwable -> {
                        actAssist.getSmartphone().setLogging(true);
                    }
            );
        }
    }

    public void onBtnScanQRCode() {
        /** starts to scanning activity */
        MainActivity mainAct = (MainActivity) homeFragment.requireActivity();
        Intent intent = new Intent(mainAct, BarcodeCaptureActivity.class);
        mainAct.startActivityForResult(intent, 0);
    }

    // TODO mark for deleteion since below is now a fragment
    //public void onBtnShowActFileView() {
    //    Intent intent = new Intent(mainact, DisplayFileActivity.class);
    //    mainact.startActivityForResult(intent, 0);
    //}

    public void onBtnDecouple() {
        /** first try to delete the device on the server
         * if the server is unreachable wait
         * if the server is reachable post a deleteion'
         *      if the device is already unregistered
         *          proceed with deletion local files
         */

        MainActivity mainAct = (MainActivity) homeFragment.requireActivity();
        actAssist.getSmartphoneAPI().flatMap(smartphone -> actAssist.deleteSmartphoneAPI(smartphone))
                .subscribe(responseBody -> {
                            deviceState = DEVICE_STATUS_UNCONFIGURED;
                            homeFragment.setServerStatus(SERVER_STATUS_UNCONFIGURED);
                            homeFragment.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
                            homeFragment.setSwitchLogging(false);
                            homeFragment.resetSpinnerLists();
                            actAssist = null;

                            // wipe data
                            data.deleteConfigFile(mainAct.getApplicationContext());
                            activityFile.deleteActivityFile(mainAct.getApplicationContext());
                            createToast("deleted everything");
                        },
                        throwable -> {
                            // TODO this is a dirty hack to respond to the
                            // inadequate 204 No Content response from server after deleting
                            // the object which results in an error but should have been a success
                            if (throwable instanceof java.util.NoSuchElementException) {
                                deviceState = DEVICE_STATUS_UNCONFIGURED;
                                homeFragment.setServerStatus(SERVER_STATUS_UNCONFIGURED);
                                homeFragment.setDeviceStatus(DEVICE_STATUS_UNCONFIGURED);
                                homeFragment.setSwitchLogging(false);
                                homeFragment.resetSpinnerLists();
                                actAssist = null;

                                // wipe data
                                data.deleteConfigFile(mainAct.getApplicationContext());
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
        if (this.deviceState.equals(DEVICE_STATUS_REGISTERED) && !this.isLogging) {
            actAssist.getSmartphoneAndActivties().flatMap(pair -> {
                // update activities and smartphone with relevant information from the server
                Smartphone sm = pair.second;
                List<Activity> acts = pair.first;
                actAssist.setSmartphone(sm);
                actAssist.setActivities(acts);
                homeFragment.setReloadSpinnerActivity((ArrayList<String>) actAssist.getActivities());
                return actAssist.getPerson();

            }).flatMap(person -> {
                // collected all data so it is proper to save it
                actAssist.setActivityFileUrl(person.getActivityFile());
                try {
                    data.dumpActAssistToFile(
                            homeFragment.requireActivity().getApplicationContext(),
                            actAssist);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (actAssist.getSmartphone().getSynchronized()) {
                    // if smartphone is in sync
                    //      upload the activity file
                    return actAssist.uploadActivityFile(person,
                            activityFile.getActivityMultipart(homeFragment.requireActivity().getApplicationContext()));
                } else if (actAssist.getActivityFileUrl() == null) {
                    // if smartphone is out of sync and the server has no activity file
                    //      delete local activity file
                    activityFile.deleteActivityFile(homeFragment.requireActivity().getApplicationContext());
                    return actAssist.putSmartphoneAPI();
                } else {
                    // if smartphone is out of sync and the server has a activitiy file
                    //      download activity file
                    return actAssist.downloadActivityFile(person.getActivityFile());
                }
            }).flatMap(object -> {
                if (object instanceof ResponseBody) {
                    try {
                        activityFile.replaceActivityFile(
                                homeFragment.requireActivity().getApplicationContext(), ((ResponseBody) object));
                    } catch (IOException e) {
                        createToast("successfully downloaded activity file but could not save");
                    }
                } else if (object instanceof Person) {
                    // case if in previous step the file was uploaded
                    actAssist.setActivityFileUrl(((Person) object).getActivityFile());
                }
                actAssist.getSmartphone().setSynchronized(true);
                // everything worked out
                return actAssist.putSmartphoneAPI();
            }).subscribe(sm -> {
                        createToast("successfully synchronized");
                        homeFragment.setServerStatus(SERVER_STATUS_ONLINE);
                    }, throwable -> {
                        createToast("couldn't synchronize");
                        if (throwable instanceof IOException) {
                            homeFragment.setServerStatus(SERVER_STATUS_OFFLINE);
                        }
                    }
            );//.dispose();
        } else {
            if (getLogging()) {
                createToast("sync is not allowed while logging");
            }
        }
    }
     public void switchLoggingToggled(boolean turnedOn) {
        /** log activity to file if an experiment is conducted
         *
         */

        if (!deviceState.equals(DEVICE_STATUS_REGISTERED)){
             homeFragment.setSwitchLogging(false);
            createToast("Connect to Activity Assistant API in order to start logging");
            return;
        }

        if (turnedOn) {
            // TODO
            //mainact.createNotification();
            if (actAssist.isExperimentConducted()) {
                try {
                    activityFile.createActivity(
                            homeFragment.requireActivity().getApplicationContext(),
                            mSelectedActivity
                    );
                    mCurrentActivity = mSelectedActivity;
                } catch (Exception e) {
                    createToast("sth went wrong writing activity file");
                }

                // update external representation of smartphone
                actAssist.getSmartphoneAndActivties().flatMap(pair -> {
                    actAssist.setSmartphone(pair.second);
                    actAssist.getSmartphone().setLogging(true);
                    actAssist.getSmartphone().setLoggedActivity(
                            actAssist.getActivityUrl(mCurrentActivity, pair.first));
                    return actAssist.putSmartphoneAPI();
                }).subscribe(
                        smartphone -> {
                            actAssist.setSmartphone(smartphone);
                            homeFragment.setServerStatus(SERVER_STATUS_ONLINE);
                        },
                        throwable -> {
                            if (throwable instanceof ConnectException) {
                                homeFragment.setServerStatus(SERVER_STATUS_OFFLINE);
                            }
                            actAssist.getSmartphone().setLogging(true);
                        }
                );
            }
        } else {
            // TODO notification
            //mainact.removeNotification();
            if (actAssist.isExperimentConducted()) {
                try {
                    activityFile.finishActivity(
                            homeFragment.requireActivity().getApplicationContext(),
                            mSelectedActivity);
                    mCurrentActivity = null;
                } catch (Exception e) {
                    createToast("sth went wrong writing activity file");
                }

                // update external representation of smartphone
                actAssist.getSmartphoneAPI().flatMap(smartphone -> {
                    actAssist.setSmartphone(smartphone);
                    actAssist.getSmartphone().setLogging(false);
                    actAssist.getSmartphone().setLoggedActivity("");
                    return actAssist.putSmartphoneAPI();
                }).subscribe(
                        smartphone -> {
                            actAssist.setSmartphone(smartphone);
                            homeFragment.setServerStatus(SERVER_STATUS_ONLINE);
                        },
                        throwable -> {
                            actAssist.getSmartphone().setLogging(false);
                            actAssist.getSmartphone().setLoggedActivity("");
                            if (throwable instanceof ConnectException) {
                                homeFragment.setServerStatus(SERVER_STATUS_OFFLINE);
                            }
                        });
            }
        }
    }



}
