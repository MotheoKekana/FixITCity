using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("users")]
public partial class User
{
    public int UserId { get; set; }

    public string? Email { get; set; }

    [Column("full_name")]
    public string? FullName { get; set; }

    public string? PasswordHash { get; set; }

    public string? Role { get; set; }

    public DateTime? CreatedAt { get; set; }

     public bool? IsActive { get; set; }          
    public DateTime? LastLogin { get; set; }    
    public DateTime? UpdatedAt { get; set; }

    public virtual ICollection<CitizenProfile> CitizenProfiles { get; set; } = new List<CitizenProfile>();

    public virtual ICollection<DeviceToken> DeviceTokens { get; set; } = new List<DeviceToken>();
    public virtual ICollection<MunicipalUser> MunicipalUsers { get; set; } = new List<MunicipalUser>();
}
