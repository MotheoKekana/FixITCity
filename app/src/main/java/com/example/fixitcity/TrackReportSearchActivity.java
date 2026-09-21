package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class TrackReportSearchActivity extends AppCompatActivity {

    private static final String TAG = "TrackSearch";

    private TextInputEditText etReference;
    private MaterialButton btnTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_report_search);

        etReference = findViewById(R.id.et_reference);
        btnTrack = findViewById(R.id.btn_track);

        btnTrack.setOnClickListener(v -> attemptTrack());
    }

    private void attemptTrack() {
        String ref = etReference.getText() != null ? etReference.getText().toString().trim() : "";

        if (ref.isEmpty()) {
            Toast.makeText(this, "Please enter a reference number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTrack.setEnabled(false);

        new Thread(() -> {
            try {
                FaultReport report = SupabaseReports.getReportByReference(ref);
                runOnUiThread(() -> {
                    btnTrack.setEnabled(true);
                    if (report == null) {
                        Toast.makeText(this,
                                "No report found with that reference number",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Intent intent = new Intent(this, TrackReportResultActivity.class);
                        intent.putExtra("reference_number", report.getReferenceNumber());
                        intent.putExtra("title", report.getTitle());
                        intent.putExtra("description", report.getDescription());
                        intent.putExtra("address_line", report.getAddressLine());
                        intent.putExtra("status", report.getStatus());
                        intent.putExtra("priority_score",
                                report.getPriorityScore() != null ? report.getPriorityScore() : 0);
                        intent.putExtra("created_at", report.getCreatedAt());
                        startActivity(intent);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Track failed: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    btnTrack.setEnabled(true);
                    Toast.makeText(this, "Track failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}