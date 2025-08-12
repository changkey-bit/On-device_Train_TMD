package com.example.pilottest.intro;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilottest.MainActivity;
import com.example.pilottest.R;

public class IntroActivity extends AppCompatActivity {

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        imageView = findViewById(R.id.imageView);

        Handler handler = new Handler();

        Animation ani = AnimationUtils.loadAnimation(this, R.anim.appear_logo);
        imageView.startAnimation(ani);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
    @Override
    protected void onPause(){
        super.onPause();
        finish();
    }
}