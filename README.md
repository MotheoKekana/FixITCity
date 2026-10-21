# FixITCity

FixITCity is a municipal fault-reporting system. Citizens can report infrastructure problems, and municipal staff can review reports, manage their status, and see priority scores.

This repository contains the FixITCity API, the staff dashboard, and SQL scripts for the Supabase PostgreSQL database.

## Projects

- **FixITCityDatabase** — ASP.NET Core Web API that provides data and services to the dashboard.
- **FixITCity_Dashboard** — ASP.NET Core Razor Pages dashboard for municipal staff.
- **FixITCity_guest** - Android app for ciizens
- **database** — SQL scripts for setting up and managing the Supabase database.

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
├── database/
│   └── FixITCity_Supabase_Setup.sql
└── README.md

Requirements
- .NET 10 SDK
- Access to the FixITCity Supabase project
- A Resend API key for features that send email
