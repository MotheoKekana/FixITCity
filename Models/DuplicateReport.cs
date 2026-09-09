using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("duplicate_reports")]
public partial class DuplicateReport
{
    public int DuplicateLinkId { get; set; }

    public int? OriginalReportId { get; set; }

    public int? DuplicateReportId { get; set; }

    public int? MarkedBy { get; set; }

    public DateTime? MarkedAt { get; set; }

    public virtual FaultReport? DuplicateReportNavigation { get; set; }

    public virtual MunicipalUser? MarkedByNavigation { get; set; }

    public virtual FaultReport? OriginalReport { get; set; }
}
