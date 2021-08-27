package com.example.drms;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;

import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import android.provider.DocumentsContract;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.lang.Object;



public class MainActivity extends AppCompatActivity
{
    //private static final String URL_Navigate_Url = "http://152.149.8.161:58734/";
    private static final String URL_Navigate_Url = "http://drms_mobile2.win-it.co.kr/";
    //private static final String URL_Navigate_Url = "http://192.168.0.15:8081/";


    String FCM_Token = ""; // Push Notification을 받기 위한 Token
    Boolean IsClickAction = false; // Notification을 클릭해서 들어오면 true
    String ClickAction_Page = ""; // Notification을 클릭해서 들어왔을때 이동해야 할 Page Parameter (Webview 전달)
    private CustomDialog Loading_Dialog; // 로딩 다이얼로그

    Context _context;
    private WebView webView;
    private long lastTimeBackPressed;;


    // 웹에서 앱으로 보낸 데이터를 받기 위해 Handler 선언
    private final Handler handler = new Handler();

    private static final int FILECHOOSER_RESULTCODE   = 1;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessages;
    private Uri mCapturedImageURI = null;

    // 백버튼 터치시 웹뷰페이지 뒤로 가기. 더이상 뒤로 갈곳이 없으면 연속 두번 터치시 종료
    @Override
    public void onBackPressed()
    {
        if (webView.canGoBack())
        {
            webView.goBack();
        }
        else
        {
            if (System.currentTimeMillis() - lastTimeBackPressed < 1500)
            {
                finish();
                return;
            }
            lastTimeBackPressed = System.currentTimeMillis();
            Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        _context = this;

        Loading_Dialog = new CustomDialog(MainActivity.this);

        // 푸쉬를 통해서 왔는지 확인한다.
        Intent _callintent = getIntent();
        Boolean Click_Check = _callintent.getBooleanExtra("IsClick", false);
        if(Click_Check != null && Click_Check) {
            IsClickAction = true;
            ClickAction_Page = _callintent.getStringExtra("MovePage");
        }

        //Runtime External storage permission for saving download files
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int PERMISSION_ALL = 1;
            String[] PERMISSIONS = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };

            // 권한 체크
            if(!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        // Push Token 발행
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if(!task.isSuccessful()) {
                    Log.w("FireBase", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                FCM_Token = token;
                // Log and toast
                //String msg = getString(R.string.msg_token_fmt, token);
                Log.d("FireBase", FCM_Token);
                //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Push Subject 구독 (전체발행을 위함)
        FirebaseMessaging.getInstance().subscribeToTopic("ALL");

        // APP remove title (line을 삭제하는것만으로 충분하다.)
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        {
            webView = (WebView)findViewById(R.id.webview);

            // 자바스크립트 사용을 허용한다.
            webView.getSettings().setJavaScriptEnabled(true); // 자바스크립트 허용용
           webView.getSettings().setDomStorageEnabled(true); // 저장공간 접근 허용
            //webView.getSettings().setCacheMode(webView.getSettings().LOAD_CACHE_ELSE_NETWORK);
            webView.getSettings().setAllowFileAccess(true); // 파일 접근 활성화
            webView.getSettings().setAllowContentAccess(true); // 웹뷰를 통해 content URL에 접근할지 여부 (아마 업로드에서 사용할듯)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.setWebContentsDebuggingEnabled(true);
            }

            // 새로운 창을 띄우지 않고 내부에서 웹뷰를 실행시킨다.
            webView.setWebViewClient(new WebViewClient()
            {
                // https 의 SSL 인증서 에러 처리시 무시하고 그냥 시작
                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
                {
                    handler.proceed();
                }

                // Webview Page Loading 시작시
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon)
                {
                    // 로딩 다이얼로그 시작
                    Loading_Dialog.show();
                }

                // Webview Page Loading 종료시
                @Override
                public void onPageFinished(WebView view, String url)
                {
                    // 로딩 다이얼로그 종료
                    Loading_Dialog.dismiss();
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("tel:")) {
                        Intent call_phone = new Intent(Intent.ACTION_CALL);
                        call_phone.setData(Uri.parse(url));
                        startActivity(call_phone); // 권한 설정은 Loing.java에서 처리했음
                    } else if (url.startsWith("sms:")) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                        startActivity(intent);
                    } else if (url.startsWith("intent:")) {
                        try {
                            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                            if (existPackage != null) {
                                startActivity(intent);
                            } else {
                                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                                startActivity(marketIntent);
                            }
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        view.loadUrl(url);
                    }
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient()
            {
                // 위치 권한 요청
                @Override
                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)
                {
                    super.onGeolocationPermissionsShowPrompt(origin, callback);
                    callback.invoke(origin, true, false);
                }

                // javascript의 alert과 confirm을 인식하기 위해서는 onJsAlert과 onJsConfirm 메서드가 있어야 합니다.
                // 없는 경우 url이 confrim title로 뜨기 때문에 url 노출을 막을수 없다.
                @Override
                public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)
                {
                    new AlertDialog.Builder(view.getContext()).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            result.confirm();
                        }
                    }).setCancelable(true).create().show();

                    return true;
                }

                @Override
                public boolean onJsConfirm(WebView view, String url, String message, final android.webkit.JsResult result)
                {
                    new AlertDialog.Builder(view.getContext()).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            result.confirm();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            result.cancel();
                        }
                    }).create().show();
                    return true;
                }

                // openFileChooser for Android 3.0+
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){
                    mUploadMessage = uploadMsg;
                    openImageChooser();
                }

                // For Lollipop 5.0+ Devices
                public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                    mUploadMessages = filePathCallback;
                    openImageChooser();
                    return true;
                }

                // openFileChooser for Android < 3.0
                public void openFileChooser(ValueCallback<Uri> uploadMsg){
                    openFileChooser(uploadMsg, "");
                }

                //openFileChooser for other Android versions
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                    openFileChooser(uploadMsg, acceptType);
                }

                private void openImageChooser() {
                    try {
                        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FolderName");
                        if (!imageStorageDir.exists()) {
                            imageStorageDir.mkdirs();
                        }
                        File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                        mCapturedImageURI = Uri.fromFile(file);

                        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        i.setType("image/*");

                        Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });

            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength)
                {
                    try {
                        webView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url));

                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setMimeType(mimeType);
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setDescription("Downloading file");
                        String cookies = CookieManager.getInstance().getCookie(url);
                        request.addRequestHeader("cookie", cookies);
                        String fileName = contentDisposition.replace("inline; filename=", "");
                        fileName = fileName.replaceAll("\"", "");
                        request.setTitle(fileName);
                        //request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        /*request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);*/
                        request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                                        url, contentDisposition, mimeType));
                        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Should we show an explanation?
                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                Toast.makeText(getBaseContext(), "첨부파일 다운로드를 위해\n동의가 필요합니다.", Toast.LENGTH_LONG).show();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        110);
                            } else {
                                Toast.makeText(getBaseContext(), "첨부파일 다운로드를 위해\n동의가 필요합니다.", Toast.LENGTH_LONG).show();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        110);
                            }
                        }
                    }
                }
            });

            // 웹과 안드로이드 간의 연결을 위해 CallByWebSite 클래스를 만들고 addJavascriptInterface 합수를 사용해줍니다.
            // name 은 Example.html 파일에서 설정했던 'myJs'로 설정합니다.
            // Vue 사용시 Vue에 설정 해둔 이름으로
            webView.addJavascriptInterface(new MainActivity.CallByWebSite(), "myJs");
            webView.addJavascriptInterface(new JavaScriptInterface(this), "myJs2");

            // 웹뷰 캐시 삭제 (속도저하 문제로 일단 주석처리함 2021-06-30)
            //webView.clearCache(true);
            //webView.clearHistory();

            webView.loadUrl(URL_Navigate_Url);

            // 안드로이드 SDK 버전이 21 이상일 경우 오류가 발생할 수 있으므로 그에 따른 소스를 적어서 처리해줍니다.
            // 혼합된 콘텐츠 사용 허용
            if (Build.VERSION.SDK_INT >= 21)
            {
                webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
        }
    }

    // 권한 체크
    public static boolean hasPermissions(Context context, String... permissions)
    {
        if(context != null && permissions != null) {
            for(String permission : permissions) {
                if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // Website에서 호출 Method Class
    public class CallByWebSite
    {
        // 생성자
        public CallByWebSite() {}
        public CallByWebSite(Context context) {
            this.context = context;
        }

        private Context context;

        // QR을 찍기 위한 카메라 Activity 호출
        @JavascriptInterface
        public void callAndroid(final String str)
        {
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    Intent intent = new Intent(MainActivity.this, Activity_barcode.class);
                    intent.putExtra("Type", str);
                    startActivityForResult(intent, 0);
                }
            });
        }

        // WebView에서 FCM Token을 요청했을때 반환한다.
        @JavascriptInterface
        public String GetUserToken() {
            return FCM_Token;
        }

        // Push를 클릭했을때 필요한 값을 반환한다.
        @JavascriptInterface
        public String GetIntent_Data() {
            String StrData = "";

            // Push 클릭시 전달할 Page Parameter 구성
            if(IsClickAction) {
                StrData = "{\"MovePage\":\""+ClickAction_Page+"\"}";
            }

            // Push관련 전역변수 초기화
            IsClickAction = false;
            ClickAction_Page = "";

            return StrData;
        }
    }

    // 바코드에서 스캔한 결과를 받아오는 부분 /  파일 첨부 부분
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // 웹뷰 파일 업로드
        if(requestCode==FILECHOOSER_RESULTCODE) {

            if (null == mUploadMessage && null == mUploadMessages) {
                return;
            }

            if (null != mUploadMessage) { // 파일 단일
                handleUploadMessage(requestCode, resultCode, data);
            } else if (mUploadMessages != null) { // 파일 다중
                handleUploadMessages(requestCode, resultCode, data);
            }
        }
        else { // 바코드
            if(resultCode == RESULT_OK) {
                // 결과를 data (intent 형식) 에서 가져옴
                String result = data.getStringExtra("Result");

                // 안드로이드에서 웹의 setMessage() function 호출
                webView.loadUrl("javascript:setMessage('" + result + "')");
            }
        }
//        // Activity_barcode.java 에서 'setResult(RESULT_OK, intent);' 를 사용해서 다시 화면으로 돌아오는데 resultCode 가 RESULT_OK로 두 부분 일치시켜줘야함.
        //mFilePathCallback = null;
    }

    private void handleUploadMessage(int requestCode, int resultCode, Intent intent) {
        Uri result = null;
        try {
            if (resultCode != RESULT_OK) {
                result = null;
            } else {
                // retrieve from the private variable if the intent is null

                result = intent == null ? mCapturedImageURI : intent.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void handleUploadMessages(int requestCode, int resultCode, Intent intent) {
        Uri[] results = null;
        try {
            if (resultCode != RESULT_OK) {
                results = null;
            } else {
                if (intent != null) {
                    String dataString = intent.getDataString();
                   /* String imgPath = getPath(this, intent.getData());
                    String tt = getResizeFileImage(this, imgPath, 6, 100,100);
                    String tt_path = getPath(this, Uri.parse(tt));

                    ExifInterface oldExif = new ExifInterface(imgPath);
                    String exifOrientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION);

                    if (exifOrientation != null) {
                        ExifInterface newExif = new ExifInterface(tt_path);
                        newExif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);
                        newExif.saveAttributes();
                    }*/




                    ClipData clipData = intent.getClipData();
                    if (clipData != null) {
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                    /*if (tt != null) {
                        results = new Uri[]{Uri.parse(tt)};
                    }*/
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                } else {
                    results = new Uri[]{mCapturedImageURI};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mUploadMessages.onReceiveValue(results);
        mUploadMessages = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT; // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
                // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId( Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null); }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0]; Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection, selectionArgs); }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /** * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path. */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getResizeFileImage(Context _cc, String file_route, int size, int width, int height) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();

        File ff = new File(file_route);
        long sssi = ff.length();
        long max_byte = 51000; // 50kb
        int k = 0;
        while(sssi > max_byte)
        {
            sssi = sssi / 2;
            k = k + 1;
        }

        k = k*2;




        //options.inSampleSize = size;
        options.inSampleSize = k;
        Bitmap src = BitmapFactory.decodeFile(file_route, options);
        long sssss = src.getByteCount();
        long sssss2 = src.getRowBytes();
        int width2 = src.getWidth();
        int height2 =src.getHeight();
        //long sssss3 = src.getPixel(width2, height2);

        //int size1 = src.getByteCount();
        Bitmap resized = Bitmap.createScaledBitmap(src, src.getWidth(), src.getHeight(), true);
        //Bitmap rr = BitmapFactory.decodeFile(file_route,
        //ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        //src.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), resized, ff.getName()+"_resized", null);
        String ttt23 = getPath(_context, Uri.parse(path));




        File ff2 = new File(ttt23);
        long sssi2 = ff2.length();

        return Uri.parse(path).toString();
    }

}


