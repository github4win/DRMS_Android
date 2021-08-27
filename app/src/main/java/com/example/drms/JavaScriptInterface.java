package com.example.drms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

// Web에서 선택한 파일 다운로드 모듈
public class JavaScriptInterface
{
    private Context context;
    public JavaScriptInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data, String FileName, String FileExtension, String Content_type) throws IOException
    {
        convertBase64StringToPdfAndStoreIt(base64Data, FileName, FileExtension, Content_type);
    }

    @JavascriptInterface
    public static String getBase64StringFromBlobUrl(String blobUrl) {
        Log.e("blobUrl", blobUrl);
        if(blobUrl.startsWith("blob")){
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '"+ blobUrl +"', true);" +
                    "xhr.setRequestHeader('Content-type','application/pdf');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            android.getBase64FromBlobData(base64data);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }
    private void convertBase64StringToPdfAndStoreIt(String base64PDf, String FileName, String FileExtension, String Content_type) throws IOException {
        Log.e("BASE 64", base64PDf);
        final int notificationId = 1;
        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
        final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/" + FileName);
        //byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:application/pdf;base64,", ""), 0);
        byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:"+Content_type+";base64,", ""), 0);
        FileOutputStream os;
        try
        {
            os = new FileOutputStream(dwldsPath, false);
            os.write(pdfAsBytes);
            os.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (dwldsPath.exists()) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            //intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf"));
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileExtension));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            String CHANNEL_ID = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ID,"name", NotificationManager.IMPORTANCE_LOW);
                Notification notification = new Notification.Builder(context,CHANNEL_ID)
                        //.setContentText("You have got something new!")
                        //.setContentTitle("File downloaded")
                        .setContentText(FileName)
                        .setContentTitle("다운로드가 완료되었습니다.")
                        .setContentIntent(pendingIntent)
                        .setChannelId(CHANNEL_ID)
                        //.setSmallIcon(android.R.drawable.sym_action_chat)
                        .setSmallIcon(R.mipmap.ic_launcher_foreground_drms)
                        .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }

            } else {
                NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        //.setContentIntent(pendingIntent)
                        .setContentTitle("MY TITLE")
                        .setContentText("MY TEXT CONTENT");

                if (notificationManager != null) {
                    notificationManager.notify(notificationId, b.build());
                    Handler h = new Handler();
                    long delayInMilliseconds = 1000;
                    h.postDelayed(new Runnable() {
                        public void run() {
                            notificationManager.cancel(notificationId);
                        }
                    }, delayInMilliseconds);
                }
            }
        }
        Toast.makeText(context, FileName+"파일이 다운로드가 완료되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
