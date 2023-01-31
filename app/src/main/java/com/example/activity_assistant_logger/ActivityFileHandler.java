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
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class ActivityFileHandler {

    final static private String ACTIVITY_FILE_NAME="activity.csv";
    // TODO java can only format milliseconds. Change this to microseconds
    final static private String DATE_FORMAT="dd-MM-yyyy HH:mm:ss.SSS";
    final static private String PLACEHOLDER = "###";
    final private SimpleDateFormat dataFormat;
    private boolean isFirstWrite;

    public ActivityFileHandler(Context appContext){
        this.dataFormat = new SimpleDateFormat(DATE_FORMAT);
        this.isFirstWrite = isFirstWrite(appContext);
    }

    private String getCurrentTimestamp(TimeZone tz){
        return this.dataFormat.format(
                Calendar.getInstance(tz).getTime()
        );
    }

    public static String cal2Str(Calendar calendar){
        // TODO critical, set timezone according to activity instance
        return new SimpleDateFormat(DATE_FORMAT).format(calendar.getTime());
    }

    public static Calendar str2Cal(String timestamp) throws ParseException{
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        Date ts = formatter.parse(timestamp);
        Calendar res = Calendar.getInstance();
        res.setTime(ts);
        return res;
    }

    public String getActivity(Context appContext, WeekViewEvent event) throws FileNotFoundException{
        String startTime  = cal2Str(event.getStartTime());
        String endTime  = cal2Str(event.getEndTime());
        String activity = event.getName();
        String line = startTime + "," + endTime + "," + activity;

        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();

        StringBuffer inputBuffer = new StringBuffer();
        for (int i = 0; i < cur_file.size() - 1; i++) {
            if (startTime.equals(cur_file.get(i)[0]) && endTime.equals(cur_file.get(i)[1]) && activity.equals(cur_file.get(i)[2])){
                 String tmp = new StringJoiner(",")
                         .add(cur_file.get(i)[0])
                         .add(cur_file.get(i)[1])
                         .add(cur_file.get(i)[2]).toString();
                 return tmp;
            }
        }
        return "";
    }
    public boolean isActivityInFile(Context appContext, WeekViewEvent activity){
        List<String[]> cur_file = null;
        String startTime  = this.cal2Str(activity.getStartTime());
        String endTime  = this.cal2Str(activity.getEndTime());
        String strActivity = activity.getName();
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
        deleteActivity(appContext, cal2Str(startTime),
                cal2Str(endTime), activity);
    }

    public void deleteActivity(Context appContext, String startTime, String endTime, String activity) throws IOException{
        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        StringBuffer inputBuffer = new StringBuffer();

        for (int i = 0; i < cur_file.size(); i++) {
            String [] row = cur_file.get(i);
            if (!startTime.equals(row[0]) || !endTime.equals(row[1]) || !activity.equals(row[2])){
                addLine2InputBuffer(inputBuffer, row);
            }
        }

        saveInputBuffer2ActivityFile(appContext, inputBuffer);
    }

    public void insertActivity(Context appContext, WeekViewEvent event) throws IOException {
        Calendar startTime = event.getStartTime();
        // todo check if timezone matches
        insertActivity(appContext, startTime, event.getEndTime(), event.getName());
    }

    public void insertActivity(Context appContext, Calendar startTime, Calendar endTime, String activity) throws IOException{
        insertActivity(appContext, cal2Str(startTime), cal2Str(endTime), activity);
    }

   private void addLine2InputBuffer(StringBuffer inputBuffer, String startTime, String endTime, String activity){
       StringJoiner joiner = new StringJoiner(",");
        String tmp = joiner.add(startTime).add(endTime).add(activity).toString();
        inputBuffer.append(tmp + "\n");
   }
   private void addLine2InputBuffer(StringBuffer inputBuffer, String[] row){
        assert row.length == 3;
        addLine2InputBuffer(inputBuffer, row[0], row[1], row[2]);
   }

    public void insertActivity(Context appContext, String startTime, String endTime, String activity) throws IOException{

        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        StringBuffer inputBuffer = new StringBuffer();
        Calendar st = null;
        try {
            st = ActivityFileHandler.str2Cal(startTime);
        } catch (ParseException e){};

        // Add header to file
        addLine2InputBuffer(inputBuffer, cur_file.get(0));

        boolean lineInserted = false;
        for (int i = 1; i < cur_file.size(); i++) {

            Calendar csv_st = null;
            Calendar csv_et = null;
            boolean couldParseSt = true;
            String [] row = cur_file.get(i);

            try {
                csv_st = ActivityFileHandler.str2Cal(row[0]);
                csv_et = ActivityFileHandler.str2Cal(row[1]);
            }catch (ParseException e){couldParseSt = false;};

            // If start time of to insert activity happens after the st & et of previous activity
            if (!lineInserted && couldParseSt && st.compareTo(csv_st) < 0 && st.compareTo(csv_et) < 0) {
                this.addLine2InputBuffer(inputBuffer, startTime, endTime, activity);
                lineInserted = true;
            }
            this.addLine2InputBuffer(inputBuffer, row);
        }

        // Case where the date is later than the last recorded activity
        if (!lineInserted){
            this.addLine2InputBuffer(inputBuffer, startTime, endTime, activity);
        }

        saveInputBuffer2ActivityFile(appContext, inputBuffer);

    }

    public void overwriteActivity(Context appContext, WeekViewEvent oldActivity, WeekViewEvent newActivity) throws FileNotFoundException, IOException{
        /*  This

         */
        String startTime  = this.cal2Str(oldActivity.getStartTime());
        String endTime  = this.cal2Str(oldActivity.getEndTime());
        String strActivity = oldActivity.getName();

        String newStartTime  = this.cal2Str(newActivity.getStartTime());
        String newEndTime  = this.cal2Str(newActivity.getEndTime());
        String newStrActivity = newActivity.getName();

        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        StringBuffer inputBuffer = new StringBuffer();

        for (int i = 0; i < cur_file.size(); i++) {
            String [] row = cur_file.get(i);

            if (startTime.equals(row[0]) && endTime.equals(row[1]) && strActivity.equals(row[2])){
                addLine2InputBuffer(inputBuffer, newStartTime, newEndTime, newStrActivity);
            }
            else{
                addLine2InputBuffer(inputBuffer, row[0], row[1], row[2]);
            }
        }

        saveInputBuffer2ActivityFile(appContext, inputBuffer);
    }

    public ArrayList<? extends WeekViewEvent> getActivitiesAsEvents(Context appContext, List<String> activities){
        ArrayList<WeekViewEvent> events = new ArrayList<WeekViewEvent>();
        List<String[]> cur_file;

        try {
            cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        } catch (FileNotFoundException e) {
            return events;
        }

        int [] colors = appContext.getResources().getIntArray(R.array.categorical);
        Map<String, String> map = new HashMap<String, String>();

        for (int i = 1; i < cur_file.size(); i++){
            String [] row = cur_file.get(i);

            // If there is an invalid or unfinished row skip sample
            if (!this.isRowValid(row, true, (i == cur_file.size()-1), activities)){
                continue;
            }

            Calendar startTime = null;
            Calendar endTime = null;
            String activity = row[2];
            try{
                startTime = str2Cal(row[0]);
                endTime = str2Cal(row[1]);
            } catch(ParseException e){}

            WeekViewEvent event = new WeekViewEvent(
                    startTime.getTimeInMillis(), activity, startTime, endTime);

            if (!map.containsKey(activity)){
                map.put(activity, String.valueOf(map.size()));
            }
            event.setColor(colors[Integer.parseInt(map.get(activity))]);
            events.add(event);
        }

        return events;
    }

    public boolean activityFileExists(Context appContext){
        File file = new File(appContext.getFilesDir(), ACTIVITY_FILE_NAME);
        return file.exists();
    }

    public void cleanupActivityFile(Context appContext, Boolean removePlaceHolder, List<String> activities) throws IOException {
        cleanupActivityFile(appContext, ACTIVITY_FILE_NAME, removePlaceHolder, activities);
    }

    public boolean isRowValid(String [] row, boolean placeIsHolderInvalid, boolean isLastRow, List<String> activities){
        boolean cond1 = (row.length == 3);
        boolean cond2 = (activities.contains(row[2]));
        boolean cond3 = (!row[1].equals(PLACEHOLDER) | (!placeIsHolderInvalid & isLastRow));
        return cond1 & cond2 & cond3;
    }

    public void cleanupActivityFile(Context appContext, String outFileName, Boolean removePlaceHolder, List<String> activities) throws IOException {
        /* Loads activity file, removes invalid indices (columns not equal 3) activities not in list
           and saves the file back to storage.
        * */
        try {
            List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
            if (cur_file.size() > 1){
                // if last line wasn't finished than exclude last line
                StringBuffer inputBuffer = new StringBuffer();
                List<Integer> validIndices = new ArrayList<Integer>();

                // Add line for header
                validIndices.add(0);

                // Iterate through every activity row and check if the length is correct
                // the activity is present or the placeholder is there when it should not be removed in the last row
                for (int i = 1; i < cur_file.size(); i++) {
                    String [] row = cur_file.get(i);
                    if (this.isRowValid(row, removePlaceHolder, (i == cur_file.size()-1), activities)){
                        validIndices.add(i);
                    }
                }

                for (int i = 0; i < validIndices.size(); i++){
                    String [] row = cur_file.get(validIndices.get(i));
                    addLine2InputBuffer(inputBuffer, row);
                }

                FileOutputStream os = appContext.openFileOutput(outFileName, Context.MODE_PRIVATE);
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

    public MultipartBody.Part getActivityMultipart(Context appContext, List<String> activities){
        /* creates a multi part file to put to the api
        * */
        MultipartBody.Part body = null;
        try{
            File file = null;
            if (this.isLastActivityUnfinished(appContext)){
                // Create new file without last activity if it is not finished yet
                String uploadFileName = ACTIVITY_FILE_NAME + ".upload";
                cleanupActivityFile(appContext, uploadFileName, true, activities);
                file = new File(appContext.getFilesDir(), uploadFileName);
            }
            else{
                file = new File(appContext.getFilesDir(), ACTIVITY_FILE_NAME);
            }
            if (!isFirstWrite(appContext)){
               RequestBody requestFile =
                       RequestBody.create(MediaType.parse("multipart/form-data"), file);
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
        } catch (IOException e)
        {
            e.printStackTrace();
        };
        return body;
    }

    public void createNewActivity(Context appContext, String new_activity, TimeZone tz) throws IOException {
        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_APPEND);
        String row = getCurrentTimestamp(tz) + "," + this.PLACEHOLDER + "," + new_activity;
        if (! this.isFirstWrite){
            row = "\n" + row;
        }
        os.write(row.getBytes());
        os.close();
    }

    public void addActivity(Context appContext, String new_activity, TimeZone tz) throws IOException {
        /** finishes a begun line and starts a new one
        * */
        this.finishLastActivity(appContext, tz);
        this.createNewActivity(appContext, new_activity, tz);
    }

    public void finishLastActivity(Context appContext, TimeZone tz) throws IOException {
        /** finishes a begun line
         * */
        final String cur_ts = getCurrentTimestamp(tz);
        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();

        String[] last_row = cur_file.get(cur_file.size() - 1);
        assert this.PLACEHOLDER.equals(last_row[1]);
        final String new_row = last_row[0] + "," + cur_ts + "," + last_row[2];

        // Read file as input buffer without last row
        StringBuffer inputBuffer = new StringBuffer();
        for (int i = 0; i < cur_file.size() - 1; i++) {
            String [] row = cur_file.get(i);
            addLine2InputBuffer(inputBuffer, row);
        }

        // Add last row
        inputBuffer.append(new_row);

        // Save file
        saveInputBuffer2ActivityFile(appContext, inputBuffer);
    }

    private void saveInputBuffer2ActivityFile(Context appContext, StringBuffer inputBuffer) throws IOException{
        FileOutputStream os = appContext.openFileOutput(ACTIVITY_FILE_NAME, Context.MODE_PRIVATE);
        os.write(inputBuffer.toString().getBytes());
        os.close();
    }

    public String getLastActivityName(Context appContext) throws IOException{
        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        String[] last_row = cur_file.get(cur_file.size() - 1);
        return last_row[2];
    }

    public boolean isLastActivityUnfinished(Context appContext) throws IOException{
        /* Checks if there are unfinished activities in the file. Happens when an activity is logged
           and the application is closed or suspended.
         */
        List<String[]> cur_file = new CSVFile(appContext.openFileInput(ACTIVITY_FILE_NAME)).read();
        String[] last_row = cur_file.get(cur_file.size() - 1);
        return this.PLACEHOLDER.equals(last_row[1]);
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


