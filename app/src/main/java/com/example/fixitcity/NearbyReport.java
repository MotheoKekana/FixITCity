package com.example.fixitcity;

public class NearbyReport {
    private int id;
    private String title;
    private String category;
    private int voteCount;
    private double distance; // in meters

    public NearbyReport(int id, String title, String category, int voteCount, double distance) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.voteCount = voteCount;
        this.distance = distance;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public int getVoteCount() { return voteCount; }
    public double getDistance() { return distance; }

    public void incrementVote() {
        this.voteCount++;
    }
}