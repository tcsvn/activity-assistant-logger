package com.example.activity_assistant_logger.actassistapi;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    //@GET("smartphones/{smartphone_id}/?format=json")
    //Single<Smartphone> getSmartphoneData(@Path("smartphone_id") int smartphoneId);

    //@GET("server/1/?format=json")
    //Call<Server> getServer();

    @GET("smartphones/{smartphone_id}/?format=json")
    Call<Smartphone> getSmartphone(@Path("smartphone_id") int smartphoneId);

    @PUT("smartphones/{smartphone_id}/?format=json")
    Call<Smartphone> putSmartphone(
            @Path("smartphone_id") int smartphoneId,
            @Body Smartphone smartphone
            );

    @Multipart
    @PUT("smartphones/{smartphone_id}/?format=json")
    Call<Smartphone> putSmartphone(
            @Path("smartphone_id") int smartphoneId,
            @Body Smartphone smartphone,
            @Part MultipartBody.Part file
            );

    @GET("activities/?format=json")
    Call<List<Activity>> getActivities();
}
