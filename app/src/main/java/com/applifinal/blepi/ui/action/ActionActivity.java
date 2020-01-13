package com.applifinal.blepi.ui.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.applifinal.blepi.R;
import com.applifinal.blepi.data.local.LocalPreferences;
import com.applifinal.blepi.data.models.ApiService;
import com.applifinal.blepi.data.models.LedStatus;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActionActivity extends AppCompatActivity {
    private  ApiService apiService;
    private final LedStatus ledStatus = new LedStatus();
    public static Intent getStartIntent(final Context ctx){
        return new Intent(ctx, ActionActivity.class);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action);
        ledStatus.setIdentifier(LocalPreferences.getInstance(this).getCurrentSelectedDevice());
        apiService = ApiService.Builder.getInstance();
    }



    void writeLedState( ){
        ledStatus.reverseStatus();
        apiService.writeStatus(ledStatus).enqueue(new Callback<LedStatus>() {
            @Override
            public void onResponse(Call<LedStatus> call, Response<LedStatus> response) {
                refreshLedState();
            }
            @Override
            public void onFailure(Call<LedStatus> call, Throwable t) {
                t.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ActionActivity.this, R.string.write_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    void refreshLedState() {
        apiService.readStatus(ledStatus.getIdentifier()).enqueue(new Callback<LedStatus>() {
            @Override
            public void onResponse(Call<LedStatus> call, Response<LedStatus> ledStatusResponse) {
                runOnUiThread(() -> {
                    boolean newStatus = ledStatus.getStatus();
                    if (ledStatusResponse.body() != null) {
                        newStatus = ledStatusResponse.body().getStatus();
                    }
                    ledStatus.setStatus(newStatus);
                });
            }

            @Override
            public void onFailure(Call<LedStatus> call, Throwable t) {
                t.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ActionActivity.this, R.string.server_connection_fault, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.state).setOnClickListener(v -> writeLedState());
    }
}
