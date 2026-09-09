using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("municipal_users")]
public partial class MunicipalUser
{
    public int MunicipalUserId { get; set; }

    public int? UserId { get; set; }

    public string? Department { get; set; }

    public string? PositionTitle { get; set; }

    public virtual ICollection<AuditLog> AuditLogs { get; set; } = new List<AuditLog>();

    public virtual ICollection<DuplicateReport> DuplicateReports { get; set; } = new List<DuplicateReport>();

    public virtual ICollection<FaultReport> FaultReports { get; set; } = new List<FaultReport>();

    public virtual ICollection<FaultStatusHistory> FaultStatusHistories { get; set; } = new List<FaultStatusHistory>();

    public virtual User? User { get; set; }
}
