namespace FixITCityAPI.Services;

public interface IReverseGeocodingService
{
    Task<GeocodingResult?> ReverseAsync(
        double latitude,
        double longitude,
        CancellationToken ct = default);
}

public record GeocodingResult(
    string? Street,
    string? Suburb,
    string? City,
    string FormattedAddress);