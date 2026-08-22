package com.example.fixitcity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class FaultCategoryAdapter extends BaseAdapter {

    private Context context;
    private List<FaultCategory> categories;
    private LayoutInflater inflater;

    public FaultCategoryAdapter(Context context, List<FaultCategory> categories) {
        this.context = context;
        this.categories = categories;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return categories.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_category, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.fault_icon);
            holder.title = convertView.findViewById(R.id.fault_title);
            holder.description = convertView.findViewById(R.id.fault_description);
            holder.severity = convertView.findViewById(R.id.fault_severity);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FaultCategory category = categories.get(position);
        holder.icon.setImageResource(category.getIconResId());
        holder.title.setText(category.getTitle());
        holder.description.setText(category.getDescription());

        int severity = category.getSeverity();
        holder.severity.setText("⚠ Severity " + severity);

        // Color code severity badge
        int bgColor, textColor;
        if (severity >= 8) {
            bgColor = ContextCompat.getColor(context, R.color.severity_high_bg);
            textColor = ContextCompat.getColor(context, R.color.severity_high_text);
        } else if (severity >= 5) {
            bgColor = ContextCompat.getColor(context, R.color.severity_medium_bg);
            textColor = ContextCompat.getColor(context, R.color.severity_medium_text);
        } else {
            bgColor = ContextCompat.getColor(context, R.color.severity_low_bg);
            textColor = ContextCompat.getColor(context, R.color.severity_low_text);
        }
        holder.severity.setBackgroundColor(bgColor);
        holder.severity.setTextColor(textColor);

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;
        TextView severity;
    }
}