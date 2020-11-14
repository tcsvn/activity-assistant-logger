package com.example.activity_assistant_logger.actassistapi;

import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.example.activity_assistant_logger.Controller;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ActAssistApi implements Serializable {
    /* thse are the keynames that are transfered via the qrcode
     * */
    public final static String URL_API = "url_api";
    public final static String SMARTPHONE_ID = "smartphone_id";
    public final static String URL_PERSON = "url_person";
    public final static String USERNAME = "username";
    public final static String PASSWORD = "password";
    public final static String EXP_RUNNING = "experiment_running";
    public final static String EXP_PAUSED = "experiment_paused";
    public final static String EXP_NOT_RUNNING = "experiment_not_running";

    private transient Controller controller;
    private int smartphone_id;
    private String url_api;
    private String password;
    private String user_name;
    private transient Retrofit retrofit;
    private Smartphone smartphone;
    private List <Activity> activities;
    private ArrayList <String> activityNames;
    private String experimentRunning;

    public ActAssistApi(Controller controller, String url_api, int smartpone_id,
                        String url_person, String user_name, String password) {
        this.controller = controller;
        this.user_name = user_name;
        this.password = password;
        this.url_api = url_api;
        this.smartphone = null;
        this.smartphone_id = smartpone_id;

        // intialize http client
        this.retrofit = new Retrofit.Builder()
                .baseUrl(url_api)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        // DEBUG START
        this.experimentRunning = EXP_RUNNING;
        // DEBUG END
    }
    public void initRetrofit(){
         this.retrofit = new Retrofit.Builder()
                .baseUrl(url_api)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }
    public void setController(Controller con){
        this.controller = con;
    }

///__JSON__------------------------------------------------------------------------------------------
    public JSONObject serializeToJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_name", user_name);
            jsonObject.put("password", password);
            jsonObject.put("url_api", url_api);
            jsonObject.put("smartphone_id", smartphone_id);
            jsonObject.put("activity_list", activityNames);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static ActAssistApi serializeFromJSON(Controller con, JSONObject jsonObject) throws JSONException {
        ActAssistApi newApi = new ActAssistApi(
                con,
                jsonObject.getString("url_api"),
                jsonObject.getInt("smartphone_id"),
                jsonObject.getString("url_person"),
                jsonObject.getString("user_name"),
                jsonObject.getString("password")
        );
        return newApi;
    }


// GETTER/SETTER for internal representation
    public Smartphone getSmartphone(){
        return smartphone;
    }

    public List<String> getActivities(){
        return activityNames;
    }

    public boolean isExperimentConducted(){
        return this.experimentRunning.equals(EXP_RUNNING)
                || this.experimentRunning.equals((EXP_PAUSED));
    }

    public boolean isExperimentRunning(){
        return this.experimentRunning.equals(EXP_RUNNING);
    }
    public void setExperiment(String experimentState){
        if (experimentState.equals(EXP_PAUSED)
                || experimentState.equals(EXP_NOT_RUNNING)
                || experimentState.equals(EXP_RUNNING)){
            this.experimentRunning = experimentState;
        }
    }
    public void setSmartphoneLogging(Boolean val){
        smartphone.setLogging(val);
    }

// API request
    public void getSmartphoneAPI() {
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // make a request by calling the corresponding method
         apiService.getSmartphone(this.smartphone_id)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SingleObserver<Smartphone>() {
                   @Override
                   public void onSubscribe(@NonNull Disposable d) {
                        //compositeDisposable.add(d);
                   }

                   @Override
                   public void onSuccess(@NonNull Smartphone sm) {
                        smartphone = sm;
                        controller.onSuccess("GET smartphone success");
                   }

                   @Override
                   public void onError(@NonNull Throwable e) {
                       controller.onFailure("GET smartphone failed");
                   }
               });
    }

    public void putSmartphoneAPI() {
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // make a request by calling the corresponding method
        apiService.putSmartphone(this.smartphone_id, this.smartphone)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Smartphone>() {
                           @Override
                           public void onSubscribe(@NonNull Disposable d) {
                                //compositeDisposable.add(d);
                           }

                           @Override
                           public void onSuccess(@NonNull Smartphone sm) {
                               controller.onSuccess("successfully put smartphone");
                           }

                           @Override
                           public void onError(@NonNull Throwable e) {
                               controller.onFailure("PUT smartphone failed");
                           }
                       });
    }

    public void putSmartphoneAPI(File file) {
        //https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
        /** puts a smartphone object
        */
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // create request body instance from file

        // make a request by calling the corresponding method
        apiService.putSmartphone(this.smartphone_id, this.smartphone);
        // TODO implement method
    }

    public void getActivitiesAPI() {
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // make a request by calling the corresponding method
        apiService.getActivities()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SingleObserver<List<Activity>>() {
                   @Override
                   public void onSubscribe(@NonNull Disposable d) {
                        //compositeDisposable.add(d);
                   }

            @Override
            public void onSuccess(@NonNull List<Activity> acts) {
                       activities = acts;
                       activityNames = extractNames(activities);
                       controller.onGetActivitiesSuccess(activityNames);
                   }

                   @Override
                   public void onError(@NonNull Throwable e) {
                       controller.onFailure("GET activities failed");
                   }
               });
    }

// HELPER methods ----------------------------------------------------------------------------------
    private ArrayList<String> extractNames(List<Activity> activities){
        ArrayList<String> result = new ArrayList<String>();
        for (Activity act : activities){
           result.add(act.getName());
        }
        return result;
    }

    //Single<Smartphone> sm = apiService.getSmartphone(smartphoneId)
        //        .subscribe(new SingleObserver<Smartphone>() {
        //            @Override
        //            public void onSubscribe(@NonNull Disposable d) {
        //                compositeDisposable.add(d);
        //            }

        //            @Override
        //            public void onSuccess(@NonNull Smartphone smartphone) {

        //            }

        //            @Override
        //            public void onError(@NonNull Throwable e) {

        //            }
        //        });
}


////__REAL_METHODS_I_NEED__------------------


//// -------------------------------------------------------------------------------------------------
//    public void pingServer(){
//       pingServer("", new String[]{});
//    }
//

