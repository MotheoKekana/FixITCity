using System.Security.Cryptography;
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
    private readonly IEmailService _emailService;
    private readonly ILogger<AuthController> _logger;

    private const int ResetCodeLifetimeMinutes = 5;
    private const int MaxResetAttempts = 5;

    public AuthController(
        FixITCityDbContext context,
        JwtTokenService jwtTokenService,
        IEmailService emailService,
        ILogger<AuthController> logger)
    {
        _context = context;
        _jwtTokenService = jwtTokenService;
        _emailService = emailService;
        _logger = logger;
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

        if (!user.PasswordHash.StartsWith("$2"))
        {
            user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password);
            await _context.SaveChangesAsync();
        }

        var role = "Citizen";
        var municipal = await _context.MunicipalUsers
            .FirstOrDefaultAsync(m => m.UserId == user.UserId);
        if (municipal != null) role = "Municipal";

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

    // POST: api/auth/forgot-password
    [HttpPost("forgot-password")]
    public async Task<IActionResult> ForgotPassword([FromBody] ForgotPasswordRequest request)
    {
        // Always return the same response to prevent account enumeration.
        var genericResponse = new
        {
            message = "If an account with that email exists, a reset code has been sent."
        };

        if (string.IsNullOrWhiteSpace(request.Email))
            return Ok(genericResponse);

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == request.Email);

        if (user == null)
        {
            _logger.LogInformation("Password reset requested for unknown email {Email}", request.Email);
            return Ok(genericResponse);
        }

        // Invalidate any outstanding un-consumed tokens for this user.
        var existing = await _context.PasswordResetTokens
            .Where(t => t.UserId == user.UserId && t.ConsumedAt == null)
            .ToListAsync();
        foreach (var t in existing)
            t.ConsumedAt = DateTime.UtcNow;

        // Generate a cryptographically secure 6-digit code.
        var code = RandomNumberGenerator.GetInt32(100000, 1000000).ToString();

        var token = new PasswordResetToken
        {
            UserId = user.UserId,
            CodeHash = HashCode(code),
            ExpiresAt = DateTime.UtcNow.AddMinutes(ResetCodeLifetimeMinutes),
            Attempts = 0,
            CreatedAt = DateTime.UtcNow
        };

        _context.PasswordResetTokens.Add(token);
        await _context.SaveChangesAsync();

        try
        {
            await _emailService.SendPasswordResetCodeAsync(user.Email, code);
        }
        catch (Exception ex)
        {
            // Don't leak the failure to the caller — same generic response.
            _logger.LogError(ex, "Failed to send reset email for user {UserId}", user.UserId);
        }

        return Ok(genericResponse);
    }

    // POST: api/auth/verify-reset-code
    [HttpPost("verify-reset-code")]
    public async Task<IActionResult> VerifyResetCode([FromBody] VerifyResetCodeRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrWhiteSpace(request.Code))
            return BadRequest(new { message = "Email and code are required." });

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == request.Email);

        if (user == null)
            return BadRequest(new { message = "Invalid or expired code." });

        var token = await _context.PasswordResetTokens
            .Where(t => t.UserId == user.UserId && t.ConsumedAt == null)
            .OrderByDescending(t => t.CreatedAt)
            .FirstOrDefaultAsync();

        if (token == null)
            return BadRequest(new { message = "Invalid or expired code." });

        if (token.ExpiresAt < DateTime.UtcNow)
            return BadRequest(new { message = "The code has expired. Please request a new one." });

        if (token.Attempts >= MaxResetAttempts)
            return BadRequest(new { message = "Too many attempts. Please request a new code." });

        var providedHash = HashCode(request.Code.Trim());
        if (providedHash != token.CodeHash)
        {
            token.Attempts++;
            await _context.SaveChangesAsync();
            return BadRequest(new { message = "Invalid or expired code." });
        }

        return Ok(new { message = "Code verified." });
    }

    // POST: api/auth/reset-password
    [HttpPost("reset-password")]
    public async Task<IActionResult> ResetPassword([FromBody] ResetPasswordRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Email)
            || string.IsNullOrWhiteSpace(request.Code)
            || string.IsNullOrWhiteSpace(request.NewPassword))
        {
            return BadRequest(new { message = "Email, code, and new password are required." });
        }

        if (request.NewPassword.Length < 8)
            return BadRequest(new { message = "Password must be at least 8 characters." });

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == request.Email);

        if (user == null)
            return BadRequest(new { message = "Invalid or expired code." });

        var token = await _context.PasswordResetTokens
            .Where(t => t.UserId == user.UserId && t.ConsumedAt == null)
            .OrderByDescending(t => t.CreatedAt)
            .FirstOrDefaultAsync();

        if (token == null)
            return BadRequest(new { message = "Invalid or expired code." });

        if (token.ExpiresAt < DateTime.UtcNow)
            return BadRequest(new { message = "The code has expired. Please request a new one." });

        if (token.Attempts >= MaxResetAttempts)
            return BadRequest(new { message = "Too many attempts. Please request a new code." });

        if (HashCode(request.Code.Trim()) != token.CodeHash)
        {
            token.Attempts++;
            await _context.SaveChangesAsync();
            return BadRequest(new { message = "Invalid or expired code." });
        }

        // Hash new password with the same algorithm login uses.
        user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.NewPassword);

        token.ConsumedAt = DateTime.UtcNow;

        // Invalidate any other outstanding tokens.
        var others = await _context.PasswordResetTokens
            .Where(t => t.UserId == user.UserId && t.ConsumedAt == null && t.Id != token.Id)
            .ToListAsync();
        foreach (var t in others)
            t.ConsumedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        _logger.LogInformation("Password reset successfully for user {UserId}", user.UserId);

        return Ok(new { message = "Password reset successfully." });
    }

    private static string HashCode(string code)
    {
        using var sha = SHA256.Create();
        var bytes = System.Text.Encoding.UTF8.GetBytes(code);
        var hash = sha.ComputeHash(bytes);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private static bool VerifyPassword(string provided, string stored)
    {
        if (stored.StartsWith("$2"))
        {
            try { return BCrypt.Net.BCrypt.Verify(provided, stored); }
            catch { return false; }
        }
        return provided == stored;
    }
}

public class LoginRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
}

public class RegisterRequest
{
    public string Email { get; set; } = "";
    public string Password { get; set; } = "";
    public string FirstName { get; set; } = "";
    public string LastName { get; set; } = "";
    public string PhoneNumber { get; set; } = "";
    public string Address { get; set; } = "";
    public string City { get; set; } = "";
    public decimal? Latitude { get; set; }
    public decimal? Longitude { get; set; }
}

public class ForgotPasswordRequest
{
    public string Email { get; set; } = "";
}

public class VerifyResetCodeRequest
{
    public string Email { get; set; } = "";
    public string Code { get; set; } = "";
}

public class ResetPasswordRequest
{
    public string Email { get; set; } = "";
    public string Code { get; set; } = "";
    public string NewPassword { get; set; } = "";
}