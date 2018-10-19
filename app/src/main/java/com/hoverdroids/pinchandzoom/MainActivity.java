package com.hoverdroids.pinchandzoom;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PinchZoomImageView img = findViewById(R.id.imageview);
        img.setImageResource(R.drawable.hoverdroids);
        img.setMaxZoom(4f);
    }
}
