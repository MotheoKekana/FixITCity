using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReportsController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public ReportsController(FixITCityDbContext context)
    {
        _context = context;
    }

    // -----------------------------------------------------
    // GET: api/reports
    // -----------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> GetAllReports()
    {
        try
        {
            var reports = await _context.FaultReports
                .Include(r => r.Citizen)
                .Include(r => r.Category)
                .Include(r => r.Area)
                .OrderByDescending(r => r.CreatedAt)
               .Select(r => new 
               {
                   ReportId = r.ReportId,
                   Title = r.Title,
                   CategoryName = r.Category.Name,
                   Status = r.Status,
                   PriorityScore = r.PriorityScore,
                   CreatedAt = r.CreatedAt,
                   ReferenceNumber = r.ReferenceNumber,

                   AreaName = r.Area != null ? r.Area.AreaName : null,
                   StreetName = r.StreetName,       
                   SuburbName = r.SuburbName,        
                   CityName = r.CityName,          
                   Latitude = r.Latitude,
                   Longitude = r.Longitude,
               })
                .ToListAsync();

            return Ok(reports);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    // -----------------------------------------------------
    // GET: api/reports/{id}
    // -----------------------------------------------------
    [HttpGet("{id}")]
    public async Task<IActionResult> GetReport(int id)
    {
        try
        {
            var report = await _context.FaultReports
                .Include(r => r.Citizen)
                .Include(r => r.Category)
                .Include(r => r.Area)
                .Include(r => r.ReportPhotos)
                .Include(r => r.FaultStatusHistories)
                .FirstOrDefaultAsync(r => r.ReportId == id);

            if (report == null)
                return NotFound(new { message = "Report not found" });

            return Ok(new
            {
                reportId = report.ReportId,
                referenceNumber = report.ReferenceNumber,
                title = report.Title,
                description = report.Description,
                status = report.Status,
                addressLine = report.AddressLine,
                categoryName = report.Category != null ? report.Category.Name : null,
                latitude = report.Latitude,
                longitude = report.Longitude,
                priorityScore = report.PriorityScore,
                reportedAt = report.CreatedAt,

                reportPhotos = report.ReportPhotos.Select(p => new
                {
                    photoId = p.PhotoId,
                    photoUrl = p.PhotoUrl,
                    supabaseStoragePath = p.SupabaseStoragePath,
                    isPrimary = p.IsPrimary,
                    uploadedAt = p.UploadedAt
                }),

                auditLogs = report.FaultStatusHistories.Select(h => new
                {
                    action = h.NewStatus,
                    adminName = "System",
                    timestamp = h.ChangedAt,
                    reason = h.Notes
                })
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    // -----------------------------------------------------
    // PUT: api/reports/{id}
    // -----------------------------------------------------
    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateReport(int id, [FromBody] UpdateReportRequest request)
    {
        if (request == null || string.IsNullOrWhiteSpace(request.Status))
            return BadRequest(new { error = "Status is required." });

        try
        {
            var report = await _context.FaultReports
                .FirstOrDefaultAsync(r => r.ReportId == id);

            if (report == null)
                return NotFound(new { error = $"Report {id} not found." });

            string oldStatus = report.Status ?? "Unknown";

            report.Status = request.Status;
            report.UpdatedAt = DateTime.UtcNow;

            var historyEntry = new FaultStatusHistory
            {
                ReportId = id,
                OldStatus = oldStatus,
                NewStatus = request.Status,
                ChangedAt = DateTime.UtcNow,
                Notes = request.Reason ?? $"Status changed from {oldStatus} to {request.Status}."
            };
            _context.FaultStatusHistories.Add(historyEntry);

            await _context.SaveChangesAsync();

            return Ok(new
            {
                success = true,
                reportId = id,
                oldStatus = oldStatus,
                newStatus = request.Status,
                message = $"Status updated to {request.Status}."
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}

// -----------------------------------------------------
// DTO for the PUT request body
// -----------------------------------------------------
public class UpdateReportRequest
{
    public string Status { get; set; } = string.Empty;
    public string? Reason { get; set; }
}