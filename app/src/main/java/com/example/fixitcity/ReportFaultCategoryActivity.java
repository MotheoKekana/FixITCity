package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ReportFaultCategoryActivity extends AppCompatActivity {

    private ListView listView;
    private FaultCategoryAdapter adapter;
    private List<FaultCategory> fullCategoryList = new ArrayList<>();
    private List<FaultCategory> filteredList = new ArrayList<>();
    private ReportData reportData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_fault_category);

        reportData = ((FixITCityApp) getApplication()).getReportData();

        listView = findViewById(R.id.list_categories);
        adapter = new FaultCategoryAdapter(this, filteredList);
        listView.setAdapter(adapter);

        loadCategories();

        // Search filter
        TextInputEditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            // FIX: use filteredList instead of categoryList
            FaultCategory selected = filteredList.get(position);
            if (selected.getId() == 99) {
                showOtherCategoryDialog();
            } else {
                reportData.setCategory(selected.getTitle());
                reportData.setCategoryDescription(selected.getDescription());
                // Go to GPS (Step 2)
                startActivity(new Intent(this, ReportFaultGPSActivity.class));
            }
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (reportData.getCategory() == null) {
                android.widget.Toast.makeText(this, "Please select a category", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, ReportFaultGPSActivity.class));
            }
        });

        // Bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v -> finish()); // back to dashboard
        findViewById(R.id.navReport).setOnClickListener(v -> {
            // Already in category – optionally restart the flow
            startActivity(new Intent(this, ReportFaultCategoryActivity.class));
            finish();
        });
        findViewById(R.id.navTrack).setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Track screen coming soon", android.widget.Toast.LENGTH_SHORT).show());
        findViewById(R.id.navSettings).setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Settings screen coming soon", android.widget.Toast.LENGTH_SHORT).show());
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    private void loadCategories() {
        fullCategoryList.clear();

        // Add all categories
        fullCategoryList.add(new FaultCategory(1, "Pothole",
                "Road surface damage, often caused by weather and traffic", 6,
                android.R.drawable.ic_menu_report_image));
        fullCategoryList.add(new FaultCategory(2, "Burst Water Pipe",
                "Major water leak affecting pressure, flooding streets", 10,
                android.R.drawable.ic_dialog_alert));
        fullCategoryList.add(new FaultCategory(3, "Blocked Drain",
                "Sewage backup causing foul smell and slow drainage", 4,
                android.R.drawable.ic_menu_compass));
        fullCategoryList.add(new FaultCategory(4, "Streetlight Outage",
                "Non-functioning streetlight creating safety hazards", 5,
                android.R.drawable.ic_menu_camera));
        fullCategoryList.add(new FaultCategory(5, "Litter / Illegal Dumping",
                "Unsanitary waste buildup requiring removal", 3,
                android.R.drawable.ic_menu_delete));
        fullCategoryList.add(new FaultCategory(6, "Overgrown Vegetation",
                "Uncontrolled growth obstructing sidewalks or visibility", 2,
                android.R.drawable.ic_menu_gallery));
        fullCategoryList.add(new FaultCategory(7, "Sidewalk Damage",
                "Cracked or uneven pavement posing tripping hazards", 4,
                android.R.drawable.ic_menu_mapmode));
        fullCategoryList.add(new FaultCategory(8, "Traffic Signal Malfunction",
                "Faulty traffic lights causing congestion or danger", 7,
                android.R.drawable.ic_menu_manage));
        fullCategoryList.add(new FaultCategory(9, "Fallen Tree / Branch",
                "Debris blocking roads or damaging property", 6,
                android.R.drawable.ic_menu_myplaces));
        fullCategoryList.add(new FaultCategory(10, "Animal Control Issue",
                "Stray animals or wildlife causing disturbance or danger", 3,
                android.R.drawable.ic_menu_compass));
        fullCategoryList.add(new FaultCategory(11, "Road Marking Damage",
                "Faded or missing road markings, stop lines, or crosswalks", 4,
                android.R.drawable.ic_menu_manage));
        fullCategoryList.add(new FaultCategory(12, "Broken Street Sign",
                "Damaged or missing street name signs or traffic signs", 3,
                android.R.drawable.ic_menu_compass));
        fullCategoryList.add(new FaultCategory(13, "Graffiti Removal",
                "Unwanted graffiti on public property", 2,
                android.R.drawable.ic_menu_camera));
        fullCategoryList.add(new FaultCategory(14, "Abandoned Vehicle",
                "Vehicle parked on public road for extended period", 4,
                android.R.drawable.ic_menu_report_image));
        // "Other" with question mark icon
        fullCategoryList.add(new FaultCategory(99, "Other",
                "If your issue is not listed, select this option to describe it", 0,
                android.R.drawable.ic_menu_help));

        // DEBUG: log the number of categories
        android.util.Log.d("CategoryActivity", "Total categories loaded: " + fullCategoryList.size());

        // Populate the filtered list
        filterCategories("");
    }

    private void filterCategories(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(fullCategoryList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (FaultCategory cat : fullCategoryList) {
                if (cat.getTitle().toLowerCase().contains(lowerQuery) ||
                        cat.getDescription().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(cat);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showOtherCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Describe the issue");
        final EditText input = new EditText(this);
        input.setHint("Enter fault description");
        builder.setView(input);
        builder.setPositiveButton("Continue", (dialog, which) -> {
            String description = input.getText().toString().trim();
            if (!description.isEmpty()) {
                reportData.setCategory("Other");
                reportData.setCategoryDescription(description);
                startActivity(new Intent(this, ReportFaultGPSActivity.class));
            } else {
                android.widget.Toast.makeText(this, "Please describe the issue", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}