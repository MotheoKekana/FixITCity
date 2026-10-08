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

    // GET: api/reports
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
                    r.ReportId,
                    r.Title,
                    r.Description,
                    r.Status,
                    r.PriorityScore,
                    r.ReferenceNumber,
                    r.Latitude,
                    r.Longitude,
                    r.CreatedAt,
                    r.ResolvedAt,
                    CitizenName = r.Citizen != null ? r.Citizen.FirstName + " " + r.Citizen.LastName : null,
                    CitizenPhone = r.Citizen != null ? r.Citizen.PhoneNumber : null,
                    CategoryName = r.Category != null ? r.Category.Name : null,
                    AreaName = r.Area != null ? r.Area.AreaName : null
                })
                .ToListAsync();

            return Ok(reports);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    // GET: api/reports/{id}
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

            return Ok(report);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    // POST: api/reports
    // [HttpPost] this method is commented out because it was not part of the original request, but it can be uncommented and used to create new reports from the Web dashboard.
    // public async Task<IActionResult> CreateReport([FromBody] CreateReportRequest request)
    // {
    //     try
    //     {
    //         var random = new Random();
    //         var referenceNumber = "FIX-" + DateTime.Now.ToString("yyyyMMdd") + "-" + random.Next(1000, 9999);

    //         var report = new FaultReport
    //         {
    //             CitizenId = request.CitizenId,
    //             CategoryId = request.CategoryId,
    //             AreaId = request.AreaId,
    //             Title = request.Title,
    //             Description = request.Description,
    //             Latitude = request.Latitude,
    //             Longitude = request.Longitude,
    //             Status = "Pending",
    //             ReferenceNumber = referenceNumber,
    //             IsOffline = request.IsOffline,
    //             SyncStatus = request.IsOffline ? "Pending" : "Synced",
    //             CreatedAt = DateTime.Now
    //         };

    //         _context.FaultReports.Add(report);
    //         await _context.SaveChangesAsync();

    //         return Ok(new
    //         {
    //             message = "Report submitted successfully",
    //             report_id = report.ReportId,
    //             reference_number = referenceNumber
    //         });
    //     }
    //     catch (Exception ex)
    //     {
    //         return StatusCode(500, new { error = ex.Message });
    //     }
    // }
}

public class CreateReportRequest
{
    public int CitizenId { get; set; }
    public int CategoryId { get; set; }
    public int? AreaId { get; set; }
    public string Title { get; set; }
    public string Description { get; set; }
    public decimal Latitude { get; set; }
    public decimal Longitude { get; set; }
    public bool IsOffline { get; set; }
}