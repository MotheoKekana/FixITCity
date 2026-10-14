using System.Text.Json.Serialization;

namespace FixITCity_Dashboard.Services.Dashboard.Dtos
{
    public class DashboardStatsApiResponse
    {
        [JsonPropertyName("total")] public int Total { get; set; }
        [JsonPropertyName("pending")] public int Pending { get; set; }
        [JsonPropertyName("underReview")] public int UnderReview { get; set; }
        [JsonPropertyName("accepted")] public int Accepted { get; set; }
        [JsonPropertyName("inProgress")] public int InProgress { get; set; }
        [JsonPropertyName("fixed")] public int Fixed { get; set; }
        [JsonPropertyName("rejected")] public int Rejected { get; set; }
    }
}