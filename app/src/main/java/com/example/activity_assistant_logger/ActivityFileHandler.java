package com.example.activity_assistant_logger;

import android.content.Context;

import com.example.activity_assistant_logger.weekview.WeekViewEvent;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class ActivityFileHandler {

    final static private String ACTIVITY_FILE_NAME="activity.csv";
    final static private String DATE_FORMAT="dd-MM-yyy HH:mm:ss.SSS";
    final private SimpleDateFormat dataFormat;
    private boolean isFirstWrite;

    public ActivityFileHandler(Context appContext){
        this.dataFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        this.isFirstWrite = isFirstWrite(appContext);
    }

    private String getCurrentTimestamp(){
        return this.dataFormat.format(
                Calendar.getInstance().getTime()
        );
    }

    public static String cal2Str(Calendar calendar, Locale locale){
        // TODO critical, set timezone according to activity instance
        return new SimpleDateFormat(DATE_FORMAT, locale).format(calendar.getTime());
    }
    public static Calendar str2Cal(String timestamp, Locale locale) throws ParseException{
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:s.S", locale);
        Date ts = formatter.parse(timestamp);
        Calendar res = Calendar.getInstance();
        res.setTime(ts);
        return res;
    }

    public String getActivity(Context appContext, WeekViewEvent event) throws FileNotFoundException{
        String startTime  = cal2Str(event.getStartTime(), Locale.getDefault());
        String endTime  = cal2Str(event.getEndTime(), Locale.getDefault());
        String activity = event.getName().toString();
        String line = startTime + "," + endTime + "," + activity;

        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();

        StringBuffer inputBuffer = new StringBuffer();
        for (int i = 0; i < cur_file.size() - 1; i++) {
            if (startTime.equals(cur_file.get(i)[0]) && endTime.equals(cur_file.get(i)[1]) && activity.equals(cur_file.get(i)[2])){
                 StringJoiner joiner = new StringJoiner(",");
                 String tmp = joiner.add(cur_file.get(i)[0])
                        .add(cur_file.get(i)[1])
                        .add(cur_file.get(i)[2]).toString();
                 return tmp;
            }
        }
        return "";
    }
    public boolean isActivityInFile(Context appContext, WeekViewEvent activity){
        List<String[]> cur_file = null;
        String startTime  = this.cal2Str(activity.getStartTime(), Locale.getDefault());
        String endTime  = this.cal2Str(activity.getEndTime(), Locale.getDefault());
        String strActivity = activity.getName().toString();
        try {
            cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        } catch (FileNotFoundException e){
            return false;
        }
        for (int i = 0; i < cur_file.size(); i++) {
            if (startTime.equals(cur_file.get(i)[0]) && endTime.equals(cur_file.get(i)[1]) && strActivity.equals(cur_file.get(i)[2])){
                 return true;
            }
        }
        return false;
    }

    public void deleteActivity(Context appContext, WeekViewEvent event) throws IOException {
        deleteActivity(appContext, event.getStartTime(), event.getEndTime(), event.getName());
    }

    public void deleteActivity(Context appContext, Calendar startTime, Calendar endTime, String activity) throws IOException{
        deleteActivity(appContext, cal2Str(startTime, Locale.getDefault()),
                cal2Str(endTime, Locale.getDefault()), activity);
    }

    public void deleteActivity(Context appContext, String startTime, String endTime, String activity) throws IOException{
        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        StringBuffer inputBuffer = new StringBuffer();

        for (int i = 0; i < cur_file.size(); i++) {
            StringJoiner joiner = new StringJoiner(",");
            if (!startTime.equals(cur_file.get(i)[0]) || !endTime.equals(cur_file.get(i)[1]) || !activity.equals(cur_file.get(i)[2])){
                String tmp = joiner.add(cur_file.get(i)[0])
                            .add(cur_file.get(i)[1])
                        .add(cur_file.get(i)[2]).toString();
                    inputBuffer.append(tmp + "\n");
                }
            }


        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_PRIVATE);
        os.write(inputBuffer.toString().getBytes());
        os.close();
    }

    public void insertActivity(Context appContext, WeekViewEvent event) throws IOException {
        insertActivity(appContext, event.getStartTime(), event.getEndTime(), event.getName());
    }

    public void insertActivity(Context appContext, Calendar startTime, Calendar endTime, String activity) throws IOException{
        insertActivity(appContext, cal2Str(startTime, Locale.getDefault()), cal2Str(endTime, Locale.getDefault()), activity);
    }

    public void insertActivity(Context appContext, String startTime, String endTime, String activity) throws IOException{

        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        StringBuffer inputBuffer = new StringBuffer();
        Calendar st = null;
        try {
            st = ActivityFileHandler.str2Cal(startTime, Locale.getDefault());
        } catch (ParseException e){};
        boolean lineInserted = false;
        for (int i = 0; i < cur_file.size(); i++) {
            StringJoiner joiner = new StringJoiner(",");
            String tmp = null;
            // TODO
            Calendar csv_st = null;
            boolean couldParseSt = true;
            try {
                csv_st = ActivityFileHandler.str2Cal(cur_file.get(i)[0], Locale.getDefault());
            }catch (ParseException e){couldParseSt = false;};

            if (couldParseSt && st.compareTo(csv_st) < 0 && !lineInserted) {
                tmp = joiner.add(startTime).add(endTime).add(activity).toString();
                lineInserted = true;
            }
            else{
                tmp = joiner.add(cur_file.get(i)[0])
                        .add(cur_file.get(i)[1])
                        .add(cur_file.get(i)[2]).toString();
            }
            inputBuffer.append(tmp + "\n");
        }
        // Case where the date is later than the last recorded activity
        if (!lineInserted){
            StringJoiner joiner = new StringJoiner(",");
            String tmp = joiner.add(startTime).add(endTime).add(activity).toString();
            inputBuffer.append(tmp + "\n");
        }

        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_PRIVATE);
        os.write(inputBuffer.toString().getBytes());
        os.close();
    }

    public void overwriteActivity(Context appContext, WeekViewEvent oldActivity, WeekViewEvent newActivity) throws FileNotFoundException, IOException{
        /*  This

         */
        String startTime  = this.cal2Str(oldActivity.getStartTime(), Locale.getDefault());
        String endTime  = this.cal2Str(oldActivity.getEndTime(), Locale.getDefault());
        String strActivity = oldActivity.getName().toString();

        String newStartTime  = this.cal2Str(newActivity.getStartTime(), Locale.getDefault());
        String newEndTime  = this.cal2Str(newActivity.getEndTime(), Locale.getDefault());
        String newStrActivity = newActivity.getName().toString();

        final String new_row = newStartTime + "," + newEndTime + "," + newStrActivity;

        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        StringBuffer inputBuffer = new StringBuffer();

        for (int i = 0; i < cur_file.size(); i++) {
            StringJoiner joiner = new StringJoiner(",");
            String tmp = null;
            if (startTime.equals(cur_file.get(i)[0]) && endTime.equals(cur_file.get(i)[1]) && strActivity.equals(cur_file.get(i)[2])){
                 tmp = joiner.add(newStartTime).add(newEndTime).add(newStrActivity).toString();
            }
            else{
                tmp = joiner.add(cur_file.get(i)[0])
                                   .add(cur_file.get(i)[1])
                                   .add(cur_file.get(i)[2]).toString();
            }
            inputBuffer.append(tmp + "\n");
        }

        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_PRIVATE);
        os.write(inputBuffer.toString().getBytes());
        os.close();


    }

    public ArrayList<? extends WeekViewEvent> getActivitiesAsEvents(Context appcontext){
        ArrayList<WeekViewEvent> events = new ArrayList<WeekViewEvent>();
        String str = null;
        try {
            str = this.getActivityFileAsString(appcontext);
        } catch (FileNotFoundException e) {
            return events;
        }
        BufferedReader bufReader = new BufferedReader(new StringReader(str));
        String line = null;
        Boolean condition = false;
        try {
            bufReader.readLine(); // Skip csv header
            condition = (line = bufReader.readLine()) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        int [] colors = appcontext.getResources().getIntArray(R.array.categorical);
        Map<String, String> map = new HashMap<String, String>();

        while (condition) {

            String[] values = line.split(",");
            // Filter out activities that are not yet finished
            if (values.length != 3){
                break;
            }
            Calendar startTime = null;
            Calendar endTime = null;
            String activity = values[2];

            try{
                startTime = str2Cal(values[0], Locale.getDefault());
                endTime = str2Cal(values[1], Locale.getDefault());
            } catch(ParseException e){}
            WeekViewEvent event = new WeekViewEvent(
                    startTime.getTimeInMillis(), activity, startTime, endTime);

            if (!map.containsKey(activity)){
                map.put(activity, String.valueOf(map.size()));
            }
            event.setColor(colors[Integer.parseInt(map.get(activity))]);
            events.add(event);
            try{
                condition = (line = bufReader.readLine()) != null;
            } catch (IOException e){
               e.printStackTrace();
            }
        }

        return events;
    }

    public boolean activityFileExists(Context appContext){
        File file = new File(appContext.getFilesDir(), ACTIVITY_FILE_NAME);
        return file.exists();
    }

    public void cleanupActivityFile(Context appContext) throws IOException {
        /* Loads activity file, removes invalid indices (columns not equal 3)
           and saves the file back to storage.
        * */
        try {
            List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
            String [] lastLine = cur_file.get(cur_file.size() - 1);
            if (lastLine.length != 3 && cur_file.size() > 1){
                // if last line wasn't finished than exclude last line
                StringBuffer inputBuffer = new StringBuffer();
                List<Integer> validIndices = new ArrayList<Integer>();
                for (int i = 0; i < cur_file.size() - 1; i++) {
                    if (cur_file.get(i).length == 3) {
                        validIndices.add(i);
                    }
                }
                for (int i =0; i < validIndices.size(); i++){
                    StringJoiner joiner = new StringJoiner(",");
                    String tmp = joiner.add(cur_file.get(i)[0])
                                       .add(cur_file.get(i)[1])
                                       .add(cur_file.get(i)[2]).toString();
                    // for the last line don't add a \n
                    if (i < validIndices.size()-2) {
                        inputBuffer.append(tmp + "\n");
                    }
                    else {
                        inputBuffer.append(tmp + "\n");
                    }
                }
                FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_PRIVATE);
                os.write(inputBuffer.toString().getBytes());
                os.close();
            }
        }
        catch(FileNotFoundException e){
        }
    }

    public boolean isFirstWrite(Context appContext){
        try {
            List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
            boolean tmp = cur_file.size() > 1;
            return !tmp;
        }
        catch(FileNotFoundException e){
            return true;
        }
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

    public boolean replaceActivityFile(Context appContext, ResponseBody body) throws IOException {
        File file = new File(appContext.getFilesDir(), ACTIVITY_FILE_NAME);
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            byte[] fileReader = new byte[4096];

            long fileSize = body.contentLength();
            long fileSizeDownloaded = 0;

            inputStream = body.byteStream();
            outputStream = new FileOutputStream(file);

            while (true) {
                int read = inputStream.read(fileReader);

                if (read == -1) {
                    break;
            }

            outputStream.write(fileReader, 0, read);

            fileSizeDownloaded += read;
        }

        outputStream.flush();
        this.isFirstWrite = false;
        return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public boolean deleteActivityFile(Context appContext){
        this.isFirstWrite = true;
        return appContext.deleteFile(ACTIVITY_FILE_NAME);
    }

    public MultipartBody.Part getActivityMultipart(Context appContext){
        /* creates a multi part file to put to the api
        * */
        MultipartBody.Part body = null;
        try{
            File file = new File(appContext.getFilesDir(), ACTIVITY_FILE_NAME);
            if (!isFirstWrite(appContext)){
               RequestBody requestFile =
                       RequestBody.create(MediaType.parse("multipart/form-data"),file);
               body = MultipartBody.Part.createFormData(
                       "activity_file", file.getName(), requestFile);
            }
            else{
                RequestBody attachmentEmpty = RequestBody.create(
                        MediaType.parse("text/plain"), "");
                body = MultipartBody.Part.createFormData(
                        "activity_file", "", attachmentEmpty);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return body;
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
                    String [] line = csvLine.split(",");
                    // don't add empty lines
                    if (line.length != 1 || line[0].length() != 0){
                        resultList.add(line);
                    }
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


