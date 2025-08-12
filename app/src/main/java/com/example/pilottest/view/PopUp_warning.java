package com.example.pilottest.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pilottest.R;

public class PopUp_warning extends AppCompatActivity {
    TextView txtText;
    Button yesBtn; Button noBtn;
    String mode, position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_popup_warning);

        yesBtn = findViewById(R.id.yesBtn);
        noBtn = findViewById(R.id.noBtn);

        txtText = findViewById(R.id.txtText);

        Intent intent = getIntent();
        mode = intent.getStringExtra("Mode");
        position = intent.getStringExtra("Position");

        txtText.setText("선택하신 모드와 포지션이 맞습니까?\n" + "모드 : " + mode + "\n핸드폰 위치 : " + position);

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, new Intent().putExtra("result", 1));
                finish();
            }
        });

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, new Intent().putExtra("result", 2));
                finish();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥 레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }
}
