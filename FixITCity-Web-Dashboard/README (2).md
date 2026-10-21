# FixITCity Dashboard 🏙️

An administrative web dashboard for municipal staff to manage, track, and resolve citizen-submitted fault reports. Built with ASP.NET Core Razor Pages, this dashboard integrates with a C# Web API and a PostgreSQL database hosted on Supabase.

## 📖 Overview

The FixITCity Dashboard provides municipal operators with a centralized view of civic issues (potholes, broken streetlights, water leaks, etc.) submitted via the FixITCity mobile app. It includes real-time statistics, an interactive map, and detailed workflows for updating report statuses.

## ✨ Key Features

*   **Operations Overview:** Live statistics cards tracking the six core workflow statuses (*Pending, Under Review, Accepted, In Progress, Fixed, Rejected*).
*   **Interactive Map:** A Leaflet-powered map displaying the geographic locations of all submitted reports.
*   **Report Management:** A searchable, filterable, and paginated table of all reports. Includes CSV export functionality.
*   **Fault Details & Actions:** A dedicated view for each report showing photos, priority scores, and allowing admins to update the status and leave audit notes.
*   **Automated Geocoding:** A background service that automatically converts raw coordinates into street names, suburbs, and cities using the OpenStreetMap Nominatim API.
*   **Secure Authentication:** Role-based access with a secure "Forgot Password" flow utilizing secure 6-digit OTP codes sent via email (Resend).
*   **Modern UI:** A clean, responsive interface built with Bootstrap 5 and custom CSS, featuring a side-navigation layout.

## 🛠️ Technology Stack

**Frontend (This Repository)**
*   ASP.NET Core Razor Pages (.NET 8)
*   HTML5 / CSS3 / Vanilla JavaScript
*   Bootstrap 5 (Layout & Components)
*   Leaflet.js (Maps)
*   FontAwesome (Icons)

**Backend & Infrastructure**
*   ASP.NET Core Web API (.NET 10) *(Separate Repository)*
*   Entity Framework Core (ORM)
*   PostgreSQL (Hosted on Supabase)
*   Resend (Transactional Email API)
*   Nominatim (Reverse Geocoding)

## 🚀 Getting Started

### Prerequisites
*   [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0)
*   A [Supabase](https://supabase.com/) account (for the PostgreSQL database)
*   A [Resend](https://resend.com/) account (for sending password reset emails)
*   The FixITCity API project must be running locally (typically on `http://localhost:5000`).

### 1. Database Setup (Supabase)
1. Create a new project in Supabase.
2. Retrieve your **Connection String** from *Project Settings > Database*. Use the "Direct connection" string for EF Core migrations.
3. Add the connection string to your `appsettings.Development.json` (see configuration below).

*(Note: If you are bootstrapping an existing database, you may need to manually seed the `__EFMigrationsHistory` table to avoid "relation already exists" errors.)*

### 2. Run the Application
1. Start the API project:
   ```bash
   cd FixITCityAPI
   dotnet run
   ```
2. Start the Dashboard project:
   ```bash
   cd FixITCity_Dashboard
   dotnet run
   ```
3. Open your browser and navigate to the localhost URL provided in the terminal (usually `https://localhost:5011`).

## 🔐 Password Reset Flow (Resend)

The dashboard implements a custom, secure password reset flow.

1. The user enters their email on the `/ForgotPass` page.
2. The API generates a 6-digit code, hashes it, and stores it in the `password_reset_tokens` table (expires in 5 minutes).
3. The API sends the email via Resend.
4. The user enters the code and their new password.

**⚠️ Resend Sandbox Limitation:** 
If you are using the `onboarding@resend.dev` sender address, **Resend will only deliver emails to the exact email address you used to sign up for your Resend account.** To send emails to real users, you must verify your custom domain in the Resend dashboard.

## 🌍 Background Services (Geocoding)

When a mobile user submits a report, it arrives with only raw GPS coordinates. The API includes a `GeocodingBackfillService` (Hosted Background Service) that runs every 2 minutes.
*   It finds reports where `geocoded_at` is null.
*   It queries the Nominatim API (throttled to 1 request/second per their usage policy).
*   It extracts the street, suburb, and city, and attempts to match the suburb to a predefined `service_areas` table.

## 📁 Project Structure

*   `/Pages`: Razor Pages (`Dashboard`, `Reports`, `FaultDetail`, `Login`, `ForgotPass`).
*   `/Services`: HTTP clients and business logic (e.g., `DashboardService`).
*   `/ViewModels`: View-specific data models.
*   `/wwwroot`: Static assets (`css/`, `js/`, `images/`).
