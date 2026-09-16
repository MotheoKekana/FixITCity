using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AdminController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public AdminController(FixITCityDbContext context)
    {
        _context = context;
    }

    // GET: api/admin/reports/pending
    [HttpGet("reports/pending")]
    public async Task<IActionResult> GetPendingReports()
    {
        try
        {
            var reports = await _context.FaultReports
                .Where(r => r.Status == "Pending" || r.Status == "Verified")
                .Include(r => r.Citizen)
                .Include(r => r.Category)
                .Include(r => r.Area)
                .OrderByDescending(r => r.PriorityScore)
                .ThenByDescending(r => r.CreatedAt)
                .Select(r => new
                {
                    r.ReportId,
                    r.Title,
                    r.Status,
                    r.PriorityScore,
                    r.ReferenceNumber,
                    r.CreatedAt,
                    CitizenName = r.Citizen != null ? r.Citizen.FirstName + " " + r.Citizen.LastName : null,
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

    // PUT: api/admin/reports/{id}/verify
    [HttpPut("reports/{id}/verify")]
    public async Task<IActionResult> VerifyReport(int id, [FromBody] VerifyRequest request)
    {
        try
        {
            var report = await _context.FaultReports.FindAsync(id);
            if (report == null)
                return NotFound(new { message = "Report not found" });

            var oldStatus = report.Status;
            report.Status = "Verified";
            report.VerifiedBy = request.AdminId;
            report.VerifiedAt = DateTime.Now;
            report.UpdatedAt = DateTime.Now;
            await _context.SaveChangesAsync();

            _context.FaultStatusHistories.Add(new FaultStatusHistory
            {
                ReportId = id,
                OldStatus = oldStatus,
                NewStatus = "Verified",
                ChangedBy = request.AdminId,
                ChangedAt = DateTime.Now,
                Notes = request.Notes
            });
            await _context.SaveChangesAsync();

            return Ok(new { message = "Report verified", report_id = id });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    // GET: api/admin/dashboard/stats
    [HttpGet("dashboard/stats")]
    public async Task<IActionResult> GetDashboardStats()
    {
        try
        {
            var total = await _context.FaultReports.CountAsync();
            var pending = await _context.FaultReports.CountAsync(r => r.Status == "Pending");
            var verified = await _context.FaultReports.CountAsync(r => r.Status == "Verified");
            var inProgress = await _context.FaultReports.CountAsync(r => r.Status == "InProgress");
            var resolved = await _context.FaultReports.CountAsync(r => r.Status == "Resolved");
            var rejected = await _context.FaultReports.CountAsync(r => r.Status == "Rejected");

            return Ok(new { total, pending, verified, inProgress, resolved, rejected });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}

public class VerifyRequest
{
    public int AdminId { get; set; }
    public string Notes { get; set; }
}