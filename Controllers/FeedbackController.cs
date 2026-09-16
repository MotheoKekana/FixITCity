using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class FeedbackController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public FeedbackController(FixITCityDbContext context)
    {
        _context = context;
    }

    [HttpPost]
    public async Task<IActionResult> SubmitFeedback([FromBody] FeedbackRequest request)
    {
        try
        {
            var report = await _context.FaultReports.FindAsync(request.ReportId);
            if (report == null)
                return NotFound(new { message = "Report not found" });

            if (report.Status != "Resolved")
                return BadRequest(new { message = "Can only submit feedback for resolved reports" });

            var existing = await _context.ReportFeedbacks
                .FirstOrDefaultAsync(f => f.ReportId == request.ReportId);

            if (existing != null)
                return BadRequest(new { message = "Feedback already submitted for this report" });

            var responseTime = report.ResolvedAt.HasValue && report.CreatedAt != default
                ? (int)(report.ResolvedAt.Value - report.CreatedAt).TotalDays
                : 0;

            var feedback = new ReportFeedback
            {
                ReportId = request.ReportId,
                CitizenId = request.CitizenId,
                Rating = request.Rating,
                Comment = request.Comment,
                ResponseTime = responseTime,
                IsAnonymous = request.IsAnonymous,
                CreatedAt = DateTime.Now
            };

            _context.ReportFeedbacks.Add(feedback);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Feedback submitted", feedback_id = feedback.FeedbackId });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("report/{reportId}")]
    public async Task<IActionResult> GetReportFeedback(int reportId)
    {
        try
        {
            var feedback = await _context.ReportFeedbacks
                .FirstOrDefaultAsync(f => f.ReportId == reportId);

            if (feedback == null)
                return NotFound(new { message = "No feedback for this report" });

            return Ok(feedback);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("all")]
    public async Task<IActionResult> GetAllFeedback()
    {
        try
        {
            var feedback = await _context.ReportFeedbacks
                .OrderByDescending(f => f.CreatedAt)
                .ToListAsync();

            return Ok(feedback);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}

public class FeedbackRequest
{
    public int ReportId { get; set; }
    public int CitizenId { get; set; }
    public int Rating { get; set; }
    public string Comment { get; set; }
    public bool IsAnonymous { get; set; }
}