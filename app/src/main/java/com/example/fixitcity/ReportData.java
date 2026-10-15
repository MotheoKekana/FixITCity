package com.example.fixitcity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportData {

    private String category;
    private long categoryId = -1;
    private String categoryDescription;
    private double categorySeverityWeight = 0.0;
    private String locationAddress;
    private double latitude;
    private double longitude;
    private int photoCount;
    private String additionalDetails;

    // Photo (local)
    private String photoUri;
    private byte[] photoBytes;
    private String photoMimeType;
    private final List<byte[]> photos = new ArrayList<>();

    // Upvote tracking
    private int upvotedReportId = -1;
    private boolean hasUpvoted = false;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    /** Alias — callers can use either getCategory() or getCategoryName(). */
    public String getCategoryName() { return category; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public String getCategoryDescription() { return categoryDescription; }
    public void setCategoryDescription(String categoryDescription) { this.categoryDescription = categoryDescription; }

    public double getCategorySeverityWeight() { return categorySeverityWeight; }
    public void setCategorySeverityWeight(double categorySeverityWeight) { this.categorySeverityWeight = categorySeverityWeight; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getPhotoCount() { return photos.size(); }
    public void setPhotoCount(int photoCount) { this.photoCount = photoCount; }

    public List<byte[]> getPhotos() { return Collections.unmodifiableList(photos); }
    public void addPhoto(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || photos.size() >= 3) {
            throw new IllegalArgumentException("A report can contain up to three photos");
        }
        photos.add(bytes);
        photoBytes = bytes;
        photoCount = photos.size();
    }
    public void removeLastPhoto() {
        if (!photos.isEmpty()) photos.remove(photos.size() - 1);
        photoBytes = photos.isEmpty() ? null : photos.get(photos.size() - 1);
        photoCount = photos.size();
    }
    public void clearPhotos() {
        photos.clear();
        photoBytes = null;
        photoUri = null;
        photoMimeType = null;
        photoCount = 0;
    }

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
