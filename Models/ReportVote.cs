using System;
using System.Collections.Generic;

namespace FixITCityAPI.Models;

public partial class ReportVote
{
    public int VoteId { get; set; }
    public int ReportId { get; set; }
    public int CitizenId { get; set; }
    public DateTime VotedAt { get; set; }

    public virtual FaultReport Report { get; set; } = null!;
    public virtual CitizenProfile Citizen { get; set; } = null!;
}