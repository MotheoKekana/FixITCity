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
        modelBuilder.Entity<AuditLog>(entity =>
        {
            entity.HasKey(e => e.LogId).HasName("PK__AuditLog__9E2397E0F56D5393");

            entity.Property(e => e.LogId).HasColumnName("log_id");
            entity.Property(e => e.Action)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("action");
            entity.Property(e => e.CreatedAt)
                .HasColumnType("datetime")
                .HasColumnName("created_at");
            entity.Property(e => e.MunicipalUserId).HasColumnName("municipal_user_id");
            entity.Property(e => e.RecordId).HasColumnName("record_id");
            entity.Property(e => e.TableAffected)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("table_affected");

            entity.HasOne(d => d.MunicipalUser).WithMany(p => p.AuditLogs)
                .HasForeignKey(d => d.MunicipalUserId)
                .HasConstraintName("FK__AuditLogs__munic__1EA48E88");
        });

        modelBuilder.Entity<CitizenProfile>(entity =>
        {
            entity.HasKey(e => e.CitizenProfileId).HasName("PK__CitizenP__040348AA182D2BFD");

            entity.Property(e => e.CitizenProfileId).HasColumnName("citizen_profile_id");
            entity.Property(e => e.AddressLine)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("address_line");
            entity.Property(e => e.City)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("city");
            entity.Property(e => e.FirstName)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("first_name");
            entity.Property(e => e.LastName)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("last_name");
            entity.Property(e => e.Latitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("latitude");
            entity.Property(e => e.Longitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("longitude");
            entity.Property(e => e.PhoneNumber)
                .HasMaxLength(20)
                .IsUnicode(false)
                .HasColumnName("phone_number");
            entity.Property(e => e.UserId).HasColumnName("user_id");

            entity.HasOne(d => d.User).WithMany(p => p.CitizenProfiles)
                .HasForeignKey(d => d.UserId)
                .HasConstraintName("FK__CitizenPr__user___778AC167");
        });

                modelBuilder.Entity<DeviceToken>(entity =>
        {
            entity.HasKey(e => e.TokenId).HasName("PK__DeviceTo__TokenId");

            entity.Property(e => e.TokenId).HasColumnName("token_id");
            entity.Property(e => e.UserId).HasColumnName("user_id");
            entity.Property(e => e.DeviceToken)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("device_token");
            entity.Property(e => e.DeviceName)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("device_name");
            entity.Property(e => e.IsActive)
                .HasColumnName("is_active")
                .HasDefaultValue(true);
            entity.Property(e => e.CreatedAt)
                .HasColumnType("datetime")
                .HasColumnName("created_at");
            entity.Property(e => e.LastUsedAt)
                .HasColumnType("datetime")
                .HasColumnName("last_used_at");

            entity.HasOne(d => d.User).WithMany(p => p.DeviceTokens)
                .HasForeignKey(d => d.UserId)
                .HasConstraintName("FK__DeviceTok__user");
        });

        modelBuilder.Entity<DuplicateReport>(entity =>
        {
            entity.HasKey(e => e.DuplicateLinkId).HasName("PK__Duplicat__C2B5B2C739CCD5D7");

            entity.Property(e => e.DuplicateLinkId).HasColumnName("duplicate_link_id");
            entity.Property(e => e.DuplicateReportId).HasColumnName("duplicate_report_id");
            entity.Property(e => e.MarkedAt)
                .HasColumnType("datetime")
                .HasColumnName("marked_at");
            entity.Property(e => e.MarkedBy).HasColumnName("marked_by");
            entity.Property(e => e.OriginalReportId).HasColumnName("original_report_id");

            entity.HasOne(d => d.DuplicateReportNavigation).WithMany(p => p.DuplicateReportDuplicateReportNavigations)
                .HasForeignKey(d => d.DuplicateReportId)
                .HasConstraintName("FK__Duplicate__dupli__0C85DE4D");

            entity.HasOne(d => d.MarkedByNavigation).WithMany(p => p.DuplicateReports)
                .HasForeignKey(d => d.MarkedBy)
                .HasConstraintName("FK__Duplicate__marke__0D7A0286");

            entity.HasOne(d => d.OriginalReport).WithMany(p => p.DuplicateReportOriginalReports)
                .HasForeignKey(d => d.OriginalReportId)
                .HasConstraintName("FK__Duplicate__origi__0B91BA14");
        });

        modelBuilder.Entity<EssentialService>(entity =>
        {
            entity.HasKey(e => e.ServiceId).HasName("PK__Essentia__3E0DB8AFE0D80852");

            entity.Property(e => e.ServiceId).HasColumnName("service_id");
            entity.Property(e => e.AddressLine)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("address_line");
            entity.Property(e => e.Latitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("latitude");
            entity.Property(e => e.Longitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("longitude");
            entity.Property(e => e.Name)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("name");
            entity.Property(e => e.ServiceType)
                .HasMaxLength(50)
                .IsUnicode(false)
                .HasColumnName("service_type");
        });

        modelBuilder.Entity<FaultCategory>(entity =>
        {
            entity.HasKey(e => e.CategoryId).HasName("PK__FaultCat__D54EE9B43850FFB9");

            entity.Property(e => e.CategoryId).HasColumnName("category_id");
            entity.Property(e => e.Description)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("description");
            entity.Property(e => e.Name)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("name");
            entity.Property(e => e.SeverityWeight)
                .HasColumnType("decimal(4, 2)")
                .HasColumnName("severity_weight");
        });

        modelBuilder.Entity<FaultReport>(entity =>
        {
            entity.HasKey(e => e.ReportId).HasName("PK__FaultRep__779B7C5829CEA724");

            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.AreaId).HasColumnName("area_id");
            entity.Property(e => e.AssignedTo).HasColumnName("assigned_to");
            entity.Property(e => e.CategoryId).HasColumnName("category_id");
            entity.Property(e => e.CitizenId).HasColumnName("citizen_id");
            entity.Property(e => e.CreatedAt)
                .HasColumnType("datetime")
                .HasColumnName("created_at");
            entity.Property(e => e.Description)
                .HasMaxLength(1000)
                .IsUnicode(false)
                .HasColumnName("description");
            entity.Property(e => e.Latitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("latitude");
            entity.Property(e => e.Longitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("longitude");
            entity.Property(e => e.ResolvedAt)
                .HasColumnType("datetime")
                .HasColumnName("resolved_at");
            entity.Property(e => e.Status)
                .HasMaxLength(30)
                .IsUnicode(false)
                .HasColumnName("status");
            entity.Property(e => e.Title)
                .HasMaxLength(200)
                .IsUnicode(false)
                .HasColumnName("title");

            entity.HasOne(d => d.Area).WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.AreaId)
                .HasConstraintName("FK__FaultRepo__area___04E4BC85");

            entity.HasOne(d => d.AssignedToNavigation).WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.AssignedTo)
                .HasConstraintName("FK__FaultRepo__assig__05D8E0BE");

            entity.HasOne(d => d.Category).WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.CategoryId)
                .HasConstraintName("FK__FaultRepo__categ__03F0984C");

            entity.HasOne(d => d.Citizen).WithMany(p => p.FaultReports)
                .HasForeignKey(d => d.CitizenId)
                .HasConstraintName("FK__FaultRepo__citiz__02FC7413");
        });

        modelBuilder.Entity<FaultStatusHistory>(entity =>
        {
            entity.HasKey(e => e.HistoryId).HasName("PK__FaultSta__096AA2E9CE52AD83");

            entity.ToTable("FaultStatusHistory");

            entity.Property(e => e.HistoryId).HasColumnName("history_id");
            entity.Property(e => e.ChangedAt)
                .HasColumnType("datetime")
                .HasColumnName("changed_at");
            entity.Property(e => e.ChangedBy).HasColumnName("changed_by");
            entity.Property(e => e.NewStatus)
                .HasMaxLength(30)
                .IsUnicode(false)
                .HasColumnName("new_status");
            entity.Property(e => e.Notes)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("notes");
            entity.Property(e => e.OldStatus)
                .HasMaxLength(30)
                .IsUnicode(false)
                .HasColumnName("old_status");
            entity.Property(e => e.ReportId).HasColumnName("report_id");

            entity.HasOne(d => d.ChangedByNavigation).WithMany(p => p.FaultStatusHistories)
                .HasForeignKey(d => d.ChangedBy)
                .HasConstraintName("FK__FaultStat__chang__14270015");

            entity.HasOne(d => d.Report).WithMany(p => p.FaultStatusHistories)
                .HasForeignKey(d => d.ReportId)
                .HasConstraintName("FK__FaultStat__repor__1332DBDC");
        });

        modelBuilder.Entity<MunicipalUser>(entity =>
        {
            entity.HasKey(e => e.MunicipalUserId).HasName("PK__Municipa__3BEE2B7F902A7C71");

            entity.Property(e => e.MunicipalUserId).HasColumnName("municipal_user_id");
            entity.Property(e => e.Department)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("department");
            entity.Property(e => e.PositionTitle)
                .HasMaxLength(100)
                .IsUnicode(false)
                .HasColumnName("position_title");
            entity.Property(e => e.UserId).HasColumnName("user_id");

            entity.HasOne(d => d.User).WithMany(p => p.MunicipalUsers)
                .HasForeignKey(d => d.UserId)
                .HasConstraintName("FK__Municipal__user___7A672E12");
        });

        modelBuilder.Entity<Notification>(entity =>
        {
            entity.HasKey(e => e.NotificationId).HasName("PK__Notifica__E059842F625D84E3");

            entity.Property(e => e.NotificationId).HasColumnName("notification_id");
            entity.Property(e => e.Channel)
                .HasMaxLength(20)
                .IsUnicode(false)
                .HasColumnName("channel");
            entity.Property(e => e.CitizenId).HasColumnName("citizen_id");
            entity.Property(e => e.Message)
                .HasMaxLength(500)
                .IsUnicode(false)
                .HasColumnName("message");
            entity.Property(e => e.ReadAt)
                .HasColumnType("datetime")
                .HasColumnName("read_at");
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.SentAt)
                .HasColumnType("datetime")
                .HasColumnName("sent_at");

            entity.HasOne(d => d.Citizen).WithMany(p => p.Notifications)
                .HasForeignKey(d => d.CitizenId)
                .HasConstraintName("FK__Notificat__citiz__1AD3FDA4");

            entity.HasOne(d => d.Report).WithMany(p => p.Notifications)
                .HasForeignKey(d => d.ReportId)
                .HasConstraintName("FK__Notificat__repor__1BC821DD");
        });

        modelBuilder.Entity<PriorityScore>(entity =>
        {
            entity.HasKey(e => e.ScoreId).HasName("PK__Priority__8CA19050D1946AFF");

            entity.Property(e => e.ScoreId).HasColumnName("score_id");
            entity.Property(e => e.AgeFactor)
                .HasColumnType("decimal(6, 2)")
                .HasColumnName("age_factor");
            entity.Property(e => e.ComputedAt)
                .HasColumnType("datetime")
                .HasColumnName("computed_at");
            entity.Property(e => e.DuplicateFactor)
                .HasColumnType("decimal(6, 2)")
                .HasColumnName("duplicate_factor");
            entity.Property(e => e.ProximityFactor)
                .HasColumnType("decimal(6, 2)")
                .HasColumnName("proximity_factor");
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.SeverityFactor)
                .HasColumnType("decimal(6, 2)")
                .HasColumnName("severity_factor");
            entity.Property(e => e.TotalScore)
                .HasColumnType("decimal(6, 2)")
                .HasColumnName("total_score");

            entity.HasOne(d => d.Report).WithMany(p => p.PriorityScores)
                .HasForeignKey(d => d.ReportId)
                .HasConstraintName("FK__PriorityS__repor__10566F31");
        });

        modelBuilder.Entity<ReportFeedback>(entity =>
        {
            entity.HasKey(e => e.FeedbackId).HasName("PK__ReportFe__7A6B2B8C0712876D");

            entity.ToTable("ReportFeedback");

            entity.Property(e => e.FeedbackId).HasColumnName("feedback_id");
            entity.Property(e => e.CitizenId).HasColumnName("citizen_id");
            entity.Property(e => e.Comment)
                .HasMaxLength(500)
                .IsUnicode(false)
                .HasColumnName("comment");
            entity.Property(e => e.CreatedAt)
                .HasColumnType("datetime")
                .HasColumnName("created_at");
            entity.Property(e => e.Rating).HasColumnName("rating");
            entity.Property(e => e.ReportId).HasColumnName("report_id");

            entity.HasOne(d => d.Citizen).WithMany(p => p.ReportFeedbacks)
                .HasForeignKey(d => d.CitizenId)
                .HasConstraintName("FK__ReportFee__citiz__17F790F9");

            entity.HasOne(d => d.Report).WithMany(p => p.ReportFeedbacks)
                .HasForeignKey(d => d.ReportId)
                .HasConstraintName("FK__ReportFee__repor__17036CC0");
        });

        modelBuilder.Entity<ReportPhoto>(entity =>
        {
            entity.HasKey(e => e.PhotoId).HasName("PK__ReportPh__CB48C83DCC8BC083");

            entity.Property(e => e.PhotoId).HasColumnName("photo_id");
            entity.Property(e => e.PhotoUrl)
                .HasMaxLength(500)
                .IsUnicode(false)
                .HasColumnName("photo_url");
            entity.Property(e => e.ReportId).HasColumnName("report_id");
            entity.Property(e => e.UploadedAt)
                .HasColumnType("datetime")
                .HasColumnName("uploaded_at");

            entity.HasOne(d => d.Report).WithMany(p => p.ReportPhotos)
                .HasForeignKey(d => d.ReportId)
                .HasConstraintName("FK__ReportPho__repor__08B54D69");
        });

        modelBuilder.Entity<ServiceArea>(entity =>
        {
            entity.HasKey(e => e.AreaId).HasName("PK__ServiceA__985D6D6B7C3D7493");

            entity.Property(e => e.AreaId).HasColumnName("area_id");
            entity.Property(e => e.AreaName)
                .HasMaxLength(50)
                .IsUnicode(false)
                .HasColumnName("area_name");
            entity.Property(e => e.Description)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("description");
            entity.Property(e => e.Latitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("latitude");
            entity.Property(e => e.Longitude)
                .HasColumnType("decimal(9, 6)")
                .HasColumnName("longitude");
            entity.Property(e => e.WardNumber)
                .HasMaxLength(10)
                .IsUnicode(false)
                .HasColumnName("ward_number");
        });

        modelBuilder.Entity<User>(entity =>
        {
            entity.HasKey(e => e.UserId).HasName("PK__Users__B9BE370FD87758BE");

            entity.Property(e => e.UserId).HasColumnName("user_id");
            entity.Property(e => e.CreatedAt)
                .HasColumnType("datetime")
                .HasColumnName("created_at");
            entity.Property(e => e.Email)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("email");
            entity.Property(e => e.PasswordHash)
                .HasMaxLength(255)
                .IsUnicode(false)
                .HasColumnName("password_hash");
            entity.Property(e => e.Role)
                .HasMaxLength(20)
                .IsUnicode(false)
                .HasColumnName("role");
        });

        OnModelCreatingPartial(modelBuilder);
    }

    partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
}
