
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace FixITCity_Dashboard.Pages
{
    public class LogoutModel : PageModel
    {
        public IActionResult OnPost()
        {
            // Clear the signed-in user's session data.
            HttpContext.Session.Clear();

            // Return the user to the login page.
            return RedirectToPage("/Login");
        }

        public IActionResult OnGet()
        {
            // Visiting /Logout directly does not sign the user out.
            return RedirectToPage("/Login");
        }
    }
}
