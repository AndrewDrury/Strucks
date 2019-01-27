package com.example.strucks;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;
import android.os.Handler;

import com.example.strucks.FaceRecognition;


public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button button;
    long animationTime = 1000;
    Handler Delay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.Animatedimage);
        button = findViewById(R.id.Loginbtn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenMainMenu();
            }
        });

        }

    public void OpenMainMenu() {
        final Intent intent = new Intent (this, FaceRecognition.class);
        String eusername = "Steven";
        String epassword = "123456";

        EditText username = (EditText) findViewById(R.id.usernametxt);
        EditText password = (EditText) findViewById(R.id.passwordtxt);

        Log.i("myuserlogs", username.getText().toString());
        Log.i("myuserlogs", password.getText().toString());

        if (username.getText().toString().equals(eusername) && password.getText().toString().equals(epassword)) {
            Toast.makeText(getApplicationContext(), "Logged in", Toast.LENGTH_LONG).show();

            imageView.setVisibility(View.VISIBLE);
            ObjectAnimator animatorX = ObjectAnimator .ofFloat(imageView, "x", 1200f);
            animatorX.setDuration(animationTime);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animatorX);
            animatorSet.start();

            Handler Delay = new Handler();
            Delay.postDelayed(new Runnable() {
                @Override
                public void run() {

                    startActivity(intent);

                }
            },1000);
        }
        else{
            Toast.makeText(getApplicationContext(), "Invalid Loggin", Toast.LENGTH_LONG).show();
        }
        }
}

