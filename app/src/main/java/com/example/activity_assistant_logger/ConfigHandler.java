package com.example.activity_assistant_logger;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;

public class ConfigHandler {
    //final public String CONNECTION_FILE_NAME = "act_assist.json";
    final public String CONNECTION_FILE_NAME = "test.ser";
    final public String ACTIVITIES_FILE_NAME = "activities.json";

//    public ActAssistApi loadActAssistFromFile(Context appContext, Controller con) throws JSONException, IOException {
//        String json_as_string = readConnectionDataFile(appContext);
//        return ActAssistApi.serializeFromJSON(con, new JSONObject(json_as_string));
//    }
//
//    public void dumpActAssistToFile(Context appContext, ActAssistApi actAssist) throws IOException {
//        JSONObject jsonObject = actAssist.serializeToJSON();
//        this.saveConnectionDataToFile(jsonObject.toString(), appContext);
//    }
    public ActAssistApi loadActAssistFromFile(Context appContext, Controller con) throws JSONException, IOException, ClassNotFoundException {
        FileInputStream is = appContext.openFileInput(CONNECTION_FILE_NAME);
        ObjectInputStream ois = new ObjectInputStream(is);
        ActAssistApi actAssist = (ActAssistApi) ois.readObject();
        ois.close();
        is.close();

        String tmp = readConnectionDataFile(appContext);



        actAssist.initRetrofit();
        actAssist.setController(con);
        return actAssist;
    }

    public void dumpActAssistToFile(Context appContext, ActAssistApi actAssist) throws IOException, JSONException {
        FileOutputStream os = appContext.openFileOutput(CONNECTION_FILE_NAME, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(actAssist);
        oos.close();
        os.close();

        // save activities as list in separate file
        JSONObject dump = new JSONObject();
        dump.put("activities", actAssist.getActivities());
        saveConnectionDataToFile(dump.toString(), appContext);
    }

    public boolean configExists(Context appContext){
       try {
           File apiFile = appContext.getFileStreamPath(CONNECTION_FILE_NAME);
           File actFile = appContext.getFileStreamPath(ACTIVITIES_FILE_NAME);
           return apiFile.exists() && actFile.exists();
       } catch(Exception e){
           return false;
       }
    }

    public void deleteConfigFile(Context appContext){
        File dir = appContext.getFilesDir();
        File file = new File(dir, CONNECTION_FILE_NAME);
        file.delete();
        File file2 = new File(dir, ACTIVITIES_FILE_NAME);
        file2.delete();
    }

////__IO__--------------------------------------------------------------------------------------------
    private void saveConnectionDataToFile(String fileContents, Context appContext) throws IOException {
        FileOutputStream outputStream;
            outputStream = appContext.openFileOutput(ACTIVITIES_FILE_NAME, appContext.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
    }

    private String readConnectionDataFile(Context context) throws IOException {
        String ret = "";
        InputStream inputStream = context.openFileInput(ACTIVITIES_FILE_NAME);
        if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
        }
        return ret;
    }
}
