package com.example.smishingsmskn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.168.152.167:5000/"; // Use 10.0.2.2 for Android Emulator
    private static final int SMS_PERMISSION_CODE = 1;

    private ApiService api;
    private SmsAdapter adapter;
    private List<SmsResult> smsResults = new ArrayList<>();
    private RecyclerView recyclerView;
    private Button checkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        }

        setContentView(R.layout.activity_main);

        checkButton = findViewById(R.id.checkButton);
        recyclerView = findViewById(R.id.smsRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SmsAdapter(smsResults);
        recyclerView.setAdapter(adapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ApiService.class);

        checkButton.setOnClickListener(v -> checkAllSms());
    }

    private void checkAllSms() {
        smsResults.clear();
        adapter.notifyDataSetChanged();

        List<String> messages = readAllSms();
        if (messages.isEmpty()) {
            Toast.makeText(this, "No messages found or permission denied", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String msg : messages) {
            SmsRequest request = new SmsRequest(msg);
            api.checkSpam(request).enqueue(new Callback<SpamResponse>() {
                @Override
                public void onResponse(Call<SpamResponse> call, Response<SpamResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        SpamResponse resp = response.body();
                        String result = resp.isSpam() ? "Spam: " + resp.getReason() : "Not Spam";
                        smsResults.add(new SmsResult(msg, result));
                    } else {
                        try {
                            Log.e("API_ERROR", response.errorBody().string());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        smsResults.add(new SmsResult(msg, "Error in response"));
                    }
                    adapter.notifyDataSetChanged();
                }



                @Override
                public void onFailure(Call<SpamResponse> call, Throwable t) {
                    smsResults.add(new SmsResult(msg, "error"));
                    adapter.notifyDataSetChanged();
                    Log.e("RetrofitError", "Message check failed", t);
                }
            });
        }
    }

    private List<String> readAllSms() {
        List<String> messages = new ArrayList<>();
        try {
            Uri uri = Uri.parse("content://sms/inbox");
            Cursor cursor = getContentResolver().query(uri, null, null, null, "date DESC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    messages.add(body);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
