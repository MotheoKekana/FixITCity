namespace FixITCity_Dashboard.Services.Dashboard.Dtos
{
    public class ReportExportRow
    {
        public string ReferenceNumber { get; set; } = string.Empty;
        public string Title { get; set; } = string.Empty;
        public string CategoryName { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
        public int? PriorityScore { get; set; }
        public DateTime? CreatedAt { get; set; } 
   
    }
}
