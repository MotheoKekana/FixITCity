using System;

namespace FixITCityAPI.Models;

public partial class DeviceToken
{
    public int TokenId { get; set; }
    public int UserId { get; set; }
    public string DeviceToken { get; set; }
    public string DeviceName { get; set; }
    public bool? IsActive { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? LastUsedAt { get; set; }

    public virtual User User { get; set; }
}