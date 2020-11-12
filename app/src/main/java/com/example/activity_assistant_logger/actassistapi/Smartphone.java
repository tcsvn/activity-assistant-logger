package com.example.activity_assistant_logger.actassistapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
// used http://www.jsonschema2pojo.org/
// from https://android.jlelse.eu/rest-api-on-android-made-simple-or-how-i-learned-to-stop-worrying-and-love-the-rxjava-b3c2c949cad4

public class Smartphone {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("person")
    @Expose
    private String person;
    @SerializedName("logging")
    @Expose
    private Boolean logging;
    @SerializedName("logged_activity")
    @Expose
    private String loggedActivity;
    @SerializedName("synchronized")
    @Expose
    private Boolean _synchronized;
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

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public Boolean getLogging() {
        return logging;
    }

    public void setLogging(Boolean logging) {
        this.logging = logging;
    }

    public String getLoggedActivity() {
        return loggedActivity;
    }

    public void setLoggedActivity(String loggedActivity) {
        this.loggedActivity = loggedActivity;
    }

    public Boolean getSynchronized() {
        return _synchronized;
    }

    public void setSynchronized(Boolean _synchronized) {
        this._synchronized = _synchronized;
    }

    public String getActivityFile() {
        return activityFile;
    }

    public void setActivityFile(String activityFile) {
        this.activityFile = activityFile;
    }
}

