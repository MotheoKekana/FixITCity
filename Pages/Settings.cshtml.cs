using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.ComponentModel.DataAnnotations;

namespace FixITCity_Dashboard.Pages
{
    public class SettingsModel : PageModel
    {
        [BindProperty]
        public SettingsInput Input { get; set; }

        public void OnGet()
        {
            Input = new SettingsInput
            {
                FullName = HttpContext.Session.GetString("FullName") ?? "",
                Email = HttpContext.Session.GetString("Email") ?? "",
                PhoneNumber = HttpContext.Session.GetString("PhoneNumber") ?? "",
                Theme = HttpContext.Session.GetString("Theme") ?? "light",
                EmailNotifications = HttpContext.Session.GetString("EmailNotifications") == "true",
                PushNotifications = HttpContext.Session.GetString("PushNotifications") == "true"
            };
        }

        public IActionResult OnPostUpdateProfile()
        {
            if (!ModelState.IsValid)
            {
                return Page();
            }

            HttpContext.Session.SetString("FullName", Input.FullName ?? "");
            HttpContext.Session.SetString("Email", Input.Email ?? "");
            HttpContext.Session.SetString("PhoneNumber", Input.PhoneNumber ?? "");

            TempData["SuccessMessage"] = "Profile updated successfully!";
            return RedirectToPage();
        }

        public IActionResult OnPostUpdatePreferences()
        {
            if (!ModelState.IsValid)
            {
                return Page();
            }

            HttpContext.Session.SetString("Theme", Input.Theme ?? "light");
            HttpContext.Session.SetString("EmailNotifications", Input.EmailNotifications.ToString());
            HttpContext.Session.SetString("PushNotifications", Input.PushNotifications.ToString());

            TempData["SuccessMessage"] = "Preferences updated successfully!";
            return RedirectToPage();
        }
    }

    public class SettingsInput
    {
        [Display(Name = "Full Name")]
        public string FullName { get; set; }

        [Display(Name = "Email Address")]
        [EmailAddress(ErrorMessage = "Please enter a valid email address")]
        public string Email { get; set; }

        [Display(Name = "Phone Number")]
        [Phone(ErrorMessage = "Please enter a valid phone number")]
        public string PhoneNumber { get; set; }

        [Display(Name = "Theme")]
        public string Theme { get; set; }
  
        [Display(Name = "Email Notifications")]
        public bool EmailNotifications { get; set; }

        [Display(Name = "Push Notifications")]
        public bool PushNotifications { get; set; }
    }
}