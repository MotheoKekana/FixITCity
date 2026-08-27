using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.ComponentModel.DataAnnotations;

namespace FixITCity_Dashboard.Pages
{
   public class LoginModel : PageModel
{
    [BindProperty]
    public LoginInput Input { get; set; }

    public void OnGet()
    {
        // Just display the login page
    }

    public IActionResult OnPost()
    {
        // Check if the username was entered
        if (string.IsNullOrWhiteSpace(Input.Username))
        {
            TempData["ErrorMessage"] = "Please enter a username.";
            return Page();
        }

        // Simple login - accept any username
        // In a real app, you'd check against a database here
        TempData["SuccessMessage"] = $"Welcome back, {Input.Username}!";

        // Store the username in session so we can use it later
        HttpContext.Session.SetString("Username", Input.Username);

        // Redirect to the home page or dashboard
        return RedirectToPage("/Index");
    }
}

   
    public class LoginInput
    {
        [Required(ErrorMessage = "Username is required")]
        [Display(Name = "Username")]
        public string Username { get; set; }
    }


}

