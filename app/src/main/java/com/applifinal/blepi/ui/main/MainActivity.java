package com.applifinal.blepi.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.applifinal.blepi.R;
import com.applifinal.blepi.ui.scan.ScanActivity;



public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //ArrayList<String> deviceArrayList = new ArrayList<>();
        //ExempleAdapter exempleAdapter = new ExempleAdapter(this, deviceArrayList);
        //ListView rvLists = findViewById(R.id.list_item);
        // rvLists.setAdapter(exempleAdapter);
        //deviceArrayList.add("Test");
        //deviceArrayList.add("Test1");
        //deviceArrayList.add("Test2");

        findViewById(R.id.scanner).setOnClickListener(v -> startActivity(ScanActivity.getStartIntent(this)));



        // deviceArrayList.add("Test");
        // deviceArrayList.add("Test1");
        // deviceArrayList.add("Test2");

    }


}
