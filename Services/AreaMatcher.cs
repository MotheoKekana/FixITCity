using FixITCityAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FixITCityAPI.Services;

public sealed class AreaMatcher : IAreaMatcher
{
    private readonly FixITCityDbContext _db;

    public AreaMatcher(FixITCityDbContext db) => _db = db;

    public async Task<int?> MatchAsync(
        string? suburb,
        string? city,
        string? addressLine,
        CancellationToken ct = default)
    {
        // Pull all active areas once — the list is small and static.
        var areas = await _db.ServiceAreas
            .Where(a => a.IsActive == true && a.AreaName != null)
            .Select(a => new { a.AreaId, a.AreaName })
            .ToListAsync(ct);

        if (areas.Count == 0) return null;

        // 1. Exact suburb match.
        if (!string.IsNullOrWhiteSpace(suburb))
        {
            var match = areas.FirstOrDefault(a =>
                a.AreaName!.Equals(suburb, StringComparison.OrdinalIgnoreCase));
            if (match != null) return match.AreaId;
        }

        // 2. Suburb contained in an area name (e.g. "Erasmusrand" vs "Erasmusrand Ext 4").
        if (!string.IsNullOrWhiteSpace(suburb))
        {
            var suburbLower = suburb.ToLower();
            var match = areas.FirstOrDefault(a =>
                a.AreaName!.ToLower().Contains(suburbLower));
            if (match != null) return match.AreaId;
        }

        // 3. Scan the full formatted address for any known area name.
        //    "MTLC Building, Summerstrand, Gqeberha, 6019" -> matches "Summerstrand".
        if (!string.IsNullOrWhiteSpace(addressLine))
        {
            var match = areas.FirstOrDefault(a =>
                addressLine.Contains(a.AreaName!, StringComparison.OrdinalIgnoreCase));
            if (match != null) return match.AreaId;
        }

        // 4. City match — last resort.
        if (!string.IsNullOrWhiteSpace(city))
        {
            var match = areas.FirstOrDefault(a =>
                a.AreaName!.Equals(city, StringComparison.OrdinalIgnoreCase));
            if (match != null) return match.AreaId;
        }

        return null;
    }
}