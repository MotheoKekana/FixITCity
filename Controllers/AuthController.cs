using FixITCityAPI.Models;
using FixITCityAPI.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FixITCityAPI.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly FixITCityDbContext _context;
    private readonly JwtTokenService _jwtTokenService;

    public AuthController(FixITCityDbContext context, JwtTokenService jwtTokenService)
    {
        _context = context;
        _jwtTokenService = jwtTokenService;
    }

    // POST: api/auth/login
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == request.Email);

        if (user == null || string.IsNullOrEmpty(user.PasswordHash)
            || !VerifyPassword(request.Password, user.PasswordHash))
        {
            return Unauthorized(new { message = "Invalid email or password" });
        }

        // Lazy migration: if the stored password isn't BCrypt yet, upgrade it now
        if (!user.PasswordHash.StartsWith("$2"))
        {
            user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password);
            await _context.SaveChangesAsync();
        }

        // Determine role — municipal users have a record in municipal_users
        var role = "Citizen";
        var municipal = await _context.MunicipalUsers
            .FirstOrDefaultAsync(m => m.UserId == user.UserId);
        if (municipal != null) role = "Municipal";

        // Generate the token
        var token = _jwtTokenService.GenerateToken(user.UserId, user.Email, role);

        return Ok(new
        {
            message = "Login successful",
            token,
            user_id = user.UserId,
            email = user.Email,
            role
        });
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
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
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

    private static bool VerifyPassword(string provided, string stored)
    {
        // New accounts: stored is a BCrypt hash starting with $2
        if (stored.StartsWith("$2"))
        {
            try { return BCrypt.Net.BCrypt.Verify(provided, stored); }
            catch { return false; }
        }

        // Legacy accounts: stored is plaintext — verify with plain comparison
        return provided == stored;
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