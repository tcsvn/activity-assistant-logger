package com.example.activity_assistant_logger.actassistapi;

 import com.google.gson.annotations.Expose;
 import com.google.gson.annotations.SerializedName;

public class Server {

    @SerializedName("server_address")
    @Expose
    private String serverAddress;
    @SerializedName("setup")
    @Expose
    private String setup;
    @SerializedName("time_zone")
    @Expose
    private String timeZone;
    @SerializedName("hass_api_token")
    @Expose
    private String hassApiToken;
    @SerializedName("hass_db_url")
    @Expose
    private String hassDbUrl;
    @SerializedName("hass_comp_installed")
    @Expose
    private Boolean hassCompInstalled;
    @SerializedName("selected_model")
    @Expose
    private Object selectedModel;
    @SerializedName("realtime_node")
    @Expose
    private Object realtimeNode;
    @SerializedName("dataset")
    @Expose
    private String dataset;
    @SerializedName("poll_interval")
    @Expose
    private String pollInterval;
    @SerializedName("is_polling")
    @Expose
    private Boolean isPolling;
    @SerializedName("zero_conf_pid")
    @Expose
    private Object zeroConfPid;
    @SerializedName("poll_service_pid")
    @Expose
    private Integer pollServicePid;
    @SerializedName("plot_gen_service_pid")
    @Expose
    private Object plotGenServicePid;
    @SerializedName("webhook_count")
    @Expose
    private Integer webhookCount;

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getSetup() {
        return setup;
    }

    public void setSetup(String setup) {
        this.setup = setup;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getHassApiToken() {
        return hassApiToken;
    }

    public void setHassApiToken(String hassApiToken) {
        this.hassApiToken = hassApiToken;
    }

    public String getHassDbUrl() {
        return hassDbUrl;
    }

    public void setHassDbUrl(String hassDbUrl) {
        this.hassDbUrl = hassDbUrl;
    }

    public Boolean getHassCompInstalled() {
        return hassCompInstalled;
    }

    public void setHassCompInstalled(Boolean hassCompInstalled) {
        this.hassCompInstalled = hassCompInstalled;
    }

    public Object getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(Object selectedModel) {
        this.selectedModel = selectedModel;
    }

    public Object getRealtimeNode() {
        return realtimeNode;
    }

    public void setRealtimeNode(Object realtimeNode) {
        this.realtimeNode = realtimeNode;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(String pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Boolean getIsPolling() {
        return isPolling;
    }

    public void setIsPolling(Boolean isPolling) {
        this.isPolling = isPolling;
    }

    public Object getZeroConfPid() {
        return zeroConfPid;
    }

    public void setZeroConfPid(Object zeroConfPid) {
        this.zeroConfPid = zeroConfPid;
    }

    public Integer getPollServicePid() {
        return pollServicePid;
    }

    public void setPollServicePid(Integer pollServicePid) {
        this.pollServicePid = pollServicePid;
    }

    public Object getPlotGenServicePid() {
        return plotGenServicePid;
    }

    public void setPlotGenServicePid(Object plotGenServicePid) {
        this.plotGenServicePid = plotGenServicePid;
    }

    public Integer getWebhookCount() {
        return webhookCount;
    }

    public void setWebhookCount(Integer webhookCount) {
        this.webhookCount = webhookCount;
    }

}

