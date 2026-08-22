package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ReportFaultCategoryActivity extends AppCompatActivity {

    private ListView listView;
    private FaultCategoryAdapter adapter;
    private List<FaultCategory> categoryList = new ArrayList<>();
    private ReportData reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_category);

        // Get or create shared data
        reportData = ((FixITCityApp) getApplication()).getReportData();

        listView = findViewById(R.id.list_categories);
        adapter = new FaultCategoryAdapter(this, categoryList);
        listView.setAdapter(adapter);

        // Load categories from API (dummy for now)
        loadCategoriesFromApi();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            FaultCategory selected = categoryList.get(position);
            reportData.setCategory(selected.getTitle());
            reportData.setCategoryDescription(selected.getDescription());
            // Navigate to next step
            startActivity(ReportFaultGPSActivity.class);
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            // If nothing selected, show a message
            if (reportData.getCategory() == null) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(ReportFaultGPSActivity.class);
            }
        });
    }

    private void loadCategoriesFromApi() {
        // Simulate API call – replace with Retrofit/Volley
        categoryList.add(new FaultCategory(1, "Burst Water Pipe",
                "Major water leak affecting pressure, flooding streets, or...", 10,
                android.R.drawable.ic_dialog_alert));
        categoryList.add(new FaultCategory(2, "Power Outage",
                "Complete loss of electricity in multiple zones", 8,
                android.R.drawable.ic_menu_report_image));
        categoryList.add(new FaultCategory(3, "Blocked Drain",
                "Sewage backup causing foul smell and slow drainage", 4,
                android.R.drawable.ic_menu_compass));
        adapter.notifyDataSetChanged();
    }

    private void startActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }
}