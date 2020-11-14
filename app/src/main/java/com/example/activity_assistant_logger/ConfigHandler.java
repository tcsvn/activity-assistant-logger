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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.example.activity_assistant_logger.actassistapi.ActAssistApi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ConfigHandler {
    //final public String CONNECTION_FILE_NAME = "act_assist.json";
    final public String CONNECTION_FILE_NAME = "test.ser";

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
        String dump = readConnectionDataFile(appContext);
        JSONObject obj = new JSONObject(dump);
        String act_list = obj.getString("activity_list");
        String [] acts = act_list.split(",");
        acts[0] = acts[0].substring(1);
        acts[acts.length-1] = acts[acts.length-1].substring(0,acts[acts.length-1].length()-1);
        List<String> tmp = new ArrayList<String>();

        for (int i = 0; i < acts.length; i++){
            tmp.add(acts[i]);
        }
        ActAssistApi actAssist = ActAssistApi.serializeFromJSON(con, obj, tmp);
        actAssist.initRetrofit();
        return actAssist;
    }

    public void dumpActAssistToFile(Context appContext, ActAssistApi actAssist) throws IOException, JSONException {
        JSONObject dump = actAssist.serializeToJSON();
        saveConnectionDataToFile(dump.toString(), appContext);
    }

    public boolean configExists(Context appContext){
       try {
           File apiFile = appContext.getFileStreamPath(CONNECTION_FILE_NAME);
           return apiFile.exists();
       } catch(Exception e){
           return false;
       }
    }

    public void deleteConfigFile(Context appContext){
        File dir = appContext.getFilesDir();
        File file = new File(dir, CONNECTION_FILE_NAME);
        file.delete();
    }

////__IO__--------------------------------------------------------------------------------------------
    private void saveConnectionDataToFile(String fileContents, Context appContext) throws IOException {
        FileOutputStream outputStream;
            outputStream = appContext.openFileOutput(CONNECTION_FILE_NAME, appContext.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
    }

    private String readConnectionDataFile(Context context) throws IOException {
        String ret = "";
        InputStream inputStream = context.openFileInput(CONNECTION_FILE_NAME);
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
