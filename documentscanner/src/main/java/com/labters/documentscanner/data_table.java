package com.labters.documentscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
        blockconf= findViewById(R.id.blockconf);
        symconf = findViewById(R.id.symcon);
        reqid = findViewById(R.id.reqid);
        vertical = findViewById(R.id.vertical);
        vehicleid = findViewById(R.id.vehicleid);
        make = findViewById(R.id.make);
        model = findViewById(R.id.model);
        variant = findViewById(R.id.variant);
        fuel = findViewById(R.id.fuel);
        cc = findViewById(R.id.cc);
        previnsu = findViewById(R.id.previnsu);
        mfgyear = findViewById(R.id.mfg);
        regdate = findViewById(R.id.regdate);
        expdate = findViewById(R.id.expdate);


        done = findViewById(R.id.done);

        resStr = getIntent().getExtras().getString("response");
        nima_score = getIntent().getExtras().getString("nima");

        nima.setText(nima_score);
        fname.setText(resStr.substring(18,46));
        blockconf.setText(resStr.substring(69,83));
        symconf.setText(resStr.substring(107,124));
        reqid.setText(resStr.substring(147,158));
        vertical.setText(resStr.substring(178,180));
        vehicleid.setText(resStr.substring(199,205));
        make.setText(resStr.substring(219,223));
        model.setText(resStr.substring(240,249));
        variant.setText(resStr.substring(268,283));
        fuel.setText(resStr.substring(299,305));
        cc.setText(resStr.substring(317,322));
        previnsu.setText(resStr.substring(343,351));
        mfgyear.setText(resStr.substring(366,371));
        regdate.setText(resStr.substring(398,408));
        expdate.setText(resStr.substring(431,441));

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });


    }
}