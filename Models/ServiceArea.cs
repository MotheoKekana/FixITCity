using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("service_areas")]
public partial class ServiceArea
{
    public int AreaId { get; set; }

    public string? AreaName { get; set; }

    public string? WardNumber { get; set; }

    public string? Description { get; set; }

    public decimal? Latitude { get; set; }

    public decimal? Longitude { get; set; }

    public virtual ICollection<FaultReport> FaultReports { get; set; } = new List<FaultReport>();
}
