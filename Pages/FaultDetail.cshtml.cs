using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System;
using System.Collections.Generic;

namespace FixITCity_Dashboard.Pages
{
    public class FaultDetailModel : PageModel
    {
        public string PhotoUrl { get; set; } = "";
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public int PriorityScore { get; set; }
        public List<AuditLogEntry> AuditLogs { get; set; } = new();

        public void OnGet(int id)
        {
            // Dummy data for testing
            PhotoUrl = "/images/sample-fault.jpg";
            Latitude = -33.957;
            Longitude = 25.602;
            PriorityScore = 78;

            AuditLogs = new List<AuditLogEntry>
            {
                new AuditLogEntry { Action = "Created", AdminName = "System", Timestamp = DateTime.Now.AddDays(-2), Reason = "Initial submission" },
                new AuditLogEntry { Action = "Reviewed", AdminName = "Admin1", Timestamp = DateTime.Now.AddDays(-1), Reason = "Checked details" }
            };
        }

        public IActionResult OnPostVerify(int id)
        {
            AddAuditLog("Verified", "AdminUser", "Report verified and moved to ranked queue");
            return RedirectToPage(new { id });
        }

        public IActionResult OnPostReject(int id, string reason)
        {
            AddAuditLog("Rejected", "AdminUser", reason ?? "No reason provided");
            return RedirectToPage(new { id });
        }

        public IActionResult OnPostUpdateStatus(int id, string statusUpdate)
        {
            AddAuditLog($"Status updated to {statusUpdate}", "AdminUser", "");
            return RedirectToPage(new { id });
        }

        private void AddAuditLog(string action, string admin, string reason)
        {
            AuditLogs.Add(new AuditLogEntry
            {
                Action = action,
                AdminName = admin,
                Timestamp = DateTime.Now,
                Reason = reason
            });

            // TODO: Persist to AuditLog table in database
        }
    }

    public class AuditLogEntry
    {
        public string Action { get; set; } = "";
        public string AdminName { get; set; } = "";
        public DateTime Timestamp { get; set; }
        public string Reason { get; set; } = "";
    }
}