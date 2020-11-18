package com.example.activity_assistant_logger.actassistapi;

import android.util.Pair;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.example.activity_assistant_logger.Controller;
import com.google.android.gms.common.api.Api;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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
    private String url_api;
    private String password;
    private String user_name;
    private transient Retrofit retrofit;
    private Smartphone smartphone;
    private List <Activity> activities;
    private ArrayList <String> activityNames;
    private String experimentRunning;
    //private CompositeDisposable compDisp;

    public ActAssistApi(Controller controller, String url_api, String user_name, String password) {
        this.controller = controller;
        this.user_name = user_name;
        this.password = password;
        this.url_api = url_api;
        this.smartphone = null;

        // intialize http client
        this.retrofit = new Retrofit.Builder()
                .baseUrl(url_api)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        //compDisp = new CompositeDisposable();
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
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_name", user_name);
            jsonObject.put("password", password);
            jsonObject.put("url_api", url_api);
            jsonObject.put("smartphone_json", gson.toJson(smartphone));
            jsonObject.put("activity_list", activityNames);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static ActAssistApi serializeFromJSON(Controller con, JSONObject jsonObject, List<String> activities) throws JSONException {
        ActAssistApi newApi = new ActAssistApi(
                con,
                jsonObject.getString("url_api"),
                jsonObject.getString("user_name"),
                jsonObject.getString("password")
        );
        newApi.setActivities((ArrayList<String>) activities);
        Gson gson = new GsonBuilder().create();
        try {
            newApi.setSmartphone(gson.fromJson(
                    jsonObject.getString("smartphone_json"),
                    Smartphone.class
            ));
        }catch(Exception e){

        }
        return newApi;
    }


// GETTER/SETTER for internal representation
    public Smartphone getSmartphone(){
        return smartphone;
    }

    public List<String> getActivities(){
        return activityNames;
    }

    public void setSmartphone(Smartphone sm){
        this.smartphone = sm;
    }
    public void setActivities(List<Activity> activities){
        this.activityNames = this.extractNames(activities);
    }

    public void setActivities(ArrayList<String> activities){
        this.activityNames = activities;
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
//        compDisp.add(apiService.getSmartphone(this.smartphone_id))
//                .doOnSuccess(sm -> smartphone = sm)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        throwable -> controller.onFailure("GET smartphone failed")
//                ));
    }

    public Single<Pair <List<Activity>, Smartphone >> getSmartphoneAndActivties(){
        ApiService apiService = retrofit.create(ApiService.class);
        Single <List<Activity>> actListSingle = apiService.getActivities();
        Single <Smartphone> smSingle = apiService.getSmartphone(smartphone.getId());
        return actListSingle.zipWith(smSingle, Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Pair <List<Activity>, Smartphone >> getSmartphoneAndActivties(int smartphoneId){
        ApiService apiService = retrofit.create(ApiService.class);
        Single <List<Activity>> actListSingle = apiService.getActivities();
        Single <Smartphone> smSingle = apiService.getSmartphone(smartphoneId);
        return actListSingle.zipWith(smSingle, Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Smartphone createNewSmartphone(String personUrl){
        Smartphone sm = new Smartphone();
        sm.setPerson(personUrl);
        sm.setName("Moto G6");
        sm.setSynchronized(true);
        sm.setLogging(false);
        return sm;
    }

    public Single<Pair <List<Activity>, Smartphone >> createSmartphoneAndGetActivties(Smartphone sm){
        ApiService apiService = retrofit.create(ApiService.class);
        Single <List<Activity>> actListSingle = apiService.getActivities();
        Single <Smartphone> smSingle = apiService.createSmartphone(
                sm.getName(),
                sm.getPerson(),
                sm.getLoggedActivity(),
                sm.getSynchronized());

        return actListSingle.zipWith(smSingle, Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Smartphone> putSmartphoneAPI() {
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // make a request by calling the corresponding method
        return apiService.putSmartphone(this.smartphone.getId(), this.smartphone)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
                //.subscribe(new SingleObserver<Smartphone>() {
              //           @Override
              //           public void onSubscribe(@NonNull Disposable d) {
              //                //compositeDisposable.add(d);
              //           }

              //           @Override
              //           public void onSuccess(@NonNull Smartphone sm) {
              //               controller.onSuccess("successfully put smartphone");
              //           }

              //           @Override
              //           public void onError(@NonNull Throwable e) {
              //               controller.onFailure("PUT smartphone failed");
              //           }
              //       });
    }

    public Single<Smartphone> putSmartphoneAPI(MultipartBody.Part file) {
        //https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
        /** puts a smartphone object
        */
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // create request body instance from file
        // TODO works for putSmartphone API
        //String personUrl = "http://192.168.178.47:8000/api/v1/persons/1/?format=json";
        //String personUrl = "http://localhost:8000/api/v1/persons/1/";
        RequestBody name = RequestBody.create(MultipartBody.FORM, smartphone.getName());
        RequestBody personUrl = RequestBody.create(MultipartBody.FORM, smartphone.getPerson());
        //RequestBody loggedActivity = RequestBody.create(MultipartBody.FORM, smartphone.getLoggedActivity());
       return apiService.putSmartphone(
               smartphone.getId(),
               name,
               personUrl,
               //loggedActivity,
               file)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
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


}
