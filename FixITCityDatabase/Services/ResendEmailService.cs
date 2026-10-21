using Resend;
using System.Net.Mail;

namespace FixITCityAPI.Services;

public sealed class ResendEmailService : IEmailService
{
    private readonly IResend _resend;
    private readonly ILogger<ResendEmailService> _logger;
    private readonly IConfiguration _config;

    public ResendEmailService(
        IResend resend,
        ILogger<ResendEmailService> logger,
        IConfiguration config)
    {
        _resend = resend;
        _logger = logger;
        _config = config;
    }

    public async Task SendPasswordResetCodeAsync(
        string toEmail, string code, CancellationToken ct = default)
    {
        var from = _config["Resend:FromAddress"] ?? "FixITCity <onboarding@resend.dev>";

        var message = new EmailMessage();
        message.From = from;
        message.To.Add(toEmail);
        message.Subject = "Your FixITCity password reset code";
        message.HtmlBody = $@"
            <div style=""font-family:Segoe UI,Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;"">
                <h2 style=""color:#009688;margin:0 0 12px;"">Password reset</h2>
                <p style=""color:#333;line-height:1.5;"">
                    Use the code below to reset your FixITCity password.
                    It expires in <strong>5 minute</strong>.
                </p>
                <div style=""font-size:32px;font-weight:700;letter-spacing:8px;
                            color:#009688;background:#f4f8f7;padding:16px;
                            text-align:center;border-radius:8px;margin:20px 0;"">
                    {code}
                </div>
                <p style=""color:#888;font-size:13px;line-height:1.5;"">
                    If you did not request this, you can safely ignore this email.
                </p>
            </div>";

        try
        {
            await _resend.EmailSendAsync(message, ct);
            _logger.LogInformation("Password reset code sent to {Email}", toEmail);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send reset email to {Email}", toEmail);
            throw;
        }
    }
}