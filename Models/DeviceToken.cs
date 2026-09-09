using System;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("device_tokens")]
public partial class DeviceToken
{
    public int TokenId { get; set; }
    public int UserId { get; set; }
    public string Token { get; set; }
    public string DeviceName { get; set; }
    public bool? IsActive { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? LastUsedAt { get; set; }

    public virtual User User { get; set; }
}