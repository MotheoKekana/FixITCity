# FixITCity - Database

This directory contains all database-related scripts and documentation for the FixITCity project. The database stores fault reports, user profiles, priority scores, and audit logs for the infrastructure fault prioritization system.

---

## Database Overview

| Aspect | Details |
| :--- | :--- |
| **Database Name** | FixITCityDB |
| **Database Engine** | Microsoft SQL Server (2019+) |
| **Management Tool** | SQL Server Management Studio (SSMS) |
| **Total Tables** | 14 Tables |
| **Schema** | `dbo` (default) |
| **Migration Approach** | Database-First with Entity Framework Core |

---

## Database Schema (14 Tables)

| # | Table Name | Description | Priority |
| :--- | :--- | :--- | :--- |
| 1 | `Users` | Login credentials for all system users | Critical |
| 2 | `CitizenProfiles` | Extended profile data for citizens | High |
| 3 | `MunicipalUsers` | Extended profile data for administrators | High |
| 4 | `FaultCategories` | Predefined fault types with severity weights | Critical |
| 5 | `EssentialServices` | Schools, clinics, hospitals with GPS coordinates | Critical |
| 6 | `FaultReports` | Core fault report data (the heart of the system) | Critical |
| 7 | `ReportPhotos` | Multiple photos per fault report | Medium |
| 8 | `DuplicateReports` | Links between duplicate fault reports | Medium |
| 9 | `PriorityScores` | Computed priority scores with factor breakdown | Critical |
| 10 | `FaultStatusHistory` | Audit trail of all status changes | High |
| 11 | `Notifications` | Log of notifications sent to citizens | Medium |
| 12 | `AuditLogs` | Log of all administrative actions | High |
| 13 | `ServiceAreas` | Geographic zones for grouping faults by region | Medium |
| 14 | `ReportAnalytics` | Pre-computed summary data for faster reporting | Medium |

---

## Table Details

### 1. Users (CRITICAL)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `UserID` | INT (PK) | Unique user identifier |
| `Email` | NVARCHAR(100) | User's email address (unique) |
| `PasswordHash` | NVARCHAR(200) | Securely hashed password |
| `Role` | NVARCHAR(20) | Citizen, Administrator |
| `CreatedAt` | DATETIME | Account creation timestamp |

**Relationships:** One-to-many with `CitizenProfiles`, `MunicipalUsers`, `AuditLogs`

**Purpose:** Stores all user login credentials and authentication data. This is the foundation of all system access.

---

### 2. CitizenProfiles (HIGH)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `ProfileID` | INT (PK) | Unique profile identifier |
| `UserID` | INT (FK) | References Users.UserID |
| `FullName` | NVARCHAR(100) | Citizen's full name |
| `PhoneNumber` | NVARCHAR(15) | Contact phone number |
| `Address` | NVARCHAR(200) | Residential address |
| `RegistrationDate` | DATETIME | Profile creation date |

**Relationships:** Many-to-one with `Users`; one-to-many with `FaultReports`, `Notifications`

**Purpose:** Stores extended citizen profile data. Links a `User` account to citizen-specific information.

---

### 3. MunicipalUsers (HIGH)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `MunicipalUserID` | INT (PK) | Unique municipal user identifier |
| `UserID` | INT (FK) | References Users.UserID |
| `EmployeeNumber` | NVARCHAR(20) | Employee identification number |
| `Department` | NVARCHAR(50) | Municipal department |

**Relationships:** Many-to-one with `Users`; one-to-many with `FaultStatusHistory`, `AuditLogs`

**Purpose:** Stores extended municipal staff profile data. Links a `User` account to administrator-specific information.

---

### 4. FaultCategories (CRITICAL)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `CategoryID` | INT (PK) | Unique category identifier |
| `CategoryName` | NVARCHAR(50) | Name of the fault type |
| `SeverityWeight` | INT | Weight for scoring algorithm (1-10) |

**Sample Data:**

| CategoryID | CategoryName | SeverityWeight |
| :--- | :--- | :--- |
| 1 | Burst Water Pipe | 10 |
| 2 | Pothole | 7 |
| 3 | Streetlight Outage | 4 |
| 4 | Electrical Outage | 8 |
| 5 | Sewage Leak | 9 |
| 6 | Pavement Crack | 2 |

**Relationships:** One-to-many with `FaultReports`

**Purpose:** Stores predefined fault types with their severity weights. Citizens select a category when reporting a fault. The severity weight is a key factor in the priority-scoring algorithm.

---

### 5. EssentialServices (CRITICAL)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `ServiceID` | INT (PK) | Unique service identifier |
| `Name` | NVARCHAR(100) | Name of the facility |
| `Type` | NVARCHAR(20) | School, Clinic, Hospital |
| `Latitude` | DECIMAL(9,6) | GPS latitude coordinate |
| `Longitude` | DECIMAL(9,6) | GPS longitude coordinate |

**Sample Data:**

| ServiceID | Name | Type | Latitude | Longitude |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Gqeberha Primary School | School | -33.924900 | 18.424100 |
| 2 | Gqeberha Clinic | Clinic | -33.925000 | 18.425000 |
| 3 | Gqeberha Hospital | Hospital | -33.920000 | 18.430000 |

**Relationships:** No direct relationships (reference data only)

**Purpose:** Stores GPS coordinates of schools, clinics, and hospitals. Used by the priority-scoring algorithm to calculate proximity. Faults near these services receive higher scores.

---

### 6. FaultReports (CRITICAL)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `ReportID` | INT (PK) | Unique report identifier |
| `CitizenID` | INT (FK) | References CitizenProfiles.ProfileID |
| `CategoryID` | INT (FK) | References FaultCategories.CategoryID |
| `GPS_Lat` | DECIMAL(9,6) | GPS latitude of fault |
| `GPS_Lng` | DECIMAL(9,6) | GPS longitude of fault |
| `Description` | NVARCHAR(500) | Citizen's description of the fault |
| `PhotoURL` | NVARCHAR(200) | URL to the uploaded photo |
| `Status` | NVARCHAR(20) | Reported, Verified, In Progress, Completed |
| `CreatedAt` | DATETIME | Report submission timestamp |
| `ReferenceNumber` | NVARCHAR(20) | Unique tracking number |

**Relationships:** Many-to-one with `CitizenProfiles`, `FaultCategories`; one-to-many with `ReportPhotos`, `PriorityScores`, `FaultStatusHistory`, `Notifications`

**Purpose:** The core table storing every fault report submitted by citizens. All other tables relate back to this one.

---

### 7. ReportPhotos (MEDIUM)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `PhotoID` | INT (PK) | Unique photo identifier |
| `ReportID` | INT (FK) | References FaultReports.ReportID |
| `ImageURL` | NVARCHAR(200) | URL to the stored image |
| `Timestamp` | DATETIME | Photo upload timestamp |

**Relationships:** Many-to-one with `FaultReports`

**Purpose:** Stores multiple photos associated with a single fault report. Supports the camera functionality of the mobile app.

---

### 8. DuplicateReports (MEDIUM)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `DuplicateID` | INT (PK) | Unique duplicate record identifier |
| `PrimaryReportID` | INT (FK) | References FaultReports.ReportID (original report) |
| `DuplicateReportID` | INT (FK) | References FaultReports.ReportID (duplicate report) |

**Relationships:** Self-referencing; two foreign keys to `FaultReports`

**Purpose:** Links duplicate reports of the same fault. Reduces administrative noise by identifying when multiple citizens report the same fault. The duplicate count influences the priority score.

---

### 9. PriorityScores (CRITICAL)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `ScoreID` | INT (PK) | Unique score identifier |
| `ReportID` | INT (FK) | References FaultReports.ReportID |
| `Score` | INT | Total priority score (0-100) |
| `ProximityFactor` | INT | Score contribution from proximity (0-35) |
| `DuplicateFactor` | INT | Score contribution from duplicate count (0-20) |
| `SeverityFactor` | INT | Score contribution from category severity (0-25) |
| `TimeFactor` | INT | Score contribution from elapsed time (0-20) |
| `CalculatedAt` | DATETIME | Score calculation timestamp |

**Relationships:** Many-to-one with `FaultReports`

**Purpose:** Stores the computed priority score and its contributing factors for each fault report. This is the innovation of the system--it provides transparency by storing the "why" behind each score.

---

### 10. FaultStatusHistory (HIGH)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `HistoryID` | INT (PK) | Unique history record identifier |
| `ReportID` | INT (FK) | References FaultReports.ReportID |
| `Status` | NVARCHAR(20) | Status at this point in time |
| `ChangedAt` | DATETIME | Status change timestamp |
| `ChangedBy` | INT (FK) | References Users.UserID |
| `Note` | NVARCHAR(200) | Optional note about the change |

**Relationships:** Many-to-one with `FaultReports`; many-to-one with `Users`

**Purpose:** Provides an audit trail of every status change for a fault report. Ensures accountability and transparency.

---

### 11. Notifications (MEDIUM)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `NotificationID` | INT (PK) | Unique notification identifier |
| `ReportID` | INT (FK) | References FaultReports.ReportID |
| `CitizenID` | INT (FK) | References CitizenProfiles.ProfileID |
| `Type` | NVARCHAR(20) | SMS, Email, In-App |
| `Message` | NVARCHAR(500) | Notification content |
| `SentAt` | DATETIME | Notification sent timestamp |

**Relationships:** Many-to-one with `FaultReports`; many-to-one with `CitizenProfiles`

**Purpose:** Logs all notifications sent to citizens about their report progress.

---

### 12. AuditLogs (HIGH)

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `LogID` | INT (PK) | Unique log identifier |
| `UserID` | INT (FK) | References Users.UserID |
| `Action` | NVARCHAR(100) | Action performed |
| `Timestamp` | DATETIME | Action timestamp |
| `Details` | NVARCHAR(500) | Additional action details |

**Relationships:** Many-to-one with `Users`

**Purpose:** Logs every action performed by municipal users. Provides a comprehensive audit trail for security and accountability.

---

### 13. ServiceAreas (MEDIUM) - NEW

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `AreaID` | INT (PK) | Unique area identifier |
| `AreaName` | NVARCHAR(50) | Name of the area (e.g., "Ward 1", "Northern Suburbs") |
| `Description` | NVARCHAR(200) | Description of the area boundaries |

**Sample Data:**

| AreaID | AreaName | Description |
| :--- | :--- | :--- |
| 1 | Ward 1 | Central Gqeberha |
| 2 | Ward 2 | Northern Suburbs |
| 3 | Ward 3 | Southern Suburbs |

**Relationships:** One-to-many with `FaultReports` (optional)

**Purpose:** Groups faults by geographic region. Enables administrators to identify problem hotspots and allocate resources more effectively.

**Why Added:** Improves reporting and analytics by allowing faults to be grouped by region. Helps identify which areas have the most infrastructure issues.

---

### 14. ReportAnalytics (MEDIUM) - NEW

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `AnalyticsID` | INT (PK) | Unique analytics record identifier |
| `ReportID` | INT (FK) | References FaultReports.ReportID |
| `ResolutionTimeMinutes` | INT | Time taken to resolve the fault (in minutes) |
| `MonthYear` | INT | Reporting period (YYYYMM) |
| `WeekNumber` | INT | Week of the year |

**Relationships:** Many-to-one with `FaultReports`

**Purpose:** Stores pre-computed summary data for faster reporting and analytics. Improves dashboard performance by avoiding complex aggregate queries.

**Why Added:** Speeds up reporting by pre-calculating key metrics. Instead of calculating resolution times or aggregating data on every dashboard request, this table stores pre-computed values for quick retrieval.

---

## Database Scripts

| Script | Description |
| :--- | :--- |
| `01_CreateTables.sql` | Creates all 14 tables with primary keys, foreign keys, and constraints |
| `02_SeedData.sql` | Populates reference data (FaultCategories, EssentialServices, ServiceAreas, etc.) |
| `03_Indexes.sql` | Creates performance-enhancing indexes |
| `04_StoredProcedures.sql` | Contains priority scoring, duplicate detection, and ranked queue procedures |
| `05_TestData.sql` | Inserts sample data for testing and development |

---

## Folder Structure
