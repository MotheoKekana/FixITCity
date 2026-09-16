using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class PhotosController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public PhotosController(FixITCityDbContext context)
    {
        _context = context;
    }

    [HttpGet("report/{reportId}")]
    public async Task<IActionResult> GetPhotos(int reportId)
    {
        try
        {
            var photos = await _context.ReportPhotos
                .Where(p => p.ReportId == reportId)
                .OrderByDescending(p => p.IsPrimary)
                .ToListAsync();

            return Ok(photos);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpPost("upload/{reportId}")]
    public async Task<IActionResult> UploadPhoto(int reportId, [FromBody] UploadPhotoRequest request)
    {
        try
        {
            var report = await _context.FaultReports.FindAsync(reportId);
            if (report == null)
                return NotFound(new { message = "Report not found" });

            var photo = new ReportPhoto
            {
                ReportId = reportId,
                PhotoUrl = request.PhotoUrl,
                IsPrimary = request.IsPrimary,
                IsOffline = request.IsOffline,
                SyncStatus = request.IsOffline ? "Pending" : "Synced",
                UploadedAt = DateTime.Now
            };

            _context.ReportPhotos.Add(photo);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Photo uploaded", photo_id = photo.PhotoId });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpDelete("{photoId}")]
    public async Task<IActionResult> DeletePhoto(int photoId)
    {
        try
        {
            var photo = await _context.ReportPhotos.FindAsync(photoId);
            if (photo == null)
                return NotFound(new { message = "Photo not found" });

            _context.ReportPhotos.Remove(photo);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Photo deleted" });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}

public class UploadPhotoRequest
{
    public string PhotoUrl { get; set; }
    public bool IsPrimary { get; set; }
    public bool IsOffline { get; set; }
}