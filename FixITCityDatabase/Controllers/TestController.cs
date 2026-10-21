using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TestController : ControllerBase
{
    private readonly FixITCityAPI.Models.FixITCityDbContext _context;

    public TestController(FixITCityAPI.Models.FixITCityDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<IActionResult> TestConnection()
    {
        try
        {
            var userCount = await _context.Users.CountAsync();
            return Ok(new { 
                message = "Database connection successful!",
                users = userCount,
                timestamp = DateTime.Now
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { 
                message = "Database connection failed!",
                error = ex.Message
            });
        }
    }
}