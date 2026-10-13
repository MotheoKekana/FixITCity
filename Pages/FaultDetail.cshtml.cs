using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Net.Http.Json;

namespace FixITCity_Dashboard.Pages
{
    public class FaultDetailModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly ILogger<FaultDetailModel> _logger;

        public FaultDetailModel(
            IHttpClientFactory httpClientFactory,
            ILogger<FaultDetailModel> logger)
        {
            _httpClientFactory = httpClientFactory;
            _logger = logger;
        }

        // ---- Properties shown on the page ----
        public string PhotoUrl { get; private set; } = "";
        public decimal? Latitude { get; private set; }
        public decimal? Longitude { get; private set; }
        public int? PriorityScore { get; private set; }
        public string? ErrorMessage { get; private set; }
        public List<AuditLogEntry> AuditLogs { get; private set; } = new();

        public bool HasCoordinates =>
            Latitude.HasValue &&
            Longitude.HasValue &&
            !(Latitude.Value == 0 && Longitude.Value == 0);

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
                Latitude = dto.Latitude;
                Longitude = dto.Longitude;
                PriorityScore = dto.PriorityScore;

                // Photo (placeholder for now)
                PhotoUrl = !string.IsNullOrWhiteSpace(dto.PhotoUrl)
                    ? dto.PhotoUrl!
                    : "/images/placeholder.jpg";

                // Audit logs
                AuditLogs = dto.AuditLogs
                    .OrderByDescending(l => l.Timestamp)
                    .Select(l => new AuditLogEntry
                    {
                        Action = l.Action ?? "",
                        AdminName = l.AdminName ?? "System",
                        Timestamp = l.Timestamp,
                        Reason = l.Reason ?? ""
                    })
                    .ToList();

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

        // ---------------------------------------------------------
        // Verify report
        // ---------------------------------------------------------
        public async Task<IActionResult> OnPostVerifyAsync(long id)
        {
            return await UpdateReportStatusAsync(id, "Accepted",
                "Report verified and moved to the ranked queue.");
        }

        // ---------------------------------------------------------
        // Reject report
        // ---------------------------------------------------------
        public async Task<IActionResult> OnPostRejectAsync(long id, string? reason)
        {
            if (string.IsNullOrWhiteSpace(reason))
            {
                TempData["ErrorMessage"] =
                    "Please enter a reason for rejecting the report.";
                return RedirectToPage(new { id });
            }

            return await UpdateReportStatusAsync(id, "Rejected", reason.Trim());
        }

        // ---------------------------------------------------------
        // Update status
        // ---------------------------------------------------------
        public async Task<IActionResult> OnPostUpdateStatusAsync(
            long id, string? statusUpdate)
        {
            if (string.IsNullOrWhiteSpace(statusUpdate))
            {
                TempData["ErrorMessage"] = "Please select a status.";
                return RedirectToPage(new { id });
            }

            return await UpdateReportStatusAsync(id, statusUpdate.Trim(),
                $"Status changed to {statusUpdate.Trim()}.");
        }

        // ---------------------------------------------------------
        // Send status update to API
        // ---------------------------------------------------------
        private async Task<IActionResult> UpdateReportStatusAsync(
            long id, string status, string reason)
        {
            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var request = new UpdateReportRequest
                {
                    Status = status,
                    Reason = reason
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

        public List<ReportPhotoDto> ReportPhotos { get; set; } = new();
        public List<AuditLogDto> AuditLogs { get; set; } = new();
    }

    public class ReportPhotoDto
    {
        public long PhotoId { get; set; }
        public string? PhotoUrl { get; set; }
        public string? SupabaseStoragePath { get; set; }
        public bool IsPrimary { get; set; }
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
        public string Action { get; set; } = "";
        public string AdminName { get; set; } = "";
        public DateTime Timestamp { get; set; }
        public string Reason { get; set; } = "";
    }
}