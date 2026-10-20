package com.example.fixitcity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fixitcity.data.SessionManager;
import com.example.fixitcity.data.SupabaseReports;
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

        ScreenInsets.apply(this);

        hideNavForGuests();
        AppNavigation.bindNotificationBell(this);

        reportData = ((FixITCityApp) getApplication()).getReportData();

        reportData.setCategory(null);
        reportData.setCategoryId(-1);
        reportData.setCategoryDescription(null);
        reportData.setCategorySeverityWeight(0.0);

        listView = findViewById(R.id.list_categories);
        adapter = new FaultCategoryAdapter(this, filteredList);
        listView.setAdapter(adapter);

        loadCategoriesFromSupabase();

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
            FaultCategory selected = filteredList.get(position);
            long realId = selected.getCategoryId();
            if (realId <= 0) {
                Toast.makeText(this,
                        "This category is missing an ID. Please pick another.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Persist the selection on ReportData so downstream screens can use it
            reportData.setCategory(selected.getName());                 // used by getCategoryName()
            reportData.setCategoryId(realId);
            reportData.setCategoryDescription(selected.getDescription());
            reportData.setCategorySeverityWeight(selected.getSeverityWeight());

            startActivity(new Intent(this, ReportFaultGPSActivity.class));
        });

        findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (reportData.getCategory() == null || reportData.getCategoryId() <= 0) {
                Toast.makeText(this, "Please select a category first", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, ReportFaultGPSActivity.class));
            }
        });

        // Bottom navigation
        findViewById(R.id.navHome).setOnClickListener(v -> AppNavigation.goHome(this));

        findViewById(R.id.navReport).setOnClickListener(v ->
                Toast.makeText(this, "You're already reporting", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navTrack).setOnClickListener(v ->
                startActivity(new Intent(this, TrackReportSearchActivity.class)));

        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileSettingsActivity.class)));
    }

    private void hideNavForGuests() {
        SessionManager session = new SessionManager(this);
        if (!session.isGuest()) return;

        View bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);
    }

    private void loadFallbackCategories() {
        fullCategoryList.clear();

        fullCategoryList.add(new FaultCategory(1, "Burst Pipe",
                "Major water leak affecting pressure, flooding streets", 10,
                android.R.drawable.ic_dialog_alert));
        fullCategoryList.add(new FaultCategory(2, "Pothole",
                "Road surface damage, often caused by weather and traffic", 6,
                android.R.drawable.ic_menu_report_image));
        fullCategoryList.add(new FaultCategory(3, "Streetlight Outage",
                "Non-functioning streetlight creating safety hazards", 5,
                android.R.drawable.ic_menu_camera));
        fullCategoryList.add(new FaultCategory(4, "Electrical Outage",
                "Loss of electrical power affecting homes or street infrastructure", 8,
                android.R.drawable.ic_menu_manage));
        fullCategoryList.add(new FaultCategory(5, "Sewage Leak",
                "Sewage overflow or leak creating health and safety hazards", 10,
                android.R.drawable.ic_dialog_alert));
        fullCategoryList.add(new FaultCategory(6, "Pavement Crack",
                "Cracked or uneven pavement posing tripping hazards", 4,
                android.R.drawable.ic_menu_mapmode));
        fullCategoryList.add(new FaultCategory(7, "Fallen Tree",
                "Tree or large branch blocking roads or damaging property", 6,
                android.R.drawable.ic_menu_myplaces));
        fullCategoryList.add(new FaultCategory(8, "Traffic Light Outage",
                "Faulty traffic lights causing congestion or danger", 7,
                android.R.drawable.ic_menu_manage));
        fullCategoryList.add(new FaultCategory(9, "Water Contamination",
                "Discoloured or contaminated water supply requiring investigation", 9,
                android.R.drawable.ic_dialog_alert));
        fullCategoryList.add(new FaultCategory(10, "Road Marker Damage",
                "Faded or missing road markings, stop lines, or crosswalks", 4,
                android.R.drawable.ic_menu_manage));
        fullCategoryList.add(new FaultCategory(11, "Illegal Dumping",
                "Unsanitary waste buildup or illegal dumping requiring removal", 3,
                android.R.drawable.ic_menu_delete));

        android.util.Log.d("CategoryActivity", "Fallback categories loaded: " + fullCategoryList.size());
        filterCategories("");
    }

    private void filterCategories(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(fullCategoryList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (FaultCategory cat : fullCategoryList) {
                if (cat.getName().toLowerCase().contains(lowerQuery) ||
                        cat.getDescription().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(cat);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadCategoriesFromSupabase() {
        SessionManager session = new SessionManager(this);
        String accessToken = session.getAccessToken();

        new Thread(() -> {
            try {
                List<FaultCategory> categories = SupabaseReports.getCategories(accessToken);
                android.util.Log.d("Categories",
                        "Loaded " + categories.size() + " categories from Supabase");

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    fullCategoryList.clear();
                    fullCategoryList.addAll(categories);

                    if (fullCategoryList.isEmpty()) {
                        android.util.Log.w("Categories", "Empty from Supabase, using fallback");
                        loadFallbackCategories();
                    } else {
                        filterCategories("");
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("Categories", "Failed to load: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.util.Log.w("Categories", "Falling back to hardcoded categories");
                    loadFallbackCategories();
                });
            }
        }).start();
    }
}
