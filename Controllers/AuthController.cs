using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using FixITCityAPI.Models;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly FixITCityDbContext _context;

    public AuthController(FixITCityDbContext context)
    {
        _context = context;
    }

    // POST: api/auth/login
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        try
        {
            var user = await _context.Users
                .FirstOrDefaultAsync(u => u.Email == request.Email);

            if (user == null)
                return Unauthorized(new { message = "Invalid email or password" });

            if (user.PasswordHash != request.Password)
                return Unauthorized(new { message = "Invalid email or password" });

            return Ok(new
            {
                message = "Login successful",
                user_id = user.UserId,
                email = user.Email,
                role = user.Role
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { message = "Login failed", error = ex.Message });
        }
    }

    // POST: api/auth/register
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequest request)
    {
        try
        {
            if (await _context.Users.AnyAsync(u => u.Email == request.Email))
                return BadRequest(new { message = "Email already exists" });

            var user = new User
            {
                Email = request.Email,
                PasswordHash = request.Password,
                Role = "Citizen",
                IsActive = true,
                CreatedAt = DateTime.Now
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();

            var profile = new CitizenProfile
            {
                UserId = user.UserId,
                FirstName = request.FirstName,
                LastName = request.LastName,
                PhoneNumber = request.PhoneNumber,
                AddressLine = request.Address,
                City = request.City,
                Latitude = request.Latitude,
                Longitude = request.Longitude,
                CreatedAt = DateTime.Now
            };

            _context.CitizenProfiles.Add(profile);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                message = "Registration successful",
                user_id = user.UserId,
                email = user.Email
            });
        }
        catch (Exception ex)
        {
            return StatusCode(500, new { message = "Registration failed", error = ex.Message });
        }
    }
}

public class LoginRequest
{
    public string Email { get; set; }
    public string Password { get; set; }
}

public class RegisterRequest
{
    public string Email { get; set; }
    public string Password { get; set; }
    public string FirstName { get; set; }
    public string LastName { get; set; }
    public string PhoneNumber { get; set; }
    public string Address { get; set; }
    public string City { get; set; }
    public decimal? Latitude { get; set; }
    public decimal? Longitude { get; set; }
}