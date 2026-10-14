using System.Globalization;
using System.Text.Json;

namespace FixITCityAPI.Services;

public sealed class NominatimReverseGeocodingService : IReverseGeocodingService
{
    private readonly HttpClient _http;
    private readonly ILogger<NominatimReverseGeocodingService> _logger;

    private static readonly SemaphoreSlim _rateLock = new(1, 1);
    private static DateTime _lastCallUtc = DateTime.MinValue;
    private const int MinIntervalMs = 1100;

    private const string UserAgent = "FixITCity/1.0 (ops@fixitcity.example)";

    public NominatimReverseGeocodingService(
        HttpClient http,
        ILogger<NominatimReverseGeocodingService> logger)
    {
        _http = http;
        _logger = logger;
    }

    public async Task<GeocodingResult?> ReverseAsync(
        double latitude,
        double longitude,
        CancellationToken ct = default)
    {
        if (latitude == 0 && longitude == 0)
            return null;

        await ThrottleAsync(ct);

        var url = string.Format(
            CultureInfo.InvariantCulture,
            "https://nominatim.openstreetmap.org/reverse?lat={0}&lon={1}&format=jsonv2&addressdetails=1&zoom=18",
            latitude, longitude);

        using var request = new HttpRequestMessage(HttpMethod.Get, url);
        request.Headers.UserAgent.ParseAdd(UserAgent);
        request.Headers.Accept.ParseAdd("application/json");

        try
        {
            using var response = await _http.SendAsync(request, ct);

            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning(
                    "Nominatim returned {Status} for {Lat},{Lng}",
                    (int)response.StatusCode, latitude, longitude);
                return null;
            }

            await using var stream = await response.Content.ReadAsStreamAsync(ct);
            using var doc = await JsonDocument.ParseAsync(stream, cancellationToken: ct);
            var root = doc.RootElement;

            if (!root.TryGetProperty("address", out var address))
            {
                _logger.LogInformation("No address block for {Lat},{Lng}", latitude, longitude);
                return null;
            }

            var displayName = GetString(root, "display_name");

            // ─── Street ────────────────────────────────────────
            var street = GetString(address, "road")
                      ?? GetString(address, "pedestrian")
                      ?? GetString(address, "footway")
                      ?? GetString(address, "path");

            // ─── City ──────────────────────────────────────────
            var city = GetString(address, "city")
                    ?? GetString(address, "town")
                    ?? GetString(address, "village")
                    ?? GetString(address, "municipality");

            // ─── Suburb ────────────────────────────────────────
            var suburb = FirstValidNonWard(street,
                ExtractSuburbFromDisplayName(displayName, city, street),
                GetString(address, "neighbourhood"),
                GetString(address, "suburb"),
                GetString(address, "quarter"),
                GetString(address, "city_district"));

            var formatted = displayName ?? "";

            return new GeocodingResult(street, suburb, city, formatted);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Reverse geocoding failed for {Lat},{Lng}", latitude, longitude);
            return null;
        }
    }

    private static string? GetString(JsonElement el, string name)
        => el.TryGetProperty(name, out var v) && v.ValueKind == JsonValueKind.String
            ? v.GetString()
            : null;

    private static string? FirstValidNonWard(string? street, params string?[] candidates)
    {
        foreach (var c in candidates)
        {
            if (string.IsNullOrWhiteSpace(c)) continue;
            if (c.Contains("Ward", StringComparison.OrdinalIgnoreCase)) continue;
            if (!string.IsNullOrWhiteSpace(street) &&
                c.Equals(street, StringComparison.OrdinalIgnoreCase)) continue;
            return c;
        }
        return null;
    }

    private static string? ExtractSuburbFromDisplayName(
        string? displayName, string? city, string? street)
    {
        if (string.IsNullOrWhiteSpace(displayName)) return null;

        var parts = displayName
            .Split(',', StringSplitOptions.TrimEntries)
            .Where(p => !string.IsNullOrWhiteSpace(p))
            .ToArray();

        if (parts.Length < 2) return null;

        int cityIndex = -1;
        if (!string.IsNullOrWhiteSpace(city))
        {
            for (int i = 0; i < parts.Length; i++)
            {
                if (parts[i].Equals(city, StringComparison.OrdinalIgnoreCase))
                {
                    cityIndex = i;
                    break;
                }
            }
        }

        int start = cityIndex > 0 ? cityIndex - 1 : parts.Length - 1;

        for (int i = start; i >= 0; i--)
        {
            var candidate = parts[i];

            if (string.IsNullOrWhiteSpace(candidate)) continue;
            if (candidate.All(char.IsDigit)) continue;
            if (candidate.Contains("Ward", StringComparison.OrdinalIgnoreCase)) continue;
            if (candidate.Equals("South Africa", StringComparison.OrdinalIgnoreCase)) continue;
            if (!string.IsNullOrWhiteSpace(street) &&
                candidate.Equals(street, StringComparison.OrdinalIgnoreCase)) continue;

            return candidate;
        }

        return null;
    }

    private static async Task ThrottleAsync(CancellationToken ct)
    {
        await _rateLock.WaitAsync(ct);
        try
        {
            var elapsed = DateTime.UtcNow - _lastCallUtc;
            var remaining = MinIntervalMs - (int)elapsed.TotalMilliseconds;
            if (remaining > 0)
                await Task.Delay(remaining, ct);

            _lastCallUtc = DateTime.UtcNow;
        }
        finally
        {
            _rateLock.Release();
        }
    }
}