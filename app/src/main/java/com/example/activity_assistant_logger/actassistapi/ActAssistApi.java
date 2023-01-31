
package com.example.activity_assistant_logger.actassistapi;

import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.example.activity_assistant_logger.ActivityFileHandler;
import com.example.activity_assistant_logger.Controller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
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

    private transient Controller mController;
    final private String mAPIUrl;
    final private String mPass;
    final private String mUserName;
    private transient Retrofit retrofit;
    private Smartphone mSmartphone;
    private List <Activity> activities;
    private ArrayList <String> activityNames;
    private String activityFileUrl;
    private String mTimeZone;

    // TODO critical, get server id over QR-Code
    final private int mServerId = 1;

    public ActAssistApi(Controller controller, String APIUrl, String userName, String password) {
        mController = controller;
        mUserName = userName;
        mPass = password;
        mAPIUrl = APIUrl;
        mSmartphone = null;

        // Initialize http client
        initRetrofit();

    }
    public void initRetrofit(){
         retrofit = new Retrofit.Builder()
                .baseUrl(mAPIUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }


///__JSON__------------------------------------------------------------------------------------------
    public JSONObject serializeToJSON(){
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_name", mUserName);
            jsonObject.put("password", mPass);
            jsonObject.put("url_api", mAPIUrl);
            jsonObject.put("time_zone", mTimeZone);
            jsonObject.put("smartphone_json", gson.toJson(mSmartphone));
            jsonObject.put("activity_list", activityNames);
            jsonObject.put("activity_file_url", activityFileUrl);
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
        try {
            newApi.setActivityFileUrl(jsonObject.getString("activity_file_url"));
        }catch (JSONException e){
            newApi.setActivityFileUrl("");
        }
        newApi.updateLocalActivities((ArrayList<String>) activities);
        Gson gson = new GsonBuilder().create();
        try {
            newApi.updateLocalSmartphone(gson.fromJson(
                    jsonObject.getString("smartphone_json"),
                    Smartphone.class
            ));
        }catch(Exception e){

        }
        try {
            newApi.setLocalTimeZone(jsonObject.getString("time_zone"));
        }catch (JSONException e){};

        return newApi;
    }


// GETTER/SETTER for internal representation
    public TimeZone getLocalTimeZone(){
        if (mTimeZone != null){
            return TimeZone.getTimeZone(mTimeZone);
        }
        else{
            return TimeZone.getDefault();
        }
    }
    public void setLocalTimeZone(String timeZone){
        boolean isTimeZoneValid = false;
        for (String str : TimeZone.getAvailableIDs()) {
            if (str.equals(timeZone)) {
                isTimeZoneValid = true;
                break;
            }
        }
        if (!isTimeZoneValid){
            mController.createToast("No valid time zone was given. ERROR THIS IS VERY BAD!");
            return;
        }
        mTimeZone = timeZone;
    }

    public Smartphone localSmartphone(){
        return mSmartphone;
    }

    public List<String> getActivities(){
        return activityNames;
    }

    public String getActivityFileUrl(){
        return this.activityFileUrl;
    }

    public void setActivityFileUrl(String url){
        this.activityFileUrl = url;
    }

    public void updateLocalSmartphone(Smartphone sm){
        mSmartphone = sm;

        // Overwrite remote values with current values
        mSmartphone.setName(mController.getDeviceName());
        boolean isLogging = mController.getLogging();
        mSmartphone.setLogging(isLogging);
        if (isLogging) {
            mSmartphone.setLoggedActivity(
                    this.getActivityUrl(
                           mController.getSelectedActivity(),
                           this.activities
                    ));
        }
        else{
            mSmartphone.setLoggedActivity(null);
        }

    }

    public String getActivityUrl(String currentActivity, List <Activity> acts){
        String actUrl = null;
        for (int i = 0; i < acts.size(); i++){
            if(acts.get(i).getName().equals(currentActivity)){
                actUrl = createActivityUrl(acts.get(i));
                break;
            }
        }
        return actUrl;
    }

    public String createActivityUrl(Activity act){
        // TODO refactor, do not hardcode these urls
        return mAPIUrl + "activities/" + act.getId() + "/";
    }

    public void updateLocalActivities(List<Activity> activities){
        this.activities = activities;
        this.activityNames = this.extractNames(activities);
    }

    public void updateLocalActivities(ArrayList<String> activities){
        this.activityNames = activities;
    }

// API request
//    public void getSmartphoneAPI() {
//        // create an instance of the ApiService
//        ApiService apiService = retrofit.create(ApiService.class);
//
//        // make a request by calling the corresponding method
//        compDisp.add(apiService.getSmartphone(this.smartphone_id))
//                .doOnSuccess(sm -> smartphone = sm)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        throwable -> controller.onFailure("GET smartphone failed")
//                ));
//    }
    public Single<Pair <List<Activity>, Smartphone >> getSmartphonePersonAndActivties(){
        ApiService apiService = retrofit.create(ApiService.class);
        Single <List<Activity>> actListSingle = apiService.getActivities();
        Single <Smartphone> smSingle = apiService.getSmartphone(mSmartphone.getId());
        return actListSingle.zipWith(smSingle, Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public Single<Pair <List<Activity>, Smartphone >> getSmartphoneAndActivties(){
        ApiService apiService = retrofit.create(ApiService.class);
        Single <List<Activity>> actListSingle = apiService.getActivities();
        Single <Smartphone> smSingle = apiService.getSmartphone(mSmartphone.getId());
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

    public Smartphone createNewLocalSmartphone(String personUrl){
        Smartphone sm = new Smartphone();
        sm.setPerson(personUrl);
        sm.setName(mController.getDeviceName());
        sm.setSynchronized(false);
        sm.setLogging(false);
        return sm;
    }

    public Single<Pair <List<Activity>, Smartphone >> createSmartphoneAndGetActivities(Smartphone sm){
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

//    public Single<Smartphone> putSmartphoneAPI() {
//        /** puts a smartphone Object to the API without the file
//         *
//         */
//        // create an instance of the ApiService
//        ApiService apiService = retrofit.create(ApiService.class);
//        // make a request by calling the corresponding method
//        return apiService.putSmartphone(this.smartphone.getId(), sm)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread());
//    }
    public Single<ResponseBody> downloadActivityFile(String url){
        ApiService apiService = retrofit.create(ApiService.class);
        return apiService.downloadActivityFile(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    public void downloadActivityFile(String activityFileUrl, ActivityFileHandler activityFile){
        downloadActivityFile(activityFileUrl).subscribe(new SingleObserver<ResponseBody>() {
                @Override
                public void onSubscribe(@NonNull Disposable d) {}

                @Override
                public void onSuccess(@NonNull ResponseBody responseBody) {
                    try {
                        activityFile.replaceActivityFile(mController.getApplicationContext(), responseBody);
                    } catch (IOException e) {
                        mController.createToast("Successfully downloaded activity file but couldn't save");
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    mController.createToast("Could not download activity file.");
                }
        });
    }

    public int getPersonId(){
        // TODO hacky way
        String [] url = mSmartphone.getPerson().split("/");
        try {
            return Integer.parseInt(url[url.length - 2]);
        }catch (Exception e){
            return Integer.parseInt(url[url.length - 1]);
        }
    }

    public Single<Person> getPerson(){
        ApiService apiService = retrofit.create(ApiService.class);
        return apiService.getPerson(getPersonId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Person> uploadActivityFile(Person person, MultipartBody.Part file){
        //https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server
        ApiService apiService = retrofit.create(ApiService.class);

        String predicStr = "false";
        if (person.getPrediction()){
            predicStr = "true";
        }
        // create request body instance from file
        RequestBody name = RequestBody.create(MultipartBody.FORM, person.getName());
        RequestBody hass_name = RequestBody.create(MultipartBody.FORM, person.getHassName());
        RequestBody prediction = RequestBody.create(MultipartBody.FORM, predicStr);
        RequestBody smartphone = RequestBody.create(MultipartBody.FORM, person.getSmartphone());

        return apiService.putPerson(person.getId(), name, hass_name, prediction, smartphone, file)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<ResponseBody> deleteRemoteSmartphone(Smartphone smartphone) {
        /** delete a smartphone object
        */
        ApiService apiService = retrofit.create(ApiService.class);
        return apiService.delSmartphone(smartphone.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Smartphone> getRemoteSmartphone() {
        /** puts a smartphone object
        */
        ApiService apiService = retrofit.create(ApiService.class);
        return apiService.getSmartphone(mSmartphone.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Smartphone> putRemoteSmartphone() {
        /** puts a smartphone object
        */
        ApiService apiService = retrofit.create(ApiService.class);
        mSmartphone.setLogging(mController.getLogging());
        if (mController.getSelectedActivity() == null){
            mSmartphone.setLoggedActivity(null);
        }
        return apiService.putSmartphone(mSmartphone.getId(), mSmartphone)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void putRemoteSmartphone(boolean update){
        putRemoteSmartphone().subscribe(
                smartphone1 -> {
                     if (update) {
                        updateLocalSmartphone(mSmartphone);
                    }
                },
                throwable -> {}
        );
    }

    public Single<Server> getRemoteServer(){
        ApiService apiService = retrofit.create(ApiService.class);
        return apiService.getServer(mServerId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void updateLocalTimeZoneFromRemote(){
        getRemoteServer().subscribe(
                server -> {
                    String tz = server.getTimeZone();
                    setLocalTimeZone(tz);
                },
                throwable -> {}
        );
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
                       mController.onGetActivitiesSuccess(activityNames);
                   }

               @Override
               public void onError(@NonNull Throwable e) {
                   mController.onFailure("GET activities failed");
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
