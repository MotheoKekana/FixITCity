using FixITCity_Dashboard.Services;
using FixITCity_Dashboard.Services.Dashboard;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddRazorPages();

builder.Services.AddDistributedMemoryCache();
builder.Services.AddSession();

// Register Dashboard Service
builder.Services.AddScoped<IDashboardService, DashboardService>();

var apiBaseUrl = builder.Configuration["ApiSettings:BaseUrl"]
                 ?? "http://localhost:5283/api/";
builder.Services.AddHttpContextAccessor();
builder.Services.AddTransient<AuthTokenHandler>();

builder.Services.AddHttpClient("Api", client =>
{
    client.BaseAddress = new Uri(apiBaseUrl);
}).AddHttpMessageHandler<AuthTokenHandler>();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();

app.UseRouting();

app.UseSession();

app.UseAuthorization();

app.MapRazorPages();

app.Run();