package com.example.pilottest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;

import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.pilottest.view.collectActivity;
import com.example.pilottest.view.learningActivity;

public class MainActivity extends AppCompatActivity {
    ImageButton collectButton, learningButton;
    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private static final int CAMERA_PERMISSION_REQUEST = 2;

    @Override
    protected void onStart() {
        super.onStart();
        requestLocationPermission();
        requestCameraPermission();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        collectButton = (ImageButton) findViewById(R.id.collectButton);
        learningButton = (ImageButton) findViewById(R.id.learningButton);

        // 버튼 관련 메소드
        collectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), collectActivity.class);
                startActivity(intent);
            }
        });

        learningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), learningActivity.class);
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        requestLocationPermission();
        requestCameraPermission();
    }
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 위치 권한이 허용됨
                } else {
                    // 위치 권한이 거부됨
                    Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case CAMERA_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 카메라 권한이 허용됨
                } else {
                    // 카메라 권한이 거부됨
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }
}