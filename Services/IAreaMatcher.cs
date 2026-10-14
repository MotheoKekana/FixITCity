namespace FixITCityAPI.Services;

public interface IAreaMatcher
{
    Task<int?> MatchAsync(
        string? suburb,
        string? city,
        string? addressLine,
        CancellationToken ct = default);
}