using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class UsersController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public UsersController(FixITCityDbContext context)
    {
        _context = context;
    }

    // GET: api/Users/me?email=user@example.com
    [HttpGet("me")]
    public async Task<IActionResult> GetCurrentUser([FromQuery] string email)
    {
        if (string.IsNullOrWhiteSpace(email))
            return BadRequest(new { error = "Email is required." });

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == email);

        if (user == null)
            return NotFound(new { error = "User not found." });

        return Ok(new
        {
            userId = user.UserId,
            email = user.Email,
            fullName = user.FullName ?? ""
        });
    }

    // PUT: api/Users/me?email=user@example.com
    [HttpPut("me")]
    public async Task<IActionResult> UpdateCurrentUser(
        [FromQuery] string email,
        [FromBody] UpdateUserProfileRequest request)
    {
        if (string.IsNullOrWhiteSpace(email))
            return BadRequest(new { error = "Email is required." });

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == email);

        if (user == null)
            return NotFound(new { error = "User not found." });

        if (request.FullName != null)
            user.FullName = request.FullName.Trim();

        if (!string.IsNullOrWhiteSpace(request.Email))
            user.Email = request.Email.Trim();

        await _context.SaveChangesAsync();

        return Ok(new
        {
            success = true,
            fullName = user.FullName,
            email = user.Email,
            message = "Profile updated successfully."
        });
    }
}

public class UpdateUserProfileRequest
{
    public string? FullName { get; set; }
    public string? Email { get; set; }
}