using System;

namespace FixITCityAPI.Models;

public partial class Notification
{
    public int NotificationId { get; set; }
    public int CitizenId { get; set; }
    public int? ReportId { get; set; }
    public string NotificationType { get; set; }   
    public string Channel { get; set; }
    public string Subject { get; set; }            
    public string Message { get; set; }
    public string DeviceToken { get; set; }        
    public string FcmMessageId { get; set; }       
    public bool? IsSent { get; set; }              
    public DateTime? SentAt { get; set; }
    public bool? IsRead { get; set; }              
    public DateTime? ReadAt { get; set; }
    public bool? IsDelivered { get; set; }         
    public DateTime? DeliveredAt { get; set; }     
    public DateTime? CreatedAt { get; set; }       

    public virtual CitizenProfile Citizen { get; set; }
    public virtual FaultReport Report { get; set; }
}