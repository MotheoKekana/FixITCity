using System;
using System.Collections.Generic;

namespace FixITCityAPI.Models;

public partial class ReportVote
{
    public long VoteId { get; set; }
    public long ReportId { get; set; }
    public long CitizenId { get; set; }
    public DateTime VotedAt { get; set; }

    public virtual FaultReport Report { get; set; } = null!;
    public virtual CitizenProfile Citizen { get; set; } = null!;
}