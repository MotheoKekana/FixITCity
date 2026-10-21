using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace FixITCityAPI.Migrations
{
    /// <inheritdoc />
    public partial class AddGeocodingTrackingToFaultReports : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "essential_services",
                columns: table => new
                {
                    service_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    name = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    service_type = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    address_line = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    latitude = table.Column<decimal>(type: "numeric", nullable: true),
                    longitude = table.Column<decimal>(type: "numeric", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_essential_services", x => x.service_id);
                });

            migrationBuilder.CreateTable(
                name: "fault_categories",
                columns: table => new
                {
                    category_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    name = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    description = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    severity_weight = table.Column<decimal>(type: "numeric", nullable: true),
                    estimated_fix_time = table.Column<int>(type: "integer", nullable: true),
                    icon = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    is_active = table.Column<bool>(type: "boolean", nullable: true, defaultValue: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_fault_categories", x => x.category_id);
                });

            migrationBuilder.CreateTable(
                name: "fault_reports_with_votes",
                columns: table => new
                {
                    report_id = table.Column<int>(type: "integer", nullable: false),
                    citizen_id = table.Column<int>(type: "integer", nullable: false),
                    category_id = table.Column<int>(type: "integer", nullable: false),
                    area_id = table.Column<int>(type: "integer", nullable: true),
                    title = table.Column<string>(type: "text", nullable: false),
                    description = table.Column<string>(type: "text", nullable: false),
                    latitude = table.Column<decimal>(type: "numeric", nullable: false),
                    longitude = table.Column<decimal>(type: "numeric", nullable: false),
                    address_line = table.Column<string>(type: "text", nullable: true),
                    status = table.Column<string>(type: "text", nullable: false),
                    priority_score = table.Column<int>(type: "integer", nullable: true),
                    reference_number = table.Column<string>(type: "text", nullable: false),
                    verified_by = table.Column<long>(type: "bigint", nullable: true),
                    verified_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    assigned_to = table.Column<long>(type: "bigint", nullable: true),
                    resolution_notes = table.Column<string>(type: "text", nullable: true),
                    resolved_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    is_duplicate = table.Column<bool>(type: "boolean", nullable: true),
                    is_offline = table.Column<bool>(type: "boolean", nullable: true),
                    sync_status = table.Column<string>(type: "text", nullable: true),
                    offline_id = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    updated_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    vote_count = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                });

            migrationBuilder.CreateTable(
                name: "service_areas",
                columns: table => new
                {
                    area_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    area_name = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    ward_number = table.Column<string>(type: "character varying(10)", maxLength: 10, nullable: true),
                    description = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    latitude = table.Column<decimal>(type: "numeric", nullable: true),
                    longitude = table.Column<decimal>(type: "numeric", nullable: true),
                    is_active = table.Column<bool>(type: "boolean", nullable: true, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_service_areas", x => x.area_id);
                });

            migrationBuilder.CreateTable(
                name: "users",
                columns: table => new
                {
                    user_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    email = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    password_hash = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    role = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    is_active = table.Column<bool>(type: "boolean", nullable: true, defaultValue: true),
                    last_login = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    updated_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_users", x => x.user_id);
                });

            migrationBuilder.CreateTable(
                name: "citizen_profiles",
                columns: table => new
                {
                    citizen_profile_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    user_id = table.Column<int>(type: "integer", nullable: true),
                    first_name = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    last_name = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    phone_number = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    address_line = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    city = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    latitude = table.Column<decimal>(type: "numeric", nullable: true),
                    longitude = table.Column<decimal>(type: "numeric", nullable: true),
                    notification_preference = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    updated_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_citizen_profiles", x => x.citizen_profile_id);
                    table.ForeignKey(
                        name: "FK_citizen_profiles_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "user_id");
                });

            migrationBuilder.CreateTable(
                name: "device_tokens",
                columns: table => new
                {
                    token_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    user_id = table.Column<int>(type: "integer", nullable: false),
                    device_token = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: false),
                    device_name = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    is_active = table.Column<bool>(type: "boolean", nullable: true, defaultValue: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    last_used_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_device_tokens", x => x.token_id);
                    table.ForeignKey(
                        name: "FK_device_tokens_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "user_id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "municipal_users",
                columns: table => new
                {
                    municipal_user_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    user_id = table.Column<int>(type: "integer", nullable: true),
                    department = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    position_title = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_municipal_users", x => x.municipal_user_id);
                    table.ForeignKey(
                        name: "FK_municipal_users_users_user_id",
                        column: x => x.user_id,
                        principalTable: "users",
                        principalColumn: "user_id");
                });

            migrationBuilder.CreateTable(
                name: "audit_logs",
                columns: table => new
                {
                    log_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    municipal_user_id = table.Column<int>(type: "integer", nullable: true),
                    action = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    table_affected = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true),
                    record_id = table.Column<int>(type: "integer", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_audit_logs", x => x.log_id);
                    table.ForeignKey(
                        name: "FK_audit_logs_municipal_users_municipal_user_id",
                        column: x => x.municipal_user_id,
                        principalTable: "municipal_users",
                        principalColumn: "municipal_user_id");
                });

            migrationBuilder.CreateTable(
                name: "fault_reports",
                columns: table => new
                {
                    report_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    citizen_id = table.Column<int>(type: "integer", nullable: false),
                    category_id = table.Column<int>(type: "integer", nullable: false),
                    area_id = table.Column<int>(type: "integer", nullable: true),
                    title = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    description = table.Column<string>(type: "character varying(1000)", maxLength: 1000, nullable: false),
                    latitude = table.Column<decimal>(type: "numeric", nullable: false),
                    longitude = table.Column<decimal>(type: "numeric", nullable: false),
                    address_line = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true),
                    suburb_name = table.Column<string>(type: "text", nullable: true),
                    city_name = table.Column<string>(type: "text", nullable: true),
                    status = table.Column<string>(type: "character varying(30)", maxLength: 30, nullable: true),
                    priority_score = table.Column<int>(type: "integer", nullable: true),
                    reference_number = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    verified_by = table.Column<int>(type: "integer", nullable: true),
                    verified_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    assigned_to = table.Column<int>(type: "integer", nullable: true),
                    resolution_notes = table.Column<string>(type: "text", nullable: true),
                    resolved_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    is_duplicate = table.Column<bool>(type: "boolean", nullable: true),
                    is_offline = table.Column<bool>(type: "boolean", nullable: true),
                    sync_status = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: true),
                    offline_id = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    updated_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    geocoded_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    geocode_attempts = table.Column<int>(type: "integer", nullable: false),
                    VerifiedByNavigationMunicipalUserId = table.Column<int>(type: "integer", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_fault_reports", x => x.report_id);
                    table.ForeignKey(
                        name: "FK_fault_reports_citizen_profiles_citizen_id",
                        column: x => x.citizen_id,
                        principalTable: "citizen_profiles",
                        principalColumn: "citizen_profile_id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_fault_reports_fault_categories_category_id",
                        column: x => x.category_id,
                        principalTable: "fault_categories",
                        principalColumn: "category_id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_fault_reports_municipal_users_VerifiedByNavigationMunicipal~",
                        column: x => x.VerifiedByNavigationMunicipalUserId,
                        principalTable: "municipal_users",
                        principalColumn: "municipal_user_id");
                    table.ForeignKey(
                        name: "FK_fault_reports_municipal_users_assigned_to",
                        column: x => x.assigned_to,
                        principalTable: "municipal_users",
                        principalColumn: "municipal_user_id");
                    table.ForeignKey(
                        name: "FK_fault_reports_service_areas_area_id",
                        column: x => x.area_id,
                        principalTable: "service_areas",
                        principalColumn: "area_id");
                });

            migrationBuilder.CreateTable(
                name: "duplicate_reports",
                columns: table => new
                {
                    duplicate_link_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    original_report_id = table.Column<int>(type: "integer", nullable: true),
                    duplicate_report_id = table.Column<int>(type: "integer", nullable: true),
                    marked_by = table.Column<int>(type: "integer", nullable: true),
                    marked_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_duplicate_reports", x => x.duplicate_link_id);
                    table.ForeignKey(
                        name: "FK_duplicate_reports_fault_reports_duplicate_report_id",
                        column: x => x.duplicate_report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                    table.ForeignKey(
                        name: "FK_duplicate_reports_fault_reports_original_report_id",
                        column: x => x.original_report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                    table.ForeignKey(
                        name: "FK_duplicate_reports_municipal_users_marked_by",
                        column: x => x.marked_by,
                        principalTable: "municipal_users",
                        principalColumn: "municipal_user_id");
                });

            migrationBuilder.CreateTable(
                name: "fault_status_history",
                columns: table => new
                {
                    history_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    report_id = table.Column<int>(type: "integer", nullable: true),
                    old_status = table.Column<string>(type: "character varying(30)", maxLength: 30, nullable: true),
                    new_status = table.Column<string>(type: "character varying(30)", maxLength: 30, nullable: true),
                    changed_by = table.Column<int>(type: "integer", nullable: true),
                    changed_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    notes = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_fault_status_history", x => x.history_id);
                    table.ForeignKey(
                        name: "FK_fault_status_history_fault_reports_report_id",
                        column: x => x.report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                    table.ForeignKey(
                        name: "FK_fault_status_history_municipal_users_changed_by",
                        column: x => x.changed_by,
                        principalTable: "municipal_users",
                        principalColumn: "municipal_user_id");
                });

            migrationBuilder.CreateTable(
                name: "notifications",
                columns: table => new
                {
                    notification_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    citizen_id = table.Column<int>(type: "integer", nullable: false),
                    report_id = table.Column<int>(type: "integer", nullable: true),
                    notification_type = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false),
                    channel = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    subject = table.Column<string>(type: "character varying(200)", maxLength: 200, nullable: false),
                    message = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    device_token = table.Column<string>(type: "character varying(255)", maxLength: 255, nullable: false),
                    fcm_message_id = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    is_sent = table.Column<bool>(type: "boolean", nullable: true, defaultValue: false),
                    sent_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    is_read = table.Column<bool>(type: "boolean", nullable: true, defaultValue: false),
                    read_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    is_delivered = table.Column<bool>(type: "boolean", nullable: true, defaultValue: false),
                    delivered_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_notifications", x => x.notification_id);
                    table.ForeignKey(
                        name: "FK_notifications_citizen_profiles_citizen_id",
                        column: x => x.citizen_id,
                        principalTable: "citizen_profiles",
                        principalColumn: "citizen_profile_id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_notifications_fault_reports_report_id",
                        column: x => x.report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                });

            migrationBuilder.CreateTable(
                name: "priority_scores",
                columns: table => new
                {
                    score_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    report_id = table.Column<int>(type: "integer", nullable: true),
                    total_score = table.Column<decimal>(type: "numeric", nullable: true),
                    severity_factor = table.Column<decimal>(type: "numeric", nullable: true),
                    proximity_factor = table.Column<decimal>(type: "numeric", nullable: true),
                    duplicate_factor = table.Column<decimal>(type: "numeric", nullable: true),
                    age_factor = table.Column<decimal>(type: "numeric", nullable: true),
                    computed_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_priority_scores", x => x.score_id);
                    table.ForeignKey(
                        name: "FK_priority_scores_fault_reports_report_id",
                        column: x => x.report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                });

            migrationBuilder.CreateTable(
                name: "report_feedback",
                columns: table => new
                {
                    feedback_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    report_id = table.Column<int>(type: "integer", nullable: true),
                    citizen_id = table.Column<int>(type: "integer", nullable: true),
                    rating = table.Column<int>(type: "integer", nullable: true),
                    comment = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    response_time = table.Column<int>(type: "integer", nullable: true),
                    is_anonymous = table.Column<bool>(type: "boolean", nullable: true, defaultValue: false),
                    created_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    updated_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_report_feedback", x => x.feedback_id);
                    table.ForeignKey(
                        name: "FK_report_feedback_citizen_profiles_citizen_id",
                        column: x => x.citizen_id,
                        principalTable: "citizen_profiles",
                        principalColumn: "citizen_profile_id");
                    table.ForeignKey(
                        name: "FK_report_feedback_fault_reports_report_id",
                        column: x => x.report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                });

            migrationBuilder.CreateTable(
                name: "report_photos",
                columns: table => new
                {
                    photo_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    report_id = table.Column<int>(type: "integer", nullable: true),
                    photo_url = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: true),
                    supabase_storage_path = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    is_primary = table.Column<bool>(type: "boolean", nullable: true, defaultValue: false),
                    is_offline = table.Column<bool>(type: "boolean", nullable: true, defaultValue: false),
                    sync_status = table.Column<string>(type: "character varying(20)", maxLength: 20, nullable: false),
                    offline_photo_path = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    uploaded_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_report_photos", x => x.photo_id);
                    table.ForeignKey(
                        name: "FK_report_photos_fault_reports_report_id",
                        column: x => x.report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id");
                });

            migrationBuilder.CreateTable(
                name: "ReportVotes",
                columns: table => new
                {
                    vote_id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    report_id = table.Column<int>(type: "integer", nullable: false),
                    citizen_id = table.Column<int>(type: "integer", nullable: false),
                    voted_at = table.Column<DateTime>(type: "timestamp with time zone", nullable: false, defaultValueSql: "now()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("report_votes_pkey", x => x.vote_id);
                    table.ForeignKey(
                        name: "FK_ReportVotes_citizen_profiles_citizen_id",
                        column: x => x.citizen_id,
                        principalTable: "citizen_profiles",
                        principalColumn: "citizen_profile_id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_ReportVotes_fault_reports_report_id",
                        column: x => x.report_id,
                        principalTable: "fault_reports",
                        principalColumn: "report_id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_audit_logs_municipal_user_id",
                table: "audit_logs",
                column: "municipal_user_id");

            migrationBuilder.CreateIndex(
                name: "IX_citizen_profiles_user_id",
                table: "citizen_profiles",
                column: "user_id");

            migrationBuilder.CreateIndex(
                name: "IX_device_tokens_user_id",
                table: "device_tokens",
                column: "user_id");

            migrationBuilder.CreateIndex(
                name: "IX_duplicate_reports_duplicate_report_id",
                table: "duplicate_reports",
                column: "duplicate_report_id");

            migrationBuilder.CreateIndex(
                name: "IX_duplicate_reports_marked_by",
                table: "duplicate_reports",
                column: "marked_by");

            migrationBuilder.CreateIndex(
                name: "IX_duplicate_reports_original_report_id",
                table: "duplicate_reports",
                column: "original_report_id");

            migrationBuilder.CreateIndex(
                name: "IX_fault_reports_area_id",
                table: "fault_reports",
                column: "area_id");

            migrationBuilder.CreateIndex(
                name: "IX_fault_reports_assigned_to",
                table: "fault_reports",
                column: "assigned_to");

            migrationBuilder.CreateIndex(
                name: "IX_fault_reports_category_id",
                table: "fault_reports",
                column: "category_id");

            migrationBuilder.CreateIndex(
                name: "IX_fault_reports_citizen_id",
                table: "fault_reports",
                column: "citizen_id");

            migrationBuilder.CreateIndex(
                name: "IX_fault_reports_VerifiedByNavigationMunicipalUserId",
                table: "fault_reports",
                column: "VerifiedByNavigationMunicipalUserId");

            migrationBuilder.CreateIndex(
                name: "IX_fault_status_history_changed_by",
                table: "fault_status_history",
                column: "changed_by");

            migrationBuilder.CreateIndex(
                name: "IX_fault_status_history_report_id",
                table: "fault_status_history",
                column: "report_id");

            migrationBuilder.CreateIndex(
                name: "IX_municipal_users_user_id",
                table: "municipal_users",
                column: "user_id");

            migrationBuilder.CreateIndex(
                name: "IX_notifications_citizen_id",
                table: "notifications",
                column: "citizen_id");

            migrationBuilder.CreateIndex(
                name: "IX_notifications_report_id",
                table: "notifications",
                column: "report_id");

            migrationBuilder.CreateIndex(
                name: "IX_priority_scores_report_id",
                table: "priority_scores",
                column: "report_id");

            migrationBuilder.CreateIndex(
                name: "IX_report_feedback_citizen_id",
                table: "report_feedback",
                column: "citizen_id");

            migrationBuilder.CreateIndex(
                name: "IX_report_feedback_report_id",
                table: "report_feedback",
                column: "report_id");

            migrationBuilder.CreateIndex(
                name: "IX_report_photos_report_id",
                table: "report_photos",
                column: "report_id");

            migrationBuilder.CreateIndex(
                name: "IX_ReportVotes_citizen_id",
                table: "ReportVotes",
                column: "citizen_id");

            migrationBuilder.CreateIndex(
                name: "report_votes_unique_per_citizen",
                table: "ReportVotes",
                columns: new[] { "report_id", "citizen_id" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "audit_logs");

            migrationBuilder.DropTable(
                name: "device_tokens");

            migrationBuilder.DropTable(
                name: "duplicate_reports");

            migrationBuilder.DropTable(
                name: "essential_services");

            migrationBuilder.DropTable(
                name: "fault_reports_with_votes");

            migrationBuilder.DropTable(
                name: "fault_status_history");

            migrationBuilder.DropTable(
                name: "notifications");

            migrationBuilder.DropTable(
                name: "priority_scores");

            migrationBuilder.DropTable(
                name: "report_feedback");

            migrationBuilder.DropTable(
                name: "report_photos");

            migrationBuilder.DropTable(
                name: "ReportVotes");

            migrationBuilder.DropTable(
                name: "fault_reports");

            migrationBuilder.DropTable(
                name: "citizen_profiles");

            migrationBuilder.DropTable(
                name: "fault_categories");

            migrationBuilder.DropTable(
                name: "municipal_users");

            migrationBuilder.DropTable(
                name: "service_areas");

            migrationBuilder.DropTable(
                name: "users");
        }
    }
}
