using Microsoft.AspNetCore.Mvc.RazorPages;

namespace FixITCity_Dashboard.Pages
{
    public class IndexModel : PageModel
    {
        public string Username { get; set; } = "";

        public void OnGet()
        {
            Username = HttpContext.Session.GetString("Username") ?? "";
        }
    }
}