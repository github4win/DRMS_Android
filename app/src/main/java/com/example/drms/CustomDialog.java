package com.example.drms;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

public class CustomDialog extends ProgressDialog {

    private Context _context;
    private ImageView _imgLoading;

    public CustomDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(false);
        _context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        _imgLoading = (ImageView) findViewById(R.id.img_Loading);

        // 애니메이션 효과
        final AnimationDrawable frameAnimation = (AnimationDrawable) _imgLoading.getBackground();
        _imgLoading.post(new Runnable() {
            @Override
            public void run() {
                frameAnimation.start();
            }
        });
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
