package com.example.activity_assistant_logger.actassistapi;

import java.util.List;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface ApiService {
    //https://android.jlelse.eu/rest-api-on-android-made-simple-or-how-i-learned-to-stop-worrying-and-love-the-rxjava-b3c2c949cad4?gi=9cd0a432e6fd
    //@GET("server/1/?format=json")
    //Call<Server> getServer();

    @FormUrlEncoded
    @POST("smartphones/")
    Single<Smartphone> createSmartphone(
            @Field("name") String name,
            @Field("person") String personUrl,
            @Field("logged_activity") String loggedActivity,
            @Field("synchronized") boolean _synchronized
    );

    @DELETE("smartphones/{smartphone_id}/")
    Single<ResponseBody> delSmartphone(
            @Path("smartphone_id") int smartphoneId
            );

    @GET("smartphones/{smartphone_id}/?format=json")
    Single<Smartphone> getSmartphone(@Path("smartphone_id") int smartphoneId);

    @GET("persons/{personId}/?format=json")
    Single<Person> getPerson(@Path("personId") int personId);

    //https://stackoverflow.com/questions/44240759/how-to-upload-empty-file-with-retrofit-2
    @Multipart
    @PUT("persons/{personId}/?format=json")
    Single<Person> putPerson(
            @Path("personId") int personId,
            @Part("name") RequestBody name,
            @Part("hass_name") RequestBody person,
            @Part("prediction") RequestBody logging,
            @Part("smartphone") RequestBody smartphone,
            @Part MultipartBody.Part file
            );

    @PUT("smartphones/{smartphone_id}/?format=json")
    Single<Smartphone> putSmartphone(
            @Path("smartphone_id") int smartphoneId,
            @Body Smartphone smartphone
            );

    //https://futurestud.io/tutorials/retrofit-2-how-to-download-files-from-server
    @GET
    Single <ResponseBody> downloadActivityFile(@Url String fileUrl);

    @GET("activities/?format=json")
    Single<List<Activity>> getActivities();
}
