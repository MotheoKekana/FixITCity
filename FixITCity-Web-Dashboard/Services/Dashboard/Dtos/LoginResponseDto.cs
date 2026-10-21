using System.Text.Json.Serialization;

namespace FixITCity_Dashboard.Services.Dashboard.Dtos
{
    public class LoginResponseDto
    {
        [JsonPropertyName("message")] public string Message { get; set; } = "";
        [JsonPropertyName("token")] public string Token { get; set; } = "";
        [JsonPropertyName("user_id")] public int UserId { get; set; }
        [JsonPropertyName("email")] public string Email { get; set; } = "";
        [JsonPropertyName("role")] public string Role { get; set; } = "";
    }
}