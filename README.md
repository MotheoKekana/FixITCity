# FixITCity

FixITCity is a municipal fault-reporting system. Citizens can report infrastructure issues, and municipal staff can review reports, manage their status, and see their priority scores.

This repository combines the FixITCity API and web dashboard. The application uses Supabase for its PostgreSQL database.

## Projects

- **FixITCityAPI** — ASP.NET Core Web API that serves report, account, and dashboard data.
- **FixITCity_Dashboard** — ASP.NET Core Razor Pages application for municipal staff.


## Repository structure

```text
FixITCity/
├── FixITCityAPI/
│   ├── Controllers/
│   ├── Migrations/
│   ├── Models/
│   ├── Services/
│   └── FixITCityAPI.csproj
├── FixITCity_Dashboard/
│   ├── Pages/
│   ├── Services/
│   ├── ViewModels/
│   ├── wwwroot/
│   └── FixITCity Dashboard.csproj
└── README.md
