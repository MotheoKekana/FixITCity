using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.ComponentModel.DataAnnotations;

namespace FixITCity_Dashboard.Pages
{
    public class ForgotPassModel : PageModel
    {
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


        public IActionResult OnPostSendCode()
        {

            if (!ModelState.IsValid)
            {
                return Page();
            }


            // Generate a 6-digit verification code
            Random random = new Random();

            string code = random.Next(100000, 999999).ToString();


            // Store the code temporarily in session
            HttpContext.Session.SetString("ResetCode", code);

            // Store the email as well
            HttpContext.Session.SetString("ResetEmail", Email);


            ShowCodeStep = true;

            return Page();
        }


        public IActionResult OnPostVerifyCode()
        {
            string? storedCode =
                HttpContext.Session.GetString("ResetCode");


            if (string.IsNullOrWhiteSpace(VerificationCode))
            {
                ModelState.AddModelError(
                    "VerificationCode",
                    "Please enter the verification code."
                );

                ShowCodeStep = true;

                return Page();
            }


            if (storedCode == null ||
                VerificationCode != storedCode)
            {
                ModelState.AddModelError(
                    "VerificationCode",
                    "The verification code is incorrect."
                );

                ShowCodeStep = true;

                return Page();
            }


            ShowPasswordStep = true;

            return Page();
        }


        public IActionResult OnPostResetPassword()
        {
            if (string.IsNullOrWhiteSpace(NewPassword))
            {
                ModelState.AddModelError(
                    "NewPassword",
                    "Please enter a new password."
                );
            }

            if (!string.IsNullOrWhiteSpace(NewPassword) && NewPassword.Length < 8)
            {
                ModelState.AddModelError(
                    "NewPassword",
                    "Password must contain at least 8 characters."
                );
            }


            if (string.IsNullOrWhiteSpace(ConfirmPassword))
            {
                ModelState.AddModelError(
                    "ConfirmPassword",
                    "Please confirm your password."
                );
            }


            if (NewPassword != ConfirmPassword)
            {
                ModelState.AddModelError(
                    "ConfirmPassword",
                    "Passwords do not match."
                );
            }


            if (!ModelState.IsValid)
            {
                ShowPasswordStep = true;

                return Page();
            }


            // Later:
            // 1. Get user using ResetEmail from session
            // 2. Hash the password
            // 3. Update database

            HttpContext.Session.Remove("ResetCode");


            ShowSuccessStep = true;

            return Page();
        }
    }
}