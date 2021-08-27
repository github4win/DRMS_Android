package com.example.drms;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class Firebase_Messaging_Service extends FirebaseMessagingService  {




    /**
     * 구글 토큰을 얻는 값입니다.
     * 아래 토큰은 앱이 설치된 디바이스에 대한 고유값으로 푸시를 보낼때 사용됩니다.
     * 토큰이 변경되는 경우 호출하게 됨
     * **/
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);



        // 토큰갱신시 작업 필요
        Log.e("Firebase1111111", "FirebaseInstanceIDService : " + s);
        //sendTokenToServer(s);
    }


    /** 메세지를 받았을 경우 그 메세지에 대하여 구현하는 부분입니다. **/
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.e("Firebase Received", "Message Received");
        //Log.e("Firebase Received", "Message : " + remoteMessage.getNotification().getTitle());
        //if(remoteMessage != null && remoteMessage.getNotification() > 0) {
        //Intent myIntent = new Intent("AAA");
        //myIntent.putExtra("sss","fgdfg");
        //this.sendBroadcast(myIntent);
        //sendToActivity(getApplicationContext(), "11","22","33","44");
        sendNotification(remoteMessage);

        //}
    }
    /**
     * remoteMessage 메세지 안애 getData와 getNotification이 있습니다.
     * 이부분은 차후 테스트 날릴때 설명 드리겠습니다.
     * **/
    private void sendNotification(RemoteMessage remoteMessage) {

        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("body");
        //String title = remoteMessage.getNotification().getTitle();
        //String message = remoteMessage.getNotification().getBody();
        Log.e("Firebase title", title);
        Log.e("Firebase message", message);

        /**
         * 오레오 버전부터는 Notification Channel이 없으면 푸시가 생성되지 않는 현상이 있습니다.
         * **/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channel = "채널";
            String channel_nm = "채널명";

            //Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            //intent.putExtra("Result", result.getContents());    // 바코드값 넘기는데 다른거도 가능

            Intent iii = new Intent(this, MainActivity.class);
            //iii.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            iii.putExtra("IsClick",true);
            iii.putExtra("MovePage","daily_maintenance");

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            //TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(iii);
            //stackBuilder.addNextIntentWithParentStack(iii);


            //PendingIntent pending_intent = PendingIntent.getActivity(getApplicationContext(), 0, iii, PendingIntent.FLAG_UPDATE_CURRENT);
            //PendingIntent pending_intent = PendingIntent.getActivity(getApplicationContext(),  0, iii, 0);
            PendingIntent pending_intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
            //PendingIntent pending_intent = PendingIntent.getActivity(getApplicationContext(), 0, iii, 0);

            NotificationManager notichannel = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channelMessage = new NotificationChannel(channel, channel_nm,
                    NotificationManager.IMPORTANCE_HIGH);
            channelMessage.setDescription("채널에 대한 설명.");
            channelMessage.enableLights(true);
            channelMessage.enableVibration(true);
            channelMessage.setShowBadge(false);
            channelMessage.setVibrationPattern(new long[]{100, 200, 100, 200});
            notichannel.createNotificationChannel(channelMessage);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channel)
                            .setSmallIcon(R.mipmap.ic_launcher_drms)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setChannelId(channel)
                            .setAutoCancel(true)
                            .setContentIntent(pending_intent)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            //notificationManager.notify(9999, notificationBuilder.build());
            notificationManager.notify((int)(System.currentTimeMillis()/1000), notificationBuilder.build());

        } else {
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, "")
                            .setSmallIcon(R.mipmap.ic_launcher_drms)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setAutoCancel(true)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify((int)(System.currentTimeMillis()/1000), notificationBuilder.build());

        }

    }

    private void sendToActivity(Context context, String from, String title, String body, String contents) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("aaa","bbb");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
