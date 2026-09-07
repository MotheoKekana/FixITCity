using System;
using System.Collections.Generic;

namespace FixITCityAPI.Models;

public partial class Notification
{
    public int NotificationId { get; set; }

    public int? CitizenId { get; set; }

    public int? ReportId { get; set; }

    public string? Channel { get; set; }

    public string? Message { get; set; }

    public DateTime? SentAt { get; set; }

    public DateTime? ReadAt { get; set; }

    public string DeviceToken { get; set; }              // device_token
    public string FcmMessageId { get; set; }            // fcm_message_id
    public bool? IsSent { get; set; }                   // is_sent
    public bool? IsRead { get; set; }                   // is_read               // read_at
    public bool? IsDelivered { get; set; }              // is_delivered
    public DateTime? DeliveredAt { get; set; }          // delivered_at
    public string NotificationType { get; set; }        // notification_type

    public virtual CitizenProfile? Citizen { get; set; }

    public virtual FaultReport? Report { get; set; }
}
