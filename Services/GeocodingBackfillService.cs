using FixITCityAPI.Models;
using Microsoft.EntityFrameworkCore;

namespace FixITCityAPI.Services;

public sealed class GeocodingBackfillService : BackgroundService
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<GeocodingBackfillService> _logger;

    private const int BatchSize = 20;
    private const int MaxAttempts = 5;
    private static readonly TimeSpan RunInterval = TimeSpan.FromMinutes(2);

    public GeocodingBackfillService(
        IServiceScopeFactory scopeFactory,
        ILogger<GeocodingBackfillService> logger)
    {
        _scopeFactory = scopeFactory;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        // Small initial delay so the app finishes starting up first.
        await Task.Delay(TimeSpan.FromSeconds(30), stoppingToken);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await ProcessBatchAsync(stoppingToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Geocoding backfill run failed");
            }

            await Task.Delay(RunInterval, stoppingToken);
        }
    }

    private async Task ProcessBatchAsync(CancellationToken ct)
    {
        using var scope = _scopeFactory.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<FixITCityDbContext>();
        var geocoder = scope.ServiceProvider.GetRequiredService<IReverseGeocodingService>();
        var matcher = scope.ServiceProvider.GetRequiredService<IAreaMatcher>();

        var pending = await db.FaultReports
            .Where(r => r.GeocodedAt == null
                     && r.GeocodeAttempts < MaxAttempts
                     && (r.Latitude != 0 || r.Longitude != 0))
            .OrderBy(r => r.ReportId)
            .Take(BatchSize)
            .ToListAsync(ct);

        if (pending.Count == 0)
        {
            _logger.LogDebug("Geocoding backfill: nothing to do");
            return;
        }

        _logger.LogInformation("Geocoding backfill: processing {Count} reports", pending.Count);

        foreach (var report in pending)
        {
            // Count the attempt before the call so a crash mid-request still
            // counts, preventing an infinite retry hot loop.
            report.GeocodeAttempts++;

            var result = await geocoder.ReverseAsync(
                (double)report.Latitude, (double)report.Longitude, ct);

            if (result == null) continue;

            if (string.IsNullOrWhiteSpace(report.AddressLine))
                report.AddressLine = result.FormattedAddress;

            report.StreetName = result.Street ?? report.StreetName;
            report.SuburbName = result.Suburb ?? report.SuburbName;
            report.CityName = result.City ?? report.CityName;

            if (report.AreaId == null)
                report.AreaId = await matcher.MatchAsync(
                    result.Suburb, result.City, report.AddressLine, ct);

            report.GeocodedAt = DateTime.UtcNow;
        }

        await db.SaveChangesAsync(ct);
    }
}