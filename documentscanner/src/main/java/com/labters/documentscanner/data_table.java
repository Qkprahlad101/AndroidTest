package com.labters.documentscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class data_table extends AppCompatActivity {

    TextView nima, fname, blockconf, symconf, reqid, vertical, vehicleid, make, model, variant, fuel, cc, previnsu, mfgyear, regdate, expdate;
    String resStr;
    Button done;
    String nima_score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_table);

        nima = findViewById(R.id.nima);
        fname = findViewById(R.id.fname);
        done = findViewById(R.id.done);

        resStr = getIntent().getExtras().getString("response");
        nima_score = getIntent().getExtras().getString("nima");

        nima.setText(nima_score);
        fname.setText(resStr);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });


    }
}