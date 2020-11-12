package com.example.activity_assistant_logger.actassistapi;

import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.example.activity_assistant_logger.Controller;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
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
                .build();
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())

        // DEBUG START
        this.experimentRunning = EXP_RUNNING;
        // DEBUG END
    }
    public void initRetrofit(){
         this.retrofit = new Retrofit.Builder()
                .baseUrl(url_api)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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
        Call<Smartphone> call = apiService.getSmartphone(this.smartphone_id);
        call.enqueue(new Callback<Smartphone>() {
            @Override
            public void onResponse(Call<Smartphone> call, Response<Smartphone> response) {
                smartphone = response.body();
                controller.onSuccess("GET smartphone success");
            }

            @Override
            public void onFailure(Call<Smartphone> call, Throwable throwable) {
                controller.onFailure("GET smartphone failed");
            }
        });

    }

    public void putSmartphoneAPI() {
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // make a request by calling the corresponding method
        Call<Smartphone> call = apiService.putSmartphone(this.smartphone_id, this.smartphone);
        call.enqueue(new Callback<Smartphone>() {
            @Override
            public void onResponse(Call<Smartphone> call, Response<Smartphone> response) {
                controller.onSuccess("successfully put smartphone");
            }

            @Override
            public void onFailure(Call<Smartphone> call, Throwable throwable) {
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
        Call<Smartphone> call = apiService.putSmartphone(this.smartphone_id, this.smartphone);
        call.enqueue(new Callback<Smartphone>() {
            @Override
            public void onResponse(Call<Smartphone> call, Response<Smartphone> response) {
                controller.onSuccess("successfully put smartphone");
            }

            @Override
            public void onFailure(Call<Smartphone> call, Throwable throwable) {
                controller.onFailure("PUT smartphone failed");
            }
        });

    }

    public void getActivitiesAPI() {
        // create an instance of the ApiService
        ApiService apiService = retrofit.create(ApiService.class);

        // make a request by calling the corresponding method
        Call<List<Activity>> call = apiService.getActivities();
        call.enqueue(new Callback<List<Activity>>() {
            @Override
            public void onResponse(Call<List<Activity>> call, Response<List<Activity>> response) {
                activities = response.body();
                activityNames = extractNames(activities);
                controller.onGetActivitiesSuccess(activityNames);
            }

            @Override
            public void onFailure(Call<List<Activity>> call, Throwable throwable) {
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

