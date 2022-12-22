package com.example.supermap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.supermap.R;
import com.tbruyelle.rxpermissions3.RxPermissions;

public class SplashScreen extends AppCompatActivity {
    Context mContext;
    RxPermissions rxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash_screen);
        mContext = SplashScreen.this;
        rxPermissions = new RxPermissions(this);

        //set permission to read phone, access location, read & write external storage
        String permissionList[] = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        rxPermissions.request(permissionList)
                .subscribe(granted -> {
                    if (granted) {
                        // Always true pre-M
                        Thread timerThread = new Thread() {
                            public void run() {
                                try {
                                    sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } finally {
                                    // Do after time expires
                                    startActivity(new Intent(mContext, MapActivity.class));
                                    finish();
                                }
                            }
                        };
                        timerThread.start();

                    } else {
                        // Oups permission denied
                        Toast.makeText(mContext, "Oupss Permission is needed!", Toast.LENGTH_LONG).show();
                    }
                });


    }
}