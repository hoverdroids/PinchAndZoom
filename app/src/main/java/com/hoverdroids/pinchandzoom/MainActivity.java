package com.hoverdroids.pinchandzoom;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PinchZoomImageView img = (PinchZoomImageView) findViewById(R.id.imageview);
        img.setImageResource(R.drawable.ic_launcher_background);
        img.setMaxZoom(4f);
    }
}
