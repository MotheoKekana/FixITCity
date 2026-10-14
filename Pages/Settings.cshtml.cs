using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.ComponentModel.DataAnnotations;
using System.Net.Http.Json;

namespace FixITCity_Dashboard.Pages
{
    public class SettingsModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly ILogger<SettingsModel> _logger;

        public SettingsModel(
            IHttpClientFactory httpClientFactory,
            ILogger<SettingsModel> logger)
        {
            _httpClientFactory = httpClientFactory;
            _logger = logger;
        }

        [BindProperty]
        public SettingsInput Input { get; set; } = new();

        public string? ErrorMessage { get; private set; }

        // ---------------------------------------------------------
        // Load profile from the API
        // ---------------------------------------------------------
        public async Task<IActionResult> OnGetAsync()
        {
            var email = HttpContext.Session.GetString("Email") ?? "";
            if (string.IsNullOrWhiteSpace(email))
                return RedirectToPage("/Login");

            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var dto = await client
                    .GetFromJsonAsync<UserSettingsDto>($"Users/me?email={email}");

                if (dto == null)
                {
                    ErrorMessage = "Could not load your profile.";
                    return Page();
                }

                Input = new SettingsInput
                {
                    FullName = dto.FullName ?? "",
                    Email = dto.Email ?? email
                };

                return Page();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to load profile.");
                ErrorMessage = "Could not connect to the API.";
                return Page();
            }
        }

        // ---------------------------------------------------------
        // Save profile
        // ---------------------------------------------------------
        public async Task<IActionResult> OnPostUpdateProfileAsync()
        {
            if (!ModelState.IsValid)
                return Page();

            var currentEmail = HttpContext.Session.GetString("Email") ?? "";
            if (string.IsNullOrWhiteSpace(currentEmail))
                return RedirectToPage("/Login");

            var client = _httpClientFactory.CreateClient("Api");

            var payload = new
            {
                fullName = Input.FullName,
                email = Input.Email
            };

            try
            {
                var response = await client
                    .PutAsJsonAsync($"Users/me?email={currentEmail}", payload);

                if (response.IsSuccessStatusCode)
                {
                    HttpContext.Session.SetString("FullName", Input.FullName ?? "");

                    if (!string.IsNullOrWhiteSpace(Input.Email) &&
                        Input.Email != currentEmail)
                    {
                        HttpContext.Session.SetString("Email", Input.Email);
                    }

                    TempData["SuccessMessage"] = "Profile updated successfully!";
                }
                else
                {
                    var apiError = await response.Content.ReadAsStringAsync();
                    _logger.LogError("Update profile failed: {ApiError}", apiError);
                    TempData["ErrorMessage"] = $"Update failed: {apiError}";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Update profile exception.");
                TempData["ErrorMessage"] = "Could not connect to the API.";
            }

            return RedirectToPage();
        }
    }

    public class UserSettingsDto
    {
        public int UserId { get; set; }
        public string? Email { get; set; }
        public string? FullName { get; set; }
    }

    public class SettingsInput
    {
        [Display(Name = "Full Name")]
        public string? FullName { get; set; }

        [Display(Name = "Email Address")]
        [EmailAddress(ErrorMessage = "Please enter a valid email address")]
        public string? Email { get; set; }
    }
}