using FixITCity_Dashboard.Services;
using FixITCity_Dashboard.Services.Dashboard;

var builder = WebApplication.CreateBuilder(args);

// ---------------------------------------------------------
// 1. Add services to the container
// ---------------------------------------------------------

// Razor Pages
builder.Services.AddRazorPages();

// Session (used to store the JWT token after login)
builder.Services.AddDistributedMemoryCache();
builder.Services.AddSession(options =>
{
    options.IdleTimeout = TimeSpan.FromMinutes(60);
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
});

// Dashboard service
builder.Services.AddScoped<IDashboardService, DashboardService>();

// Access to the current HTTP context (needed for session in AuthTokenHandler)
builder.Services.AddHttpContextAccessor();

// JWT handler that attaches the token to every API request
builder.Services.AddTransient<AuthTokenHandler>();

// Base URL for the API
var apiBaseUrl = builder.Configuration["ApiSettings:BaseUrl"]
                 ?? "http://localhost:5283/api/";

// HTTP client for talking to the API
builder.Services.AddHttpClient("Api", client =>
{
    client.BaseAddress = new Uri(apiBaseUrl);
}).AddHttpMessageHandler<AuthTokenHandler>();

// ---------------------------------------------------------
// 2. Build the application
// ---------------------------------------------------------
var app = builder.Build();

// ---------------------------------------------------------
// 3. Configure the HTTP request pipeline
// ---------------------------------------------------------
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();

app.UseRouting();

app.UseSession();

// If you add ASP.NET Core authentication later, uncomment this:
// app.UseAuthentication();

app.UseAuthorization();

app.MapRazorPages();

app.Run();