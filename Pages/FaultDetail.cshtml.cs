using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading.Tasks;

namespace FixITCity_Dashboard.Pages
{
    public class FaultDetailModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;

        public FaultDetailModel(IHttpClientFactory httpClientFactory)
        {
            _httpClientFactory = httpClientFactory;
        }

        // Properties shown on the page
        public string PhotoUrl { get; set; } = "/images/placeholder.jpg";
        public string MediaContentType { get; set; } = "image"; // "image" or "video"
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public int PriorityScore { get; set; }
        public string FaultDescription { get; set; } = string.Empty;
        public string FaultCategory { get; set; } = string.Empty;
        public string FaultStatus { get; set; } = string.Empty;
        public DateTime ReportedAt { get; set; }
        // Human readable address resolved from latitude/longitude
        public string Address { get; set; } = string.Empty;
        public List<AuditLogEntry> AuditLogs { get; set; } = new();

        public async Task<IActionResult> OnGetAsync(int id)
        {
            var client = _httpClientFactory.CreateClient("Api");

            ReportDetailDto? dto = null;
            try
            {
                dto = await client.GetFromJsonAsync<ReportDetailDto>($"Reports/{id}");
            }
            catch
            {
                // If API fails, return NotFound so page shows 404
            }

            if (dto == null)
                return NotFound();

            FaultDescription = dto.Description ?? string.Empty;
            FaultCategory = dto.CategoryName ?? "Unknown";
            FaultStatus = dto.Status ?? "Pending";
            ReportedAt = dto.ReportedAt;
            Latitude = dto.Latitude;
            Longitude = dto.Longitude;
            PriorityScore = dto.PriorityScore ?? 0;

            var firstMedia = dto.ReportPhotos?.FirstOrDefault();
            if (firstMedia != null && !string.IsNullOrWhiteSpace(firstMedia.FirebaseUrl))
            {
                PhotoUrl = firstMedia.FirebaseUrl!;
                MediaContentType = (firstMedia.ContentType ?? "").StartsWith("video") ? "video" : "image";
            }

            AuditLogs = dto.AuditLogs?.OrderByDescending(a => a.Timestamp)
                .Select(a => new AuditLogEntry
                {
                    Action = a.Action ?? string.Empty,
                    AdminName = a.AdminName ?? "System",
                    Timestamp = a.Timestamp,
                    Reason = a.Reason ?? string.Empty
                }).ToList() ?? new List<AuditLogEntry>();

            // Try to resolve a human readable address from the latitude/longitude.
            // Uses OpenStreetMap Nominatim reverse geocoding. This is a best-effort call and
            // will not block the page if it fails.
            try
            {
                var addr = await ReverseGeocodeAsync(Latitude, Longitude);
                if (!string.IsNullOrWhiteSpace(addr))
                    Address = addr!;
            }
            catch
            {
                // ignore geocoding errors - show coordinates instead
            }

            return Page();
        }

        private async Task<string?> ReverseGeocodeAsync(double lat, double lon)
        {
            // Nominatim requires a valid User-Agent. Replace the contact email with a real one for production.
            var client = _httpClientFactory.CreateClient();
            client.DefaultRequestHeaders.UserAgent.ParseAdd("FixITCityDashboard/1.0 (contact@example.com)");

            var url = $"https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat={lat}&lon={lon}&addressdetails=1";

            var resp = await client.GetAsync(url);
            if (!resp.IsSuccessStatusCode)
                return null;

            using var stream = await resp.Content.ReadAsStreamAsync();
            using var doc = await JsonDocument.ParseAsync(stream);
            if (doc.RootElement.TryGetProperty("display_name", out var displayName))
                return displayName.GetString();

            return null;
        }

        // Verify
        public async Task<IActionResult> OnPostVerifyAsync(int id)
        {
            var client = _httpClientFactory.CreateClient("Api");

            var resp = await client.PutAsJsonAsync($"Reports/{id}", new { Status = "Verified" });
            // ignore response details for now
            return RedirectToPage(new { id });
        }

        // Reject with reason
        public async Task<IActionResult> OnPostRejectAsync(int id, string reason)
        {
            var client = _httpClientFactory.CreateClient("Api");

            var resp = await client.PutAsJsonAsync($"Reports/{id}", new { Status = "Rejected", Reason = reason });
            return RedirectToPage(new { id });
        }

        // Update status from select
        public async Task<IActionResult> OnPostUpdateStatusAsync(int id, string statusUpdate)
        {
            var client = _httpClientFactory.CreateClient("Api");

            var resp = await client.PutAsJsonAsync($"Reports/{id}", new { Status = statusUpdate });
            return RedirectToPage(new { id });
        }
    }

    // Lightweight DTOs used only by the page to deserialize API responses
    public class ReportDetailDto
    {
        public int ReportId { get; set; }
        public string? Title { get; set; }
        public string? Description { get; set; }
        public string? Status { get; set; }
        public int? PriorityScore { get; set; }
        public string? ReferenceNumber { get; set; }
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public DateTime ReportedAt { get; set; }
        public string? CategoryName { get; set; }
        public List<ReportPhotoDto>? ReportPhotos { get; set; }
        public List<AuditLogDto>? AuditLogs { get; set; }
    }

    public class ReportPhotoDto
    {
        public string? FirebaseUrl { get; set; }
        public string? ContentType { get; set; }
    }

    public class AuditLogDto
    {
        public string? Action { get; set; }
        public string? AdminName { get; set; }
        public DateTime Timestamp { get; set; }
        public string? Reason { get; set; }
    }

    public class AuditLogEntry
    {
        public string Action { get; set; } = string.Empty;
        public string AdminName { get; set; } = string.Empty;
        public DateTime Timestamp { get; set; }
        public string Reason { get; set; } = string.Empty;
    }
}
