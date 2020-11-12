package com.example.activity_assistant_logger;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import java.io.FileNotFoundException;

import com.example.activity_assistant_logger.actassistapi.Activity;


public class DisplayFileActivity extends AppCompatActivity{

    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_actfile);
        text = findViewById(R.id.textView);

        ActivityFileHandler actFile = new ActivityFileHandler(this.getApplicationContext());
        String str = "";
        try {
            str = actFile.getActivityFileAsString(this.getApplicationContext());
        } catch (FileNotFoundException e) {
        }
        text.setText(str);
    }

}
