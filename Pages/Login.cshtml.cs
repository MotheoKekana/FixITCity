using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.ComponentModel.DataAnnotations;
using System.Net.Http.Json;
using FixITCity_Dashboard.Services.Dashboard.Dtos;

namespace FixITCity_Dashboard.Pages
{
    public class LoginModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;

        public LoginModel(IHttpClientFactory httpClientFactory)
        {
            _httpClientFactory = httpClientFactory;
        }

        [BindProperty]
        public LoginInput Input { get; set; } = new();

        public void OnGet()
        {
        }

        public async Task<IActionResult> OnPostAsync()
        {
            if (!ModelState.IsValid)
            {
                return Page();
            }

            try
            {
                var client = _httpClientFactory.CreateClient("Api");

                var response = await client.PostAsJsonAsync("Auth/login", new
                {
                    email = Input.Email,
                    password = Input.Password
                });

                if (!response.IsSuccessStatusCode)
                {
                    TempData["ErrorMessage"] = "Invalid email or password.";
                    return Page();
                }

                var result = await response.Content.ReadFromJsonAsync<LoginResponseDto>();

                if (result == null)
                {
                    TempData["ErrorMessage"] = "Login failed — unexpected response from server.";
                    return Page();
                }

                HttpContext.Session.SetString("Username", result.Email);
                HttpContext.Session.SetString("Email", result.Email);
                HttpContext.Session.SetString("Role", result.Role);
                HttpContext.Session.SetInt32("UserId", result.UserId);
                HttpContext.Session.SetString("JwtToken", result.Token);

                return RedirectToPage("/Dashboard");
            }
            catch (Exception ex)
            {
                TempData["ErrorMessage"] = "Could not reach the login service: " + ex.Message;
                return Page();
            }
        }
    }

    public class LoginInput
    {
        [Required(ErrorMessage = "Email is required")]
        [EmailAddress(ErrorMessage = "Please enter a valid email")]
        [Display(Name = "Email")]
        public string Email { get; set; } = "";

        [Required(ErrorMessage = "Password is required")]
        [Display(Name = "Password")]
        public string Password { get; set; } = "";
    }
}