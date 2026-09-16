using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class NotificationsController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public NotificationsController(FixITCityDbContext context)
    {
        _context = context;
    }

    [HttpPost("register")]
    public async Task<IActionResult> RegisterToken([FromBody] RegisterTokenRequest request)
    {
        try
        {
            var existing = await _context.DeviceTokens
                .FirstOrDefaultAsync(t => t.Token == request.Token);

            if (existing != null)
            {
                existing.IsActive = true;
                existing.LastUsedAt = DateTime.Now;
                await _context.SaveChangesAsync();
                return Ok(new { message = "Token updated" });
            }

            var token = new DeviceToken
            {
                UserId = request.UserId,
                Token = request.Token,
                DeviceName = request.DeviceName,
                IsActive = true,
                CreatedAt = DateTime.Now,
                LastUsedAt = DateTime.Now
            };

            _context.DeviceTokens.Add(token);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Token registered", token_id = token.TokenId });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("my-notifications/{citizenId}")]
    public async Task<IActionResult> GetMyNotifications(int citizenId)
    {
        try
        {
            var notifications = await _context.Notifications
                .Where(n => n.CitizenId == citizenId)
                .OrderByDescending(n => n.CreatedAt)
                .ToListAsync();

            return Ok(notifications);
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpPut("{notificationId}/read")]
    public async Task<IActionResult> MarkAsRead(int notificationId)
    {
        try
        {
            var notification = await _context.Notifications.FindAsync(notificationId);
            if (notification == null)
                return NotFound(new { message = "Notification not found" });

            notification.IsRead = true;
            notification.ReadAt = DateTime.Now;
            await _context.SaveChangesAsync();

            return Ok(new { message = "Marked as read" });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpPost("send")]
    public async Task<IActionResult> SendNotification([FromBody] SendNotificationRequest request)
    {
        try
        {
            var notification = new Notification
            {
                CitizenId = request.CitizenId,
                ReportId = request.ReportId,
                NotificationType = request.NotificationType,
                Channel = "Push",
                Subject = request.Subject,
                Message = request.Message,
                IsSent = true,
                SentAt = DateTime.Now,
                CreatedAt = DateTime.Now
            };

            _context.Notifications.Add(notification);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Notification sent", notification_id = notification.NotificationId });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { error = ex.Message });
        }
    }
}

public class RegisterTokenRequest
{
    public int UserId { get; set; }
    public string Token { get; set; }
    public string DeviceName { get; set; }
}

public class SendNotificationRequest
{
    public int CitizenId { get; set; }
    public int? ReportId { get; set; }
    public string NotificationType { get; set; }
    public string Subject { get; set; }
    public string Message { get; set; }
}