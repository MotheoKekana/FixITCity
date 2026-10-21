using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Globalization;
using System.Net.Http.Json;
using System.Text.RegularExpressions;

namespace FixITCity_Dashboard.Pages
{
    public class FaultDetailModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly ILogger<FaultDetailModel> _logger;

        // Two log rows for the same event this close together count as one.
        private const double DuplicateWindowSeconds = 5;

        private static readonly string[] KnownStatuses =
        {
            "Pending", "Under Review", "Accepted", "In Progress", "Fixed", "Rejected"
        };

        // Matches: Status changed to Accepted.  /  Status changed to "Under Review"
        private static readonly Regex StatusChangedRegex = new(
            "^\\s*Status changed to\\s+\"?(?<status>[^\"]+?)\"?\\s*\\.?\\s*$",
            RegexOptions.IgnoreCase | RegexOptions.Compiled);

        public FaultDetailModel(
            IHttpClientFactory httpClientFactory,
            ILogger<FaultDetailModel> logger)
        {
            _httpClientFactory = httpClientFactory;
            _logger = logger;
        }

        // ---- Properties shown on the page ----
        public string? FaultTitle { get; private set; }
        public string? FaultDescription { get; private set; }
        public List<string> PhotoUrls { get; private set; } = new();
        public bool HasPhoto => PhotoUrls.Count > 0;

        public string? AddressLine { get; private set; }
        public decimal? Latitude { get; private set; }
        public decimal? Longitude { get; private set; }
        public int? PriorityScore { get; private set; }
        public decimal? ProximityScore { get; private set; }
        public decimal? SeverityScore { get; private set; }
        public decimal? AgeScore { get; private set; }
        public decimal? DuplicateScore { get; private set; }
        public string? ErrorMessage { get; private set; }
        public List<AuditLogEntry> AuditLogs { get; private set; } = new();

        public bool HasCoordinates =>
            Latitude.HasValue &&
            Longitude.HasValue &&
            !(Latitude.Value == 0 && Longitude.Value == 0);

        // Invariant culture so a comma decimal separator never breaks the map
        public string? LatitudeText =>
            Latitude?.ToString(CultureInfo.InvariantCulture);

        public string? LongitudeText =>
            Longitude?.ToString(CultureInfo.InvariantCulture);

        public string? CoordinatesText => HasCoordinates
            ? $"{Latitude!.Value.ToString("0.000000", CultureInfo.InvariantCulture)}, " +
              $"{Longitude!.Value.ToString("0.000000", CultureInfo.InvariantCulture)}"
            : null;

        public string? GoogleMapsUrl => HasCoordinates
            ? $"https://www.google.com/maps/search/?api=1&query={LatitudeText},{LongitudeText}"
            : null;

        // ---------------------------------------------------------
        // Load fault report on GET
        // ---------------------------------------------------------
        public async Task<IActionResult> OnGetAsync(long id)
        {
            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                using var response = await client.GetAsync($"/api/Reports/{id}");

                if (response.StatusCode == System.Net.HttpStatusCode.NotFound)
                {
                    ErrorMessage = $"Report {id} was not found.";
                    return Page();
                }

                if (!response.IsSuccessStatusCode)
                {
                    var apiError = await response.Content.ReadAsStringAsync();
                    _logger.LogError(
                        "API returned {StatusCode} for report {ReportId}. Response: {ApiError}",
                        response.StatusCode, id, apiError);

                    ErrorMessage =
                        $"The API returned error {(int)response.StatusCode}: {response.ReasonPhrase}";
                    return Page();
                }

                var dto = await response.Content
                    .ReadFromJsonAsync<ReportDetailDto>();

                if (dto == null)
                {
                    ErrorMessage = "The API returned an empty report.";
                    return Page();
                }

                // ---- Real data from database ----
                FaultTitle = dto.Title;
                FaultDescription = dto.Description;
                Latitude = dto.Latitude;
                Longitude = dto.Longitude;
                PriorityScore = dto.PriorityScore;
                AddressLine = dto.AddressLine;

                if (dto.PriorityBreakdown != null)
                {
                    ProximityScore = dto.PriorityBreakdown.ProximityFactor;
                    SeverityScore = dto.PriorityBreakdown.SeverityFactor;
                    AgeScore = dto.PriorityBreakdown.AgeFactor;
                    DuplicateScore = dto.PriorityBreakdown.DuplicateFactor;
                }

                // All photos: primary first, then in the order they were uploaded
                PhotoUrls = (dto.ReportPhotos ?? new())
                    .Where(p => !string.IsNullOrWhiteSpace(p.PhotoUrl))
                    .OrderByDescending(p => p.IsPrimary ?? false)
                    .ThenBy(p => p.UploadedAt)
                    .ThenBy(p => p.PhotoId)
                    .Select(p => p.PhotoUrl!)
                    .Distinct()
                    .ToList();

                // Audit log: one readable line per event
                AuditLogs = BuildAuditLog(dto.AuditLogs);

                return Page();
            }
            catch (HttpRequestException ex)
            {
                _logger.LogError(ex,
                    "Could not connect to the API while loading report {ReportId}.", id);
                ErrorMessage =
                    "Could not connect to the API. Make sure the API is running.";
                return Page();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex,
                    "Unexpected error while loading report {ReportId}.", id);
                ErrorMessage =
                    "An unexpected error occurred while loading the report.";
                return Page();
            }
        }

        // Verify report
        // ---------------------------------------------------------
        public async Task<IActionResult> OnPostVerifyAsync(long id)
        {
            // Pass a default reason so the 3-argument method is satisfied
            return await UpdateReportStatusAsync(id, "Accepted", "Report verified and moved to the ranked queue.");
        }

        // ---------------------------------------------------------
        // Update status
        // ---------------------------------------------------------
        public async Task<IActionResult> OnPostUpdateStatusAsync(long id, string? statusUpdate, string? reason)
        {
            if (string.IsNullOrWhiteSpace(statusUpdate))
            {
                TempData["ErrorMessage"] = "Please select a status.";
                return RedirectToPage(new { id });
            }

            // VALIDATION: If they are rejecting, they MUST provide a reason.
            if (statusUpdate == "Rejected" && string.IsNullOrWhiteSpace(reason))
            {
                TempData["ErrorMessage"] = "You must provide a reason when rejecting a report.";
                return RedirectToPage(new { id });
            }

            // Default reason if they are changing to something else
            string finalReason = string.IsNullOrWhiteSpace(reason)
                ? $"Status changed to {statusUpdate.Trim()}."
                : reason.Trim();

            return await UpdateReportStatusAsync(id, statusUpdate.Trim(), finalReason);
        }
        // ---------------------------------------------------------
        // Send status update to API
        // ---------------------------------------------------------
        private async Task<IActionResult> UpdateReportStatusAsync(long id, string status, string reason)
        {
            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var request = new UpdateReportRequest
                {
                    Status = status,
                    Reason = reason // <--- Now uses the dynamic reason passed in
                };

                using var response = await client
                    .PutAsJsonAsync($"/api/Reports/{id}", request);

                if (response.IsSuccessStatusCode)
                {
                    TempData["SuccessMessage"] =
                        $"Report status changed to {status}.";
                }
                else
                {
                    var apiError = await response.Content.ReadAsStringAsync();
                    _logger.LogError(
                        "Status update failed for report {ReportId}. " +
                        "Status: {StatusCode}. Response: {ApiError}",
                        id, response.StatusCode, apiError);

                    TempData["ErrorMessage"] =
                        $"The report could not be updated. " +
                        $"API error: {(int)response.StatusCode}. {apiError}";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Could not update report {ReportId}.", id);
                TempData["ErrorMessage"] = "Could not connect to the API.";
            }

            return RedirectToPage(new { id });
        }
        // ---------------------------------------------------------
        // Audit log helpers
        // ---------------------------------------------------------
        private static List<AuditLogEntry> BuildAuditLog(IEnumerable<AuditLogDto>? logs)
        {
            var entries = new List<AuditLogEntry>();
            if (logs == null) return entries;

            foreach (var log in logs.OrderByDescending(l => l.Timestamp))
            {
                var summary = BuildSummary(log);
                var admin = string.IsNullOrWhiteSpace(log.AdminName)
                    ? "System"
                    : log.AdminName.Trim();

                // Same event recorded twice within a few seconds? Keep only one row.
                var duplicate = entries.FirstOrDefault(e =>
                    e.Summary == summary &&
                    Math.Abs((e.Timestamp - log.Timestamp).TotalSeconds) <= DuplicateWindowSeconds);

                if (duplicate == null)
                {
                    entries.Add(new AuditLogEntry
                    {
                        Summary = summary,
                        AdminName = admin,
                        Timestamp = log.Timestamp
                    });
                }
                else if (duplicate.AdminName == "System" && admin != "System")
                {
                    // Prefer the row that names the real admin
                    duplicate.AdminName = admin;
                }
            }

            return entries;
        }

        private static string BuildSummary(AuditLogDto log)
        {
            var action = log.Action?.Trim() ?? "";
            var reason = log.Reason?.Trim() ?? "";

            // 1. The reason already says "Status changed to X"
            var match = StatusChangedRegex.Match(reason);
            if (match.Success)
            {
                return $"Status changed to \"{match.Groups["status"].Value}\"";
            }

            // 2. The action itself is a status name (e.g. "Accepted")
            var knownStatus = KnownStatuses.FirstOrDefault(s =>
                s.Equals(action, StringComparison.OrdinalIgnoreCase));
            if (knownStatus != null)
            {
                return $"Status changed to \"{knownStatus}\"";
            }

            // 3. Anything else: show what the API recorded
            if (action.Length > 0 && reason.Length > 0) return $"{action}: {reason}";
            if (action.Length > 0) return action;
            if (reason.Length > 0) return reason;
            return "Activity recorded";
        }
    }

    // -------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------
    public class ReportDetailDto
    {
        public long ReportId { get; set; }
        public string? ReferenceNumber { get; set; }
        public string? Title { get; set; }
        public string? Description { get; set; }
        public string? Status { get; set; }
        public string? AddressLine { get; set; }
        public string? CategoryName { get; set; }
        public decimal? Latitude { get; set; }
        public decimal? Longitude { get; set; }
        public int? PriorityScore { get; set; }
        public DateTime? ReportedAt { get; set; }
        public string? PhotoUrl { get; set; }

        public PriorityBreakdownDto? PriorityBreakdown { get; set; }

        public List<ReportPhotoDto> ReportPhotos { get; set; } = new();
        public List<AuditLogDto> AuditLogs { get; set; } = new();
    }

    public class PriorityBreakdownDto
    {
        public decimal? ProximityFactor { get; set; }
        public decimal? SeverityFactor { get; set; }
        public decimal? AgeFactor { get; set; }
        public decimal? DuplicateFactor { get; set; }
    }
    public class ReportPhotoDto
    {
        public long PhotoId { get; set; }
        public string? PhotoUrl { get; set; }
        public string? SupabaseStoragePath { get; set; }
        public bool? IsPrimary { get; set; }
        public DateTime? UploadedAt { get; set; }
    }

    public class AuditLogDto
    {
        public string? Action { get; set; }
        public string? AdminName { get; set; }
        public DateTime Timestamp { get; set; }
        public string? Reason { get; set; }
    }

    public class UpdateReportRequest
    {
        public string Status { get; set; } = string.Empty;
        public string? Reason { get; set; }
    }

    public class AuditLogEntry
    {
        public string Summary { get; set; } = "";
        public string AdminName { get; set; } = "";
        public DateTime Timestamp { get; set; }
    }
}