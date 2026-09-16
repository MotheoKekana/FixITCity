using Microsoft.AspNetCore.Mvc;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class HealthController : ControllerBase
{
    [HttpGet]
    public IActionResult Ping() =>
        Ok(new { status = "API is running", timestamp = DateTime.Now });

    [HttpGet("routes")]
    public IActionResult Routes() => Ok(new[]
    {
        "GET  /api/Health",
        "GET  /api/Test",
        "POST /api/Auth/login",
        "POST /api/Auth/register",
        "GET  /api/Reports",
        "GET  /api/Reports/{id}",
        "POST /api/Reports",
        "GET  /api/Admin/reports/pending",
        "PUT  /api/Admin/reports/{id}/verify",
        "GET  /api/Admin/dashboard/stats",
        // etc
    });
}