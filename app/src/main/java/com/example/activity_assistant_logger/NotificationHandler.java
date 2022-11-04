package com.example.activity_assistant_logger;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class NotificationHandler {


    final static private String CHANNEL_ID = "0";

    public static void createNotification(MainActivity mainActivity, String selectedActivity){
        // define click behaviour
        Intent intent = new Intent(mainActivity, MainActivity.class);
        intent.putExtra("currentActivity", selectedActivity);
        PendingIntent contentIntent = PendingIntent.getActivity(mainActivity, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
                //intent, PendingIntent.FLAG_IMMUTABLE);
        // define notification

        String temp_text = selectedActivity + " " + mainActivity.getString(R.string.notification_text);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mainActivity, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_account_circle_24)
                .setContentTitle(mainActivity.getString(R.string.notification_title))
                .setContentText(temp_text)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent);
        builder.setOngoing(true);
        // publish notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mainActivity);
        notificationManager = NotificationManagerCompat.from(mainActivity);
        notificationManager.notify(0,builder.build());
    }



    public static void createNotificationChannel(MainActivity mainActivity){
        // create notificationchannel
        CharSequence name = mainActivity.getString(R.string.channel_name);
        String desc = mainActivity.getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(desc);
        NotificationManager notificationManager = mainActivity.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }




    public static void removeNotification(MainActivity context){
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(0);
        }
        catch (Exception e){ }
    }



    public static boolean isStartedFromNotification(Intent intent){
        /** returns whether the main acitivity was started by pressing
         * on a new notification
         * */
        String curAct = intent.getStringExtra("currentActivity");
        return curAct != null;
    }


}
