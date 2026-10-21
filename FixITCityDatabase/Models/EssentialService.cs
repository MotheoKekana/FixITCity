using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("essential_services")]
public partial class EssentialService
{
    public int ServiceId { get; set; }

    public string? Name { get; set; }

    public string? ServiceType { get; set; }

    public string? AddressLine { get; set; }

    public decimal? Latitude { get; set; }

    public decimal? Longitude { get; set; }
}
