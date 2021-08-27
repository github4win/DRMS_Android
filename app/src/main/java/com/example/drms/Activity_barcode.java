package com.example.drms;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

// zxing import
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class Activity_barcode extends AppCompatActivity
{
    private static String TYPE;

    IntentIntegrator integrator = new IntentIntegrator(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        // getIntent 로 이전 화면에서 넘어온 데이터를 가져옴
        Intent intent = new Intent(this.getIntent());
        TYPE = intent.getStringExtra("Type");

        if (TYPE.equals("QR"))
            Toast.makeText(this, "QR 코드를 찍어주세요", Toast.LENGTH_SHORT).show();

        integrator.setCaptureActivity(barcode_scan.class);  // CaptureActivity -> barcode_scan.class
        integrator.setOrientationLocked(true);              // 세로모드
        integrator.setBeepEnabled(true);                    // 바코드 인식시 소리
        integrator.initiateScan();                          // 바코드 실행
    }

    /* 지원 바코드 종류
    1D product	                1D industrial	            2D
    ---------------------------------------------------------------------------------
    UPC-A	                    Code 39	                    QR Code
    UPC-E	                    Code 93	                    Data Matrix
    EAN-8	                    Code 128	                Aztec (beta)
    EAN-13	                    Codabar	                    PDF 417 (beta)
    UPC/EAN Extension 2/5	    ITF	                        MaxiCode
                                                            RSS-14
                                                            RSS-Expanded
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // QR코드, 바코드를 스캔한 결과
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // result.getFormatName() : 바코드 종류
        // result.getContents()   : 바코드 값

        // 뒤로가기 누를 시 바코드 창을 닫고 return
        if (result.getFormatName() == null)
        {
            finish();
            return;
        }

        // QR Code가 아닐 경우 return
        if (!result.getFormatName().equals("QR_CODE"))
        {
            Toast.makeText(this, "잘못된 코드 방식입니다.", Toast.LENGTH_SHORT).show();
            integrator.initiateScan();
            return;
        }

        //
        if (requestCode == 1)
        {
            integrator.initiateScan();
        }
        else
        {
            if (data != null)
            {
//                 Toast.makeText(this, "바코드 종류 : " + result.getFormatName() + "\n바코드 값 : " + result.getContents(), Toast.LENGTH_LONG).show();

                // 다시 원래 화면으로
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("Result", result.getContents());    // 바코드값 넘기는데 다른거도 가능
                setResult(RESULT_OK, intent);
            }
        }

        finish();
    }
}
