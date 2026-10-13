package com.example.fixitcity;

public class NearbyReport {
    private long reportId;
    private String referenceNumber;
    private String title;
    private String category;
    private String status;
    private int priorityScore;
    private int voteCount;
    private double distance;            // in meters
    private long ownerCitizenId = -1;   // citizen_id of the report's creator

    public NearbyReport(long reportId,
                        String referenceNumber,
                        String title,
                        String category,
                        String status,
                        int priorityScore,
                        int voteCount,
                        double distance) {
        this.reportId = reportId;
        this.referenceNumber = referenceNumber;
        this.title = title;
        this.category = category;
        this.status = status;
        this.priorityScore = priorityScore;
        this.voteCount = voteCount;
        this.distance = distance;
    }

    public long getReportId() { return reportId; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public int getPriorityScore() { return priorityScore; }
    public int getVoteCount() { return voteCount; }
    public double getDistance() { return distance; }

    /** Setter — used by the optimistic upvote UI. */
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    /** Owner (creator) of the report — used to block self-upvote. */
    public long getOwnerCitizenId() { return ownerCitizenId; }
    public void setOwnerCitizenId(long id) { this.ownerCitizenId = id; }

    /** Kept for backward compatibility — adapter calls getId(). */
    public int getId() { return (int) reportId; }

    /** Kept for backward compatibility. */
    public void incrementVote() { this.voteCount++; }
}