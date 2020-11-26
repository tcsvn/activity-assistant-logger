package com.example.activity_assistant_logger.actassistapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Person {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("hass_name")
    @Expose
    private String hassName;
    @SerializedName("prediction")
    @Expose
    private Boolean prediction;
    @SerializedName("smartphone")
    @Expose
    private String smartphone;
    @SerializedName("activity_file")
    @Expose
    private String activityFile;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHassName() {
        return hassName;
    }

    public void setHassName(String hassName) {
        this.hassName = hassName;
    }

    public Boolean getPrediction() {
        return prediction;
    }

    public void setPrediction(Boolean prediction) {
        this.prediction = prediction;
    }

    public String getSmartphone() {
        return smartphone;
    }

    public void setSmartphone(String smartphone) {
        this.smartphone = smartphone;
    }

    public String getActivityFile() {
        return activityFile;
    }

    public void setActivityFile(String activityFile) {
        this.activityFile = activityFile;
    }

}
