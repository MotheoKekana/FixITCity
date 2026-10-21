using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("priority_scores")]
public partial class PriorityScore
{
    public int ScoreId { get; set; }

    public int? ReportId { get; set; }

    public decimal? TotalScore { get; set; }

    public decimal? SeverityFactor { get; set; }

    public decimal? ProximityFactor { get; set; }

    public decimal? DuplicateFactor { get; set; }

    public decimal? AgeFactor { get; set; }

    public DateTime? ComputedAt { get; set; }

    public virtual FaultReport? Report { get; set; }
}
