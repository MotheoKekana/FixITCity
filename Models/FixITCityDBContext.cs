using System;
using System.Collections.Generic;
using Microsoft.EntityFrameworkCore;

namespace FixITCityAPI.Models;

public partial class FixITCityDbContext : DbContext
{
    public FixITCityDbContext()
    {
    }

    public FixITCityDbContext(DbContextOptions<FixITCityDbContext> options)
        : base(options)
    {
    }

    public virtual DbSet<AuditLog> AuditLogs { get; set; }
    public virtual DbSet<CitizenProfile> CitizenProfiles { get; set; }
    public virtual DbSet<DeviceToken> DeviceTokens { get; set; }
    public virtual DbSet<DuplicateReport> DuplicateReports { get; set; }
    public virtual DbSet<EssentialService> EssentialServices { get; set; }
    public virtual DbSet<FaultCategory> FaultCategories { get; set; }
    public virtual DbSet<FaultReport> FaultReports { get; set; }
    public virtual DbSet<FaultStatusHistory> FaultStatusHistories { get; set; }
    public virtual DbSet<MunicipalUser> MunicipalUsers { get; set; }
    public virtual DbSet<Notification> Notifications { get; set; }
    public virtual DbSet<PriorityScore> PriorityScores { get; set; }
    public virtual DbSet<ReportFeedback> ReportFeedbacks { get; set; }
    public virtual DbSet<ReportPhoto> ReportPhotos { get; set; }
    public virtual DbSet<ServiceArea> ServiceAreas { get; set; }
    public virtual DbSet<User> Users { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        // ============================================================
        // MAP TABLES TO POSTGRESQL LOWERCASE NAMES
        // ============================================================
        modelBuilder.Entity<AuditLog>().ToTable("audit_logs");
        modelBuilder.Entity<CitizenProfile>().ToTable("citizen_profiles");
        modelBuilder.Entity<DeviceToken>().ToTable("device_tokens");
        modelBuilder.Entity<DuplicateReport>().ToTable("duplicate_reports");
        modelBuilder.Entity<EssentialService>().ToTable("essential_services");
        modelBuilder.Entity<FaultCategory>().ToTable("fault_categories");
        modelBuilder.Entity<FaultReport>().ToTable("fault_reports");
        modelBuilder.Entity<FaultStatusHistory>().ToTable("fault_status_history");
        modelBuilder.Entity<MunicipalUser>().ToTable("municipal_users");
        modelBuilder.Entity<Notification>().ToTable("notifications");
        modelBuilder.Entity<PriorityScore>().ToTable("priority_scores");
        modelBuilder.Entity<ReportFeedback>().ToTable("report_feedback");
        modelBuilder.Entity<ReportPhoto>().ToTable("report_photos");
        modelBuilder.Entity<ServiceArea>().ToTable("service_areas");
        modelBuilder.Entity<User>().ToTable("users");
        // ============================================================

        modelBuilder.Entity<AuditLog>(entity =>
        {
            entity.HasKey(e => e.LogId);
            entity.Property(e => e.LogId).HasColumnName("log_id");
            entity.Property(e => e.Action)
                .HasMaxLength(100)
                .HasColumnName("action");
            entity.Property(e => e.CreatedAt)
                .HasColumnName("created_at");
            entity.Property(e => e.MunicipalUserId).HasColumnName("municipal_user_id");
            entity.Property(e => e.RecordId).HasColumnName("record_id");
            entity.Property(e => e.TableAffected)
                .HasMaxLength(100)
                .HasColumnName("table_affected");

            entity.HasOne(d => d.MunicipalUser)
                .WithMany(p => p.AuditLogs)
                .HasForeignKey(d => d.MunicipalUserId);
        });

        modelBuilder.Entity<CitizenProfile>(entity =>
        {
            entity.HasKey(e => e.CitizenProfileId);
            entity.Property(e => e.CitizenProfileId).HasColumnName("citizen_profile_id");
            entity.Property(e => e.AddressLine)
                .HasMaxLength(255)
                .HasColumnName("address_line");
            entity.Property(e => e.City)
                .HasMaxLength(100)
                .HasColumnName("city");
            entity.Property(e => e.FirstName)
                .HasMaxLength(100)
                .HasColumnName("first_name");
            entity.Property(e => e.LastName)
                .HasMaxLength(100)
                .HasColumnName("last_name");
            entity.Property(e => e.Latitude)
                .HasColumnName("latitude");
            entity.Property(e => e.Longitude)
                .HasColumnName("longitude");
            entity.Property(e => e.PhoneNumber)
                .HasMaxLength(20)
                .HasColumnName("phone_number");
            entity.Property(e => e.UserId).HasColumnName("user_id");

            entity.HasOne(d => d.User)
                .WithMany(p => p.CitizenProfiles)
                .HasForeignKey(d => d.UserId);
            
            entity.Property(e => e.NotificationPreference).HasColumnName("notification_preference").HasMaxLength(20);
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
            entity.Property(e => e.UpdatedAt).HasColumnName("updated_at");
        });

        modelBuilder.Entity<DeviceToken>(entity =>
        {
            entity.HasKey(e => e.TokenId);
            entity.Property(e => e.TokenId).HasColumnName("token_id");
            entity.Property(e => e.UserId).HasColumnName("user_id");
            entity.Property(e => e.Token)
                .HasMaxLength(255)
                .HasColumnName("device_token");
            entity.Property(e => e.DeviceName)
                .HasMaxLength(100)
                .HasColumnName("device_name");
            entity.Property(e => e.IsActive)
                .HasColumnName("is_active")
                .HasDefaultValue(true);
            entity.Property(e => e.CreatedAt)
                .HasColumnName("created_at");
            entity.Property(e => e.LastUsedAt)
                .HasColumnName("last_used_at");

            entity.HasOne(d => d.User)
                .WithMany(p => p.DeviceTokens)
                .HasForeignKey(d => d.UserId);
        });

        modelBuilder.Entity<DuplicateReport>(entity =>
        {
            entity.HasKey(e => e.DuplicateLinkId);
            entity.Property(e => e.DuplicateLinkId).HasColumnName("duplicate_link_id");
            entity.Property(e => e.DuplicateReportId).HasColumnName("duplicate_report_id");
            entity.Property(e => e.MarkedAt).HasColumnName("marked_at");
            entity.Property(e => e.MarkedBy).HasColumnName("marked_by");
            entity.Property(e => e.OriginalReportId).HasColumnName("original_report_id");

            entity.HasOne(d => d.DuplicateReportNavigation)
                .WithMany(p => p.DuplicateReportDuplicateReportNavigations)
                .HasForeignKey(d => d.DuplicateReportId);

            entity.HasOne(d => d.MarkedByNavigation)
                .WithMany(p => p.DuplicateReports)
                .HasForeignKey(d => d.MarkedBy);

            entity.HasOne(d => d.OriginalReport)
                .WithMany(p => p.DuplicateReportOriginalReports)
                .HasForeignKey(d => d.OriginalReportId);
        });

        modelBuilder.Entity<EssentialService>(entity =>
        {
            entity.HasKey(e => e.ServiceId);
            entity.Property(e => e.ServiceId).HasColumnName("service_id");
            entity.Property(e => e.AddressLine)
                .HasMaxLength(255)
                .HasColumnName("address_line");
            entity.Property(e => e.Latitude).HasColumnName("latitude");
            entity.Property(e => e.Longitude).HasColumnName("longitude");
            entity.Property(e => e.Name)
                .HasMaxLength(255)
                .HasColumnName("name");
            entity.Property(e => e.ServiceType)
                .HasMaxLength(50)
                .HasColumnName("service_type");
        });

        modelBuilder.Entity<FaultCategory>(entity =>
        {
            entity.HasKey(e => e.CategoryId);
            entity.Property(e => e.CategoryId).HasColumnName("category_id");
            entity.Property(e => e.Description)
                .HasMaxLength(255)
                .HasColumnName("description");
            entity.Property(e => e.Name)
                .HasMaxLength(100)
                .HasColumnName("name");
            entity.Property(e => e.SeverityWeight).HasColumnName("severity_weight");
            entity.Property(e => e.EstimatedFixTime).HasColumnName("estimated_fix_time");
            entity.Property(e => e.Icon)
                .HasMaxLength(50)
                .HasColumnName("icon");
            entity.Property(e => e.IsActive).HasColumnName("is_active").HasDefaultValue(true);
        });

        modelBuilder.Entity<FaultReport>(entity =>
        {
            entity.HasKey(e => e.ReportId);
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.AreaId).HasColumnName("area_id");
            entity.Property(e => e.AssignedTo).HasColumnName("assigned_to");
            entity.Property(e => e.CategoryId).HasColumnName("category_id");
            entity.Property(e => e.CitizenId).HasColumnName("citizen_id");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
            entity.Property(e => e.Description)
                .HasMaxLength(1000)
                .HasColumnName("description");
            entity.Property(e => e.Latitude).HasColumnName("latitude");
            entity.Property(e => e.Longitude).HasColumnName("longitude");
            entity.Property(e => e.ResolvedAt).HasColumnName("resolved_at");
            entity.Property(e => e.Status)
                .HasMaxLength(30)
                .HasColumnName("status");
            entity.Property(e => e.Title)
                .HasMaxLength(200)
                .HasColumnName("title");
            entity.Property(e => e.PriorityScore).HasColumnName("priority_score");
            entity.Property(e => e.ReferenceNumber).HasColumnName("reference_number").HasMaxLength(50);
            entity.Property(e => e.AddressLine).HasColumnName("address_line").HasMaxLength(255);
            entity.Property(e => e.VerifiedBy).HasColumnName("verified_by");
            entity.Property(e => e.VerifiedAt).HasColumnName("verified_at");
            entity.Property(e => e.IsDuplicate).HasColumnName("is_duplicate");
            entity.Property(e => e.IsOffline).HasColumnName("is_offline");
            entity.Property(e => e.SyncStatus).HasColumnName("sync_status").HasMaxLength(20);
            entity.Property(e => e.OfflineId).HasColumnName("offline_id").HasMaxLength(50);
            entity.Property(e => e.UpdatedAt).HasColumnName("updated_at");
            entity.HasOne(d => d.Area)
                .WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.AreaId);

            entity.HasOne(d => d.AssignedToNavigation)
                .WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.AssignedTo);

            entity.HasOne(d => d.Category)
                .WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.CategoryId);

            entity.HasOne(d => d.Citizen)
                .WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.CitizenId);
        });

        modelBuilder.Entity<FaultStatusHistory>(entity =>
        {
            entity.HasKey(e => e.HistoryId);
            entity.ToTable("fault_status_history");
            entity.Property(e => e.HistoryId).HasColumnName("history_id");
            entity.Property(e => e.ChangedAt).HasColumnName("changed_at");
            entity.Property(e => e.ChangedBy).HasColumnName("changed_by");
            entity.Property(e => e.NewStatus)
                .HasMaxLength(30)
                .HasColumnName("new_status");
            entity.Property(e => e.Notes)
                .HasMaxLength(255)
                .HasColumnName("notes");
            entity.Property(e => e.OldStatus)
                .HasMaxLength(30)
                .HasColumnName("old_status");
            entity.Property(e => e.ReportId).HasColumnName("report_id");

            entity.HasOne(d => d.ChangedByNavigation)
                .WithMany(p => p.FaultStatusHistories)
                .HasForeignKey(d => d.ChangedBy);

            entity.HasOne(d => d.Report)
                .WithMany(p => p.FaultStatusHistories)
                .HasForeignKey(d => d.ReportId);
        });

        modelBuilder.Entity<MunicipalUser>(entity =>
        {
            entity.HasKey(e => e.MunicipalUserId);
            entity.Property(e => e.MunicipalUserId).HasColumnName("municipal_user_id");
            entity.Property(e => e.Department)
                .HasMaxLength(100)
                .HasColumnName("department");
            entity.Property(e => e.PositionTitle)
                .HasMaxLength(100)
                .HasColumnName("position_title");
            entity.Property(e => e.UserId).HasColumnName("user_id");

            entity.HasOne(d => d.User)
                .WithMany(p => p.MunicipalUsers)
                .HasForeignKey(d => d.UserId);
        });

        modelBuilder.Entity<Notification>(entity =>
        {
            entity.HasKey(e => e.NotificationId);
            entity.Property(e => e.NotificationId).HasColumnName("notification_id");
            entity.Property(e => e.Channel)
                .HasMaxLength(20)
                .HasColumnName("channel");
            entity.Property(e => e.CitizenId).HasColumnName("citizen_id");
            entity.Property(e => e.Message)
                .HasMaxLength(500)
                .HasColumnName("message");
            entity.Property(e => e.ReadAt).HasColumnName("read_at");
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.SentAt).HasColumnName("sent_at");

            entity.HasOne(d => d.Citizen)
                .WithMany(p => p.Notifications)
                .HasForeignKey(d => d.CitizenId);

            entity.HasOne(d => d.Report)
                .WithMany(p => p.Notifications)
                .HasForeignKey(d => d.ReportId);
            entity.Property(e => e.NotificationType).HasColumnName("notification_type").HasMaxLength(50);
            entity.Property(e => e.Subject).HasColumnName("subject").HasMaxLength(200);
            entity.Property(e => e.DeviceToken).HasColumnName("device_token").HasMaxLength(255);
            entity.Property(e => e.FcmMessageId).HasColumnName("fcm_message_id").HasMaxLength(100);
            entity.Property(e => e.IsSent).HasColumnName("is_sent").HasDefaultValue(false);
            entity.Property(e => e.IsRead).HasColumnName("is_read").HasDefaultValue(false);
            entity.Property(e => e.IsDelivered).HasColumnName("is_delivered").HasDefaultValue(false);
            entity.Property(e => e.DeliveredAt).HasColumnName("delivered_at");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
        });

        modelBuilder.Entity<PriorityScore>(entity =>
        {
            entity.HasKey(e => e.ScoreId);
            entity.Property(e => e.ScoreId).HasColumnName("score_id");
            entity.Property(e => e.AgeFactor).HasColumnName("age_factor");
            entity.Property(e => e.ComputedAt).HasColumnName("computed_at");
            entity.Property(e => e.DuplicateFactor).HasColumnName("duplicate_factor");
            entity.Property(e => e.ProximityFactor).HasColumnName("proximity_factor");
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.SeverityFactor).HasColumnName("severity_factor");
            entity.Property(e => e.TotalScore).HasColumnName("total_score");

            entity.HasOne(d => d.Report)
                .WithMany(p => p.PriorityScores)
                .HasForeignKey(d => d.ReportId);
        });

        modelBuilder.Entity<ReportFeedback>(entity =>
        {
            entity.HasKey(e => e.FeedbackId);
            entity.ToTable("report_feedback");
            entity.Property(e => e.FeedbackId).HasColumnName("feedback_id");
            entity.Property(e => e.CitizenId).HasColumnName("citizen_id");
            entity.Property(e => e.Comment)
                .HasMaxLength(500)
                .HasColumnName("comment");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
            entity.Property(e => e.Rating).HasColumnName("rating");
            entity.Property(e => e.ReportId).HasColumnName("report_id");

            entity.HasOne(d => d.Citizen)
                .WithMany(p => p.ReportFeedbacks)
                .HasForeignKey(d => d.CitizenId);

            entity.HasOne(d => d.Report)
                .WithMany(p => p.ReportFeedbacks)
                .HasForeignKey(d => d.ReportId);
            entity.Property(e => e.ResponseTime).HasColumnName("response_time");
            entity.Property(e => e.IsAnonymous).HasColumnName("is_anonymous").HasDefaultValue(false);
            entity.Property(e => e.UpdatedAt).HasColumnName("updated_at");
        });

        modelBuilder.Entity<ReportPhoto>(entity =>
        {
            entity.HasKey(e => e.PhotoId);
            entity.Property(e => e.PhotoId).HasColumnName("photo_id");
            entity.Property(e => e.PhotoUrl)
                .HasMaxLength(500)
                .HasColumnName("photo_url");
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.UploadedAt).HasColumnName("uploaded_at");

            entity.HasOne(d => d.Report)
                .WithMany(p => p.ReportPhotos)
                .HasForeignKey(d => d.ReportId);
            entity.Property(e => e.SupabaseStoragePath).HasColumnName("supabase_storage_path").HasMaxLength(500);
            entity.Property(e => e.IsPrimary).HasColumnName("is_primary").HasDefaultValue(false);
            entity.Property(e => e.IsOffline).HasColumnName("is_offline").HasDefaultValue(false);
            entity.Property(e => e.SyncStatus).HasColumnName("sync_status").HasMaxLength(20);
            entity.Property(e => e.OfflinePhotoPath).HasColumnName("offline_photo_path").HasMaxLength(500);
        });

        modelBuilder.Entity<ServiceArea>(entity =>
        {
            entity.HasKey(e => e.AreaId);
            entity.Property(e => e.AreaId).HasColumnName("area_id");
            entity.Property(e => e.AreaName)
                .HasMaxLength(50)
                .HasColumnName("area_name");
            entity.Property(e => e.Description)
                .HasMaxLength(255)
                .HasColumnName("description");
            entity.Property(e => e.Latitude).HasColumnName("latitude");
            entity.Property(e => e.Longitude).HasColumnName("longitude");
            entity.Property(e => e.WardNumber)
                .HasMaxLength(10)
                .HasColumnName("ward_number");
            entity.Property(e => e.IsActive).HasColumnName("is_active").HasDefaultValue(true);
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
        });

        modelBuilder.Entity<User>(entity =>
        {
            entity.HasKey(e => e.UserId);
            entity.Property(e => e.UserId).HasColumnName("user_id");
            entity.Property(e => e.CreatedAt).HasColumnName("created_at");
            entity.Property(e => e.Email)
                .HasMaxLength(255)
                .HasColumnName("email");
            entity.Property(e => e.PasswordHash)
                .HasMaxLength(255)
                .HasColumnName("password_hash");
            entity.Property(e => e.Role)
                .HasMaxLength(20)
                .HasColumnName("role");
            entity.Property(e => e.IsActive).HasColumnName("is_active").HasDefaultValue(true);
            entity.Property(e => e.LastLogin).HasColumnName("last_login");
            entity.Property(e => e.UpdatedAt).HasColumnName("updated_at");
        });

        OnModelCreatingPartial(modelBuilder);
    }

    partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
}