using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class DashboardController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public DashboardController(FixITCityDbContext context)
    {
        _context = context;
    }

    [HttpGet("stats")]
    public async Task<IActionResult> GetStats()
    {
        try
        {
            var total = await _context.FaultReports.CountAsync();
            var pending = await _context.FaultReports.CountAsync(r => r.Status == "Pending");
            var underReview = await _context.FaultReports.CountAsync(r => r.Status == "Under Review");
            var accepted = await _context.FaultReports.CountAsync(r => r.Status == "Accepted");
            var inProgress = await _context.FaultReports.CountAsync(r => r.Status == "In Progress");
            var fixedCount = await _context.FaultReports.CountAsync(r => r.Status == "Fixed");
            var rejected = await _context.FaultReports.CountAsync(r => r.Status == "Rejected");

            return Ok(new
            {
                total,
                pending,
                underReview,
                accepted,
                inProgress,
                @fixed = fixedCount,
                rejected
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
    [HttpGet("reports-by-category")]
    public async Task<IActionResult> GetReportsByCategory()
    {
        try
        {
            var data = await _context.FaultReports
                .Include(r => r.Category)
                .GroupBy(r => r.Category.Name)
                .Select(g => new
                {
                    CategoryName = g.Key,
                    Count = g.Count()
                })
                .ToListAsync();

            return Ok(data);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
    [HttpGet("reports-by-area")]
    public async Task<IActionResult> GetReportsByArea()
    {
        try
        {
            var data = await _context.FaultReports
                .Include(r => r.Area)
                .Where(r => r.Area != null)
                .GroupBy(r => r.Area.AreaName)
                .Select(g => new
                {
                    AreaName = g.Key,
                    Count = g.Count()
                })
                .ToListAsync();

            return Ok(data);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("reports-by-status")]
    public async Task<IActionResult> GetReportsByStatus()
    {
        try
        {
            var data = await _context.FaultReports
                .GroupBy(r => r.Status)
                .Select(g => new
                {
                    Status = g.Key,
                    Count = g.Count()
                })
                .ToListAsync();

            return Ok(data);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("avg-priority-by-category")]
    public async Task<IActionResult> GetAvgPriorityByCategory()
    {
        try
        {
            var data = await _context.FaultReports
                .Include(r => r.Category)
                .Where(r => r.PriorityScore.HasValue)
                .GroupBy(r => r.Category.Name)
                .Select(g => new
                {
                    CategoryName = g.Key,
                    AveragePriority = Math.Round(g.Average(r => (double)r.PriorityScore.Value), 2)
                })
                .ToListAsync();

            return Ok(data);

        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}