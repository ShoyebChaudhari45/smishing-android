package com.example.smishingsmskn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.168.152.167:5000/"; // Replace with your IP
    private static final int SMS_PERMISSION_CODE = 1;

    private ApiService api;
    private SmsAdapter adapter;
    private List<SmsResult> smsResults = new ArrayList<>();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView overallResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        }

        setContentView(R.layout.activity_main);

        // UI setup
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.smsRecyclerView);
        overallResult = findViewById(R.id.overallResult);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SmsAdapter(smsResults);
        recyclerView.setAdapter(adapter);

        // Retrofit setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ApiService.class);

        // Refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::checkAllSms);

        // Initial load
        swipeRefreshLayout.setRefreshing(true);
        checkAllSms();
    }

    private void checkAllSms() {
        List<String> messages = readAllSms();
        if (messages.isEmpty()) {
            Toast.makeText(this, "No SMS found or permission denied", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        int total = messages.size();
        int[] completed = {0};
        int[] spamCount = {0};

        smsResults = new ArrayList<>(Collections.nCopies(total, null)); // Reserve slots

        for (int i = 0; i < total; i++) {
            final int index = i;
            String msg = messages.get(i);
            SmsRequest request = new SmsRequest(msg);

            api.checkSpam(request).enqueue(new Callback<SpamResponse>() {
                @Override
                public void onResponse(Call<SpamResponse> call, Response<SpamResponse> response) {
                    String result;
                    if (response.isSuccessful() && response.body() != null) {
                        SpamResponse resp = response.body();
                        result = resp.isSpam() ? "Spam: " + resp.getReason() : "Not Spam";
                        if (resp.isSpam()) spamCount[0]++;
                    } else {
                        result = "Error: empty response";
                    }

                    smsResults.set(index, new SmsResult(msg, result));
                    updateProgress(++completed[0], total, spamCount[0]);
                }

                @Override
                public void onFailure(Call<SpamResponse> call, Throwable t) {
                    smsResults.set(index, new SmsResult(msg, "Error"));
                    Log.e("SMS API", "Request failed", t);
                    updateProgress(++completed[0], total, spamCount[0]);
                }
            });
        }
    }

    private void updateProgress(int done, int total, int spamCount) {
        if (done == total) {
            adapter.setSmsList(smsResults); // set and refresh
            swipeRefreshLayout.setRefreshing(false);
            updateOverallResult(spamCount, total);
        }
    }

    private void updateOverallResult(int spamCount, int total) {
        overallResult.setText("Detected " + spamCount + " spam messages out of " + total);
    }

    private List<String> readAllSms() {
        List<String> messages = new ArrayList<>();
        try {
            // Query ALL SMS messages from all folders (inbox, sent, draft, etc.)
            Uri uri = Uri.parse("content://sms/");

            // Specify columns to improve performance
            String[] projection = {"body", "date", "type", "address"};

            // Optional: Filter to include only received and sent messages
            // Remove this filter if you want ALL messages including drafts, failed, etc.
            String selection = "type = ? OR type = ?";
            String[] selectionArgs = {"1", "2"}; // 1=inbox (received), 2=sent

            Cursor cursor = getContentResolver().query(
                    uri,
                    projection,
                    selection,           // Use null if you want ALL message types
                    selectionArgs,       // Use null if you want ALL message types
                    "date DESC"          // Latest first
            );

            if (cursor != null) {
                Log.d("SMS_READ", "Found " + cursor.getCount() + " SMS messages");

                while (cursor.moveToNext()) {
                    String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));

                    if (body != null && !body.trim().isEmpty()) {
                        messages.add(body);
                        Log.d("SMS_READ", "Message type: " + type + ", from: " + address + ", body: " + body.substring(0, Math.min(50, body.length())) + "...");
                    }
                }
                cursor.close();
            } else {
                Log.e("SMS_READ", "Cursor is null - permission might be denied");
            }

            Log.d("SMS_READ", "Total messages collected: " + messages.size());

        } catch (SecurityException e) {
            Log.e("SMS_READ", "Permission denied to read SMS", e);
            Toast.makeText(this, "SMS permission is required to read messages", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("SMS_READ", "Error reading SMS", e);
        }
        return messages;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                swipeRefreshLayout.setRefreshing(true);
                checkAllSms();
            } else {
                Toast.makeText(this, "SMS permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}