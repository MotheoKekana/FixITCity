package com.example.fixitcity;

public class ReportData {
    private String category;
    private long categoryId = -1;
    private String categoryDescription;
    private double categorySeverityWeight = 0.0;   // NEW — from fault_categories.severity_weight
    private String locationAddress;
    private double latitude;
    private double longitude;
    private int photoCount;
    private String additionalDetails;

    // Photo (local)
    private String photoUri;        // local content:// path
    private byte[] photoBytes;      // raw bytes for upload
    private String photoMimeType;   // e.g. image/jpeg

    // Upvote tracking
    private int upvotedReportId = -1;
    private boolean hasUpvoted = false;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public String getCategoryDescription() { return categoryDescription; }
    public void setCategoryDescription(String categoryDescription) { this.categoryDescription = categoryDescription; }

    // NEW
    public double getCategorySeverityWeight() { return categorySeverityWeight; }
    public void setCategorySeverityWeight(double categorySeverityWeight) { this.categorySeverityWeight = categorySeverityWeight; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getPhotoCount() { return photoCount; }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public String getAdditionalDetails() { return additionalDetails; }
    public void setAdditionalDetails(String additionalDetails) { this.additionalDetails = additionalDetails; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }

    public byte[] getPhotoBytes() { return photoBytes; }
    public void setPhotoBytes(byte[] photoBytes) { this.photoBytes = photoBytes; }

    public String getPhotoMimeType() { return photoMimeType; }
    public void setPhotoMimeType(String photoMimeType) { this.photoMimeType = photoMimeType; }

    public int getUpvotedReportId() { return upvotedReportId; }
    public void setUpvotedReportId(int upvotedReportId) { this.upvotedReportId = upvotedReportId; }

    public boolean hasUpvoted() { return hasUpvoted; }
    public void setHasUpvoted(boolean hasUpvoted) { this.hasUpvoted = hasUpvoted; }
}