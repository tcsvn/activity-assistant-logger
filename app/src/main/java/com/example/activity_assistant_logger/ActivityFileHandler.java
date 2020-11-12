package com.example.activity_assistant_logger;

import android.content.Context;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringJoiner;

public class ActivityFileHandler {

    final private String ACTIVITY_FILE_NAME="activity.csv";
    final private String DATE_FORMAT="dd-MM-yyy HH:mm:ss";
    final private SimpleDateFormat dataFormat;
    private boolean isFirstWrite;
    public ActivityFileHandler(Context appContext){
        this.dataFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        this.isFirstWrite = isFirstWrite(appContext);
    }

    private String getCurrentTimestamp(){
        return this.dataFormat.format(
                Calendar.getInstance().getTime()
        );
    }
    public boolean isFirstWrite(Context appContext){
        File file = new File(ACTIVITY_FILE_NAME);
        return !file.exists();
    }

    public String getActivityFileAsString(Context appContext) throws FileNotFoundException {
        FileInputStream is = appContext.openFileInput(ACTIVITY_FILE_NAME);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = is.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = is.read();
            }
            is.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }

    public void setActivityFile(Context appContext, StringBuffer sbf) throws IOException {
    }

    //public File getActivityFile(){
    //    return new File();
    //}
    public boolean deleteActivityFile(Context appContext){
        return appContext.deleteFile(ACTIVITY_FILE_NAME);
    }

    public void createActivity(Context appContext, String new_activity) throws IOException {
        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_APPEND);
        String row;
        if (this.isFirstWrite){
            row = getCurrentTimestamp();
            this.isFirstWrite = false;
        }
        else {
            row = "\n" + getCurrentTimestamp();
        }
        os.write(row.getBytes());
        os.close();
    }

    public void addActivity(Context appContext, String old_activity, String new_activity) throws IOException {
        /** finishes a begun line and starts a new one
        * */
        this.finishActivity(appContext, old_activity);
        this.createActivity(appContext, new_activity);
    }

    public void finishActivity(Context appContext, String activity) throws IOException {
        /** finishes a begun line
         * */
        final String cur_ts = getCurrentTimestamp();
        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        String old_ts = cur_file.get(cur_file.size() - 1)[0];
        final String new_row = old_ts + "," + cur_ts + "," + activity;

        StringBuffer inputBuffer = new StringBuffer();
        for (int i = 0; i < cur_file.size() - 1; i++) {
            StringJoiner joiner = new StringJoiner(",");
            String tmp = joiner.add(cur_file.get(i)[0])
                    .add(cur_file.get(i)[1])
                    .add(cur_file.get(i)[2]).toString();
            inputBuffer.append(tmp + "\n");
        }
        inputBuffer.append(new_row);
        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_PRIVATE);
        os.write(inputBuffer.toString().getBytes());
        os.close();
    }

    private class CSVFile {
        InputStream inputStream;

        public CSVFile(InputStream inputStream){
            this.inputStream = inputStream;
        }

        public List<String[]> read(){
            List<String[]> resultList = new ArrayList<String[]>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String csvLine;
                while ((csvLine = reader.readLine()) != null) {
                    resultList.add(csvLine.split(","));
                }
            }
            catch (IOException ex) {
                throw new RuntimeException("Error in reading CSV file: "+ex);
            }
            finally {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("Error while closing input stream: "+e);
                }
            }
            return resultList;
        }
    }
}


