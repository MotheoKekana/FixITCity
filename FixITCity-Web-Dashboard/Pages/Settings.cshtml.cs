
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

        public async Task<IActionResult> OnGetAsync()
        {
            var email = HttpContext.Session.GetString("Email") ?? "";

            if (string.IsNullOrWhiteSpace(email))
            {
                return RedirectToPage("/Login");
            }

            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var encodedEmail = Uri.EscapeDataString(email);

                var dto = await client.GetFromJsonAsync<UserSettingsDto>(
                    $"Users/me?email={encodedEmail}");

                if (dto == null)
                {
                    ErrorMessage = "Could not load your profile.";
                    return Page();
                }

                var themeMode = NormalizeTheme(dto.ThemeMode);

                Input = new SettingsInput
                {
                    FullName = dto.FullName ?? "",
                    Email = dto.Email ?? email,
                    ThemeMode = themeMode,
                    PreferredLanguage = dto.PreferredLanguage ?? "en"
                };

                // Use the preference returned by the API for this session.
                HttpContext.Session.SetString("ThemeMode", themeMode);

                return Page();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to load profile.");
                ErrorMessage = "Could not connect to the API.";
                return Page();
            }
        }

        public async Task<IActionResult> OnPostUpdateProfileAsync()
        {
            var currentEmail = HttpContext.Session.GetString("Email") ?? "";

            if (string.IsNullOrWhiteSpace(currentEmail))
            {
                return RedirectToPage("/Login");
            }

            if (!ModelState.IsValid)
            {
                return Page();
            }

            var client = _httpClientFactory.CreateClient("Api");
            var encodedEmail = Uri.EscapeDataString(currentEmail);

            // Notifications are intentionally omitted. The API must preserve
            // their existing database values when these fields are absent.
            var payload = new
            {
                fullName = Input.FullName,
                email = Input.Email,
                themeMode = Input.ThemeMode,
                preferredLanguage = Input.PreferredLanguage
            };

            try
            {
                var response = await client.PutAsJsonAsync(
                    $"Users/me?email={encodedEmail}",
                    payload);

                if (response.IsSuccessStatusCode)
                {
                    HttpContext.Session.SetString(
                        "FullName", Input.FullName ?? "");

                    HttpContext.Session.SetString(
                        "ThemeMode", Input.ThemeMode);

                    if (!string.IsNullOrWhiteSpace(Input.Email))
                    {
                        HttpContext.Session.SetString("Email", Input.Email);
                    }

                    TempData["SuccessMessage"] = "Settings saved successfully.";
                }
                else
                {
                    var apiError = await response.Content.ReadAsStringAsync();

                    _logger.LogError(
                        "Update settings failed. Status: {StatusCode}. Response: {ApiError}",
                        response.StatusCode,
                        apiError);

                    TempData["ErrorMessage"] =
                        "Your settings could not be saved. Please try again.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Update settings exception.");

                TempData["ErrorMessage"] =
                    "Could not connect to the API. Your settings were not saved.";
            }

            return RedirectToPage();
        }

        private static string NormalizeTheme(string? themeMode)
        {
            return themeMode?.Trim().ToLowerInvariant() switch
            {
                "dark" => "dark",
                "system" => "system",
                _ => "light"
            };
        }
    }

    public class UserSettingsDto
    {
        public int UserId { get; set; }
        public string? Email { get; set; }
        public string? FullName { get; set; }
        public string? ThemeMode { get; set; }
        public bool? EmailNotifications { get; set; }
        public bool? SmsNotifications { get; set; }
        public string? PreferredLanguage { get; set; }
    }

    public class SettingsInput
    {
        [Required]
        [Display(Name = "Full name")]
        public string? FullName { get; set; }

        [Required]
        [EmailAddress(ErrorMessage = "Please enter a valid email address.")]
        [Display(Name = "Email address")]
        public string? Email { get; set; }

        [Required]
        [RegularExpression(
            "^(light|dark|system)$",
            ErrorMessage = "Please select a valid appearance preference.")]
        [Display(Name = "Appearance")]
        public string ThemeMode { get; set; } = "light";

        [Required]
        [RegularExpression(
            "^(en|af|xh)$",
            ErrorMessage = "Please select a supported language.")]
        [Display(Name = "Language")]
        public string PreferredLanguage { get; set; } = "en";
    }
}
