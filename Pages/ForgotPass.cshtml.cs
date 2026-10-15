using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.ComponentModel.DataAnnotations;
using System.Net.Http.Json;

namespace FixITCity_Dashboard.Pages
{
    public class ForgotPassModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly ILogger<ForgotPassModel> _logger;

        public ForgotPassModel(
            IHttpClientFactory httpClientFactory,
            ILogger<ForgotPassModel> logger)
        {
            _httpClientFactory = httpClientFactory;
            _logger = logger;
        }

        [BindProperty]
        [Required(ErrorMessage = "Email address is required.")]
        [EmailAddress(ErrorMessage = "Please enter a valid email address.")]
        public string Email { get; set; } = "";

        [BindProperty]
        public string? VerificationCode { get; set; } = "";

        [BindProperty]
        public string? NewPassword { get; set; } = "";

        [BindProperty]
        public string? ConfirmPassword { get; set; } = "";

        public bool ShowCodeStep { get; set; }
        public bool ShowPasswordStep { get; set; }
        public bool ShowSuccessStep { get; set; }

        public void OnGet()
        {
        }

        public async Task<IActionResult> OnPostSendCodeAsync()
        {
            if (!ModelState.IsValid)
                return Page();

            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var response = await client.PostAsJsonAsync("Auth/forgot-password",
                    new { email = Email });

                if (response.IsSuccessStatusCode)
                {
                    TempData["InfoMessage"] =
                        "If an account with that email exists, a reset code has been sent.";
                }
                else
                {
                    TempData["ErrorMessage"] = "Something went wrong. Please try again.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Forgot-password request failed.");
                TempData["ErrorMessage"] = "We could not reach the service. Please try again.";
            }

            ShowCodeStep = true;
            return Page();
        }

        public async Task<IActionResult> OnPostVerifyCodeAsync()
        {
            ModelState.Remove(nameof(Email));

            if (string.IsNullOrWhiteSpace(VerificationCode))
            {
                ModelState.AddModelError(nameof(VerificationCode), "Please enter the verification code.");
                ShowCodeStep = true;
                return Page();
            }

            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var response = await client.PostAsJsonAsync("Auth/verify-reset-code", new
                {
                    email = Email,
                    code = VerificationCode.Trim()
                });

                if (!response.IsSuccessStatusCode)
                {
                    ModelState.AddModelError(nameof(VerificationCode),
                        "Invalid or expired code. Please try again.");
                    ShowCodeStep = true;
                    return Page();
                }

                ShowPasswordStep = true;
                return Page();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Verify-reset-code request failed.");
                ModelState.AddModelError(nameof(VerificationCode),
                    "We could not verify the code. Please try again.");
                ShowCodeStep = true;
                return Page();
            }
        }

        public async Task<IActionResult> OnPostResetPasswordAsync()
        {
            ModelState.Remove(nameof(Email));

            if (string.IsNullOrWhiteSpace(NewPassword))
                ModelState.AddModelError(nameof(NewPassword), "Please enter a new password.");
            else if (NewPassword.Length < 8)
                ModelState.AddModelError(nameof(NewPassword), "Password must contain at least 8 characters.");

            if (string.IsNullOrWhiteSpace(ConfirmPassword))
                ModelState.AddModelError(nameof(ConfirmPassword), "Please confirm your password.");
            else if (NewPassword != ConfirmPassword)
                ModelState.AddModelError(nameof(ConfirmPassword), "Passwords do not match.");

            if (!ModelState.IsValid)
            {
                ShowPasswordStep = true;
                return Page();
            }

            var client = _httpClientFactory.CreateClient("Api");

            try
            {
                var response = await client.PostAsJsonAsync("Auth/reset-password", new
                {
                    email = Email,
                    code = VerificationCode?.Trim(),
                    newPassword = NewPassword
                });

                if (!response.IsSuccessStatusCode)
                {
                    ModelState.AddModelError(string.Empty,
                        "The reset link has expired. Please start over.");
                    ShowPasswordStep = true;
                    return Page();
                }

                ShowSuccessStep = true;
                return Page();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Reset-password request failed.");
                ModelState.AddModelError(string.Empty,
                    "We could not reset your password. Please try again.");
                ShowPasswordStep = true;
                return Page();
            }
        }
    }
}