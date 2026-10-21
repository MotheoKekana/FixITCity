-- ============================================================
-- FIXITCITY SUPABASE DATABASE SETUP SCRIPT
-- Extracted from the supplied database build script.
--
-- IMPORTANT:
-- This is intended for a NEW, EMPTY Supabase project only.
-- Do not run it blindly against the existing FixITCity database.
-- It creates tables, functions, triggers, grants, and RLS policies.
-- Several CREATE POLICY statements are not idempotent and will fail
-- if the policies already exist. CREATE TABLE IF NOT EXISTS also
-- does not update an existing table to match this script.
-- Review and test changes before applying them to a live database.
-- ============================================================

-- ============================================================
-- FIXITCITY DATABASE BUILD SCRIPT
-- Version: 1.0 (Final)
-- Target: Supabase (PostgreSQL)
-- ============================================================

-- ============================================================
-- PART 1: CORE TABLES
-- ============================================================

-- 1. Users (Custom profile linked to Supabase Auth)
CREATE TABLE IF NOT EXISTS public.users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    supabase_user_id UUID UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'Citizen' CHECK (role IN ('Citizen', 'Municipal')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 2. Citizen Profiles
CREATE TABLE IF NOT EXISTS public.citizen_profiles (
    citizen_profile_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES public.users(user_id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    address_line VARCHAR(255),
    city VARCHAR(100),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    notification_preference VARCHAR(20) DEFAULT 'Push',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 3. Municipal Users
CREATE TABLE IF NOT EXISTS public.municipal_users (
    municipal_user_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES public.users(user_id) ON DELETE CASCADE,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    position_title VARCHAR(100) NOT NULL,
    hire_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Fault Categories
CREATE TABLE IF NOT EXISTS public.fault_categories (
    category_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    severity_weight DECIMAL(4,2) NOT NULL CHECK (severity_weight BETWEEN 1 AND 10),
    estimated_fix_time INT,
    icon VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE
);

-- 5. Essential Services
CREATE TABLE IF NOT EXISTS public.essential_services (
    service_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    service_type VARCHAR(50) NOT NULL CHECK (service_type IN ('School', 'Clinic', 'Hospital', 'Police', 'Fire Station')),
    address_line VARCHAR(255),
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    ward_number VARCHAR(10),
    contact_number VARCHAR(20)
);

-- 6. Service Areas
CREATE TABLE IF NOT EXISTS public.service_areas (
    area_id BIGSERIAL PRIMARY KEY,
    area_name VARCHAR(50) NOT NULL UNIQUE,
    ward_number VARCHAR(10) NOT NULL,
    description VARCHAR(255),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. Fault Reports
CREATE TABLE IF NOT EXISTS public.fault_reports (
    report_id BIGSERIAL PRIMARY KEY,
    citizen_id BIGINT REFERENCES public.citizen_profiles(citizen_profile_id),
    guest_auth_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES public.fault_categories(category_id),
    area_id BIGINT REFERENCES public.service_areas(area_id),
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    address_line VARCHAR(255),
    status VARCHAR(30) DEFAULT 'Pending' CHECK (status IN ('Pending', 'Under Review', 'Accepted', 'In Progress', 'Fixed', 'Rejected')),
    priority_score INT CHECK (priority_score BETWEEN 0 AND 100),
    reference_number VARCHAR(20) NOT NULL UNIQUE,
    verified_by BIGINT REFERENCES public.municipal_users(municipal_user_id),
    verified_at TIMESTAMP WITH TIME ZONE,
    assigned_to BIGINT REFERENCES public.municipal_users(municipal_user_id),
    resolution_notes VARCHAR(500),
    resolved_at TIMESTAMP WITH TIME ZONE,
    is_duplicate BOOLEAN DEFAULT FALSE,
    is_offline BOOLEAN DEFAULT FALSE,
    sync_status VARCHAR(20) DEFAULT 'Synced' CHECK (sync_status IN ('Pending', 'Synced', 'Failed')),
    offline_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    -- Enforce exactly one owner type
    CONSTRAINT fault_reports_owner_check CHECK (
        (citizen_id IS NOT NULL AND guest_auth_user_id IS NULL)
        OR
        (citizen_id IS NULL AND guest_auth_user_id IS NOT NULL)
    )
);

-- 8. Report Photos
CREATE TABLE IF NOT EXISTS public.report_photos (
    photo_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES public.fault_reports(report_id) ON DELETE CASCADE,
    photo_url VARCHAR(500) NOT NULL,
    supabase_storage_path VARCHAR(500),
    is_primary BOOLEAN DEFAULT FALSE,
    is_offline BOOLEAN DEFAULT FALSE,
    sync_status VARCHAR(20) DEFAULT 'Synced' CHECK (sync_status IN ('Pending', 'Synced', 'Failed')),
    offline_photo_path VARCHAR(500),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 9. Duplicate Reports
CREATE TABLE IF NOT EXISTS public.duplicate_reports (
    duplicate_link_id BIGSERIAL PRIMARY KEY,
    original_report_id BIGINT NOT NULL REFERENCES public.fault_reports(report_id),
    duplicate_report_id BIGINT NOT NULL REFERENCES public.fault_reports(report_id),
    match_percentage DECIMAL(5,2) NOT NULL,
    marked_by BIGINT REFERENCES public.municipal_users(municipal_user_id),
    marked_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_merged BOOLEAN DEFAULT FALSE,
    CONSTRAINT different_reports CHECK (original_report_id != duplicate_report_id)
);

-- 10. Priority Scores
CREATE TABLE IF NOT EXISTS public.priority_scores (
    score_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL UNIQUE REFERENCES public.fault_reports(report_id) ON DELETE CASCADE,
    total_score DECIMAL(6,2) NOT NULL CHECK (total_score BETWEEN 0 AND 100),
    severity_factor DECIMAL(6,2) NOT NULL CHECK (severity_factor BETWEEN 0 AND 25),
    proximity_factor DECIMAL(6,2) NOT NULL CHECK (proximity_factor BETWEEN 0 AND 35),
    duplicate_factor DECIMAL(6,2) NOT NULL CHECK (duplicate_factor BETWEEN 0 AND 20),
    age_factor DECIMAL(6,2) NOT NULL CHECK (age_factor BETWEEN 0 AND 20),
    computed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 11. Fault Status History
CREATE TABLE IF NOT EXISTS public.fault_status_history (
    history_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES public.fault_reports(report_id) ON DELETE CASCADE,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by BIGINT REFERENCES public.municipal_users(municipal_user_id),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    notes VARCHAR(255)
);

-- 12. Report Feedback
CREATE TABLE IF NOT EXISTS public.report_feedback (
    feedback_id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL UNIQUE REFERENCES public.fault_reports(report_id) ON DELETE CASCADE,
    citizen_id BIGINT NOT NULL REFERENCES public.citizen_profiles(citizen_profile_id),
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(500),
    response_time INT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

-- 13. Notifications (Updated for Push Processing)
CREATE TABLE IF NOT EXISTS public.notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    citizen_id BIGINT NOT NULL REFERENCES public.citizen_profiles(citizen_profile_id),
    report_id BIGINT REFERENCES public.fault_reports(report_id) ON DELETE SET NULL,
    notification_type VARCHAR(50) CHECK (notification_type IN ('StatusUpdate', 'Assignment', 'Reminder', 'Alert')),
    channel VARCHAR(20) DEFAULT 'Push' CHECK (channel IN ('Email', 'SMS', 'Push')),
    subject VARCHAR(200) NOT NULL,
    message VARCHAR(500) NOT NULL,
    device_token VARCHAR(255),
    fcm_message_id VARCHAR(100),
    is_sent BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP WITH TIME ZONE,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    is_delivered BOOLEAN DEFAULT FALSE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    -- Push processing fields
    push_claim_token UUID,
    push_claimed_at TIMESTAMP WITH TIME ZONE,
    push_attempts INT NOT NULL DEFAULT 0,
    push_last_error TEXT
);

-- 14. Audit Logs
CREATE TABLE IF NOT EXISTS public.audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    municipal_user_id BIGINT NOT NULL REFERENCES public.municipal_users(municipal_user_id),
    action VARCHAR(100) NOT NULL,
    table_affected VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 15. Device Tokens
CREATE TABLE IF NOT EXISTS public.device_tokens (
    token_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(user_id) ON DELETE CASCADE,
    device_token VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE
);

-- 16. Report Votes
CREATE TABLE IF NOT EXISTS public.report_votes (
    vote_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    report_id BIGINT NOT NULL REFERENCES public.fault_reports(report_id) ON DELETE CASCADE,
    citizen_id BIGINT NOT NULL REFERENCES public.citizen_profiles(citizen_profile_id) ON DELETE CASCADE,
    voted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT report_votes_unique_per_citizen UNIQUE (report_id, citizen_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_fault_reports_citizen ON public.fault_reports(citizen_id);
CREATE INDEX IF NOT EXISTS idx_fault_reports_status ON public.fault_reports(status);
CREATE INDEX IF NOT EXISTS idx_fault_reports_created ON public.fault_reports(created_at);
CREATE INDEX IF NOT EXISTS idx_fault_reports_latlong ON public.fault_reports(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_report_photos_report ON public.report_photos(report_id);
CREATE INDEX IF NOT EXISTS idx_notifications_citizen ON public.notifications(citizen_id);

-- ============================================================
-- PART 2: VIEWS
-- ============================================================

CREATE OR REPLACE VIEW public.fault_reports_with_votes AS
SELECT 
    fr.*,
    COALESCE(COUNT(rv.vote_id), 0)::int AS vote_count
FROM public.fault_reports fr
LEFT JOIN public.report_votes rv ON rv.report_id = fr.report_id
GROUP BY fr.report_id;

GRANT SELECT ON public.fault_reports_with_votes TO authenticated, anon;

-- ============================================================
-- PART 3: FUNCTIONS & TRIGGERS
-- ============================================================

-- A. Auth Trigger (Skips Guests, Creates Citizens)
CREATE OR REPLACE FUNCTION public.handle_new_citizen_registration()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id BIGINT;
    v_is_anonymous BOOLEAN;
BEGIN
    -- Detect Anonymous User (Checks metadata, app metadata, and auth column)
    v_is_anonymous := COALESCE(
        (new.raw_app_meta_data ->> 'provider') = 'anonymous',
        (new.raw_user_meta_data ->> 'is_anonymous')::boolean,
        (new.is_anonymous),
        false
    );

    IF v_is_anonymous THEN
        RETURN new;
    END IF;

    -- Insert public.users
    INSERT INTO public.users (email, supabase_user_id, role, is_active, created_at)
    VALUES (new.email, new.id, 'Citizen', true, NOW())
    ON CONFLICT (supabase_user_id) DO UPDATE SET email = EXCLUDED.email, updated_at = NOW()
    RETURNING user_id INTO v_user_id;

    -- Insert citizen_profiles
    INSERT INTO public.citizen_profiles (user_id, first_name, last_name, phone_number, created_at)
    VALUES (
        v_user_id, 
        COALESCE(NULLIF(BTRIM(new.raw_user_meta_data ->> 'first_name'), ''), 'Citizen'), 
        COALESCE(NULLIF(BTRIM(new.raw_user_meta_data ->> 'last_name'), ''), ''), 
        NULLIF(BTRIM(new.raw_user_meta_data ->> 'phone_number'), ''), 
        NOW()
    )
    ON CONFLICT (user_id) DO NOTHING;

    RETURN new;
EXCEPTION
    WHEN OTHERS THEN
        RAISE LOG 'handle_new_citizen_registration failed for %: %', new.id, SQLERRM;
        RETURN new; -- Never block auth signup
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_citizen_registration();

-- B. Auto-populate Status History
CREATE OR REPLACE FUNCTION public.log_fault_report_creation()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.fault_status_history (report_id, old_status, new_status, changed_by, notes)
    VALUES (NEW.report_id, NULL, NEW.status, NULL, 'Report created');
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_fault_reports_creation_history ON public.fault_reports;
CREATE TRIGGER trg_fault_reports_creation_history
AFTER INSERT ON public.fault_reports
FOR EACH ROW EXECUTE FUNCTION public.log_fault_report_creation();

-- C. Log Status Changes & Notify Citizens
CREATE OR REPLACE FUNCTION public.notify_citizen_on_status_change()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_citizen_id BIGINT;
    v_reference VARCHAR;
BEGIN
    IF NEW.status IS NOT DISTINCT FROM OLD.status THEN
        RETURN NEW;
    END IF;

    -- Log History
    INSERT INTO public.fault_status_history (report_id, old_status, new_status, changed_by, notes)
    VALUES (NEW.report_id, OLD.status, NEW.status, NULL, 'Status changed via system');

    -- Notify Citizen (Skip if guest)
    SELECT citizen_id, reference_number INTO v_citizen_id, v_reference
    FROM public.fault_reports WHERE report_id = NEW.report_id;

    IF v_citizen_id IS NOT NULL THEN
        INSERT INTO public.notifications (
            citizen_id, report_id, notification_type, channel, subject, message, is_sent, created_at
        ) VALUES (
            v_citizen_id, NEW.report_id, 'StatusUpdate', 'Push',
            'Report status updated',
            'Your report ' || COALESCE(v_reference, '(unknown)') || ' is now ''' || NEW.status || '''.',
            false, NOW()
        );
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_on_status_change ON public.fault_reports;
CREATE TRIGGER trg_notify_on_status_change
AFTER UPDATE ON public.fault_reports
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION public.notify_citizen_on_status_change();

-- ============================================================
-- PART 4: RPCs (REST API CALLABLE FUNCTIONS)
-- ============================================================

-- A. Nearby Reports (Guests Blocked)
CREATE OR REPLACE FUNCTION public.nearby_reports(lat DOUBLE PRECISION, lng DOUBLE PRECISION)
RETURNS TABLE (
    report_id BIGINT, category_id BIGINT, citizen_id BIGINT, reference_number VARCHAR,
    title VARCHAR, latitude NUMERIC, longitude NUMERIC, status VARCHAR, priority_score INT, vote_count INT
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = true THEN
        RAISE EXCEPTION 'Guests cannot access nearby reports';
    END IF;

    RETURN QUERY
    SELECT fr.report_id, fr.category_id, fr.citizen_id, fr.reference_number, fr.title,
           fr.latitude, fr.longitude, fr.status, fr.priority_score, COALESCE(COUNT(rv.vote_id), 0)::int
    FROM public.fault_reports fr
    LEFT JOIN public.report_votes rv ON rv.report_id = fr.report_id
    WHERE fr.status IN ('Pending', 'Under Review')
      AND (6371000 * 2 * ASIN(SQRT(POWER(SIN(RADIANS(fr.latitude - lat) / 2), 2) + COS(RADIANS(lat)) * COS(RADIANS(fr.latitude)) * POWER(SIN(RADIANS(fr.longitude - lng) / 2), 2)))) <= 2000
    GROUP BY fr.report_id
    ORDER BY fr.priority_score DESC NULLS LAST, fr.created_at ASC
    LIMIT 100;
END;
$$;
REVOKE ALL ON FUNCTION public.nearby_reports(DOUBLE PRECISION, DOUBLE PRECISION) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.nearby_reports(DOUBLE PRECISION, DOUBLE PRECISION) TO authenticated;

-- B. Guest Track Report
CREATE OR REPLACE FUNCTION public.guest_track_report(p_reference text)
RETURNS TABLE (
    report_id BIGINT, reference_number VARCHAR, title VARCHAR,
    status VARCHAR, priority_score INT, created_at TIMESTAMP WITH TIME ZONE
)
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public
AS $$
BEGIN
    IF COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = false THEN
        RAISE EXCEPTION 'Only anonymous users can call this function';
    END IF;

    RETURN QUERY
    SELECT fr.report_id, fr.reference_number, fr.title, fr.status, fr.priority_score, fr.created_at
    FROM public.fault_reports fr
    WHERE fr.reference_number = upper(trim(p_reference))
    LIMIT 1;
END;
$$;
REVOKE ALL ON FUNCTION public.guest_track_report(text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.guest_track_report(text) TO authenticated;

-- C. Push Notification Claim/Finish (Service Role Only)
CREATE OR REPLACE FUNCTION public.claim_notification_push(p_notification_id BIGINT)
RETURNS SETOF public.notifications
LANGUAGE sql SECURITY DEFINER SET search_path = ''
AS $$
  UPDATE public.notifications AS n
  SET push_claim_token = gen_random_uuid(),
      push_claimed_at = now(),
      push_attempts = n.push_attempts + 1,
      push_last_error = NULL
  WHERE n.notification_id = p_notification_id
    AND n.is_sent IS NOT TRUE
    AND (n.push_claimed_at IS NULL OR n.push_claimed_at < now() - interval '10 minutes')
  RETURNING n.*;
$$;

CREATE OR REPLACE FUNCTION public.finish_notification_push(
  p_notification_id BIGINT, p_claim_token UUID,
  p_sent BOOLEAN, p_message_id TEXT, p_error TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''
AS $$
DECLARE v_changed INTEGER;
BEGIN
  UPDATE public.notifications
  SET is_sent = p_sent,
      sent_at = CASE WHEN p_sent THEN timezone('UTC', now()) ELSE sent_at END,
      fcm_message_id = COALESCE(left(p_message_id, 100), fcm_message_id),
      push_last_error = CASE WHEN p_sent THEN NULL ELSE left(p_error, 1000) END,
      push_claim_token = NULL,
      push_claimed_at = CASE WHEN p_sent THEN NULL ELSE push_claimed_at END
  WHERE notification_id = p_notification_id AND push_claim_token = p_claim_token;
  GET DIAGNOSTICS v_changed = ROW_COUNT;
  RETURN v_changed = 1;
END;
$$;

REVOKE ALL ON FUNCTION public.claim_notification_push(BIGINT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.finish_notification_push(BIGINT, UUID, BOOLEAN, TEXT, TEXT) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.claim_notification_push(BIGINT) TO service_role;
GRANT EXECUTE ON FUNCTION public.finish_notification_push(BIGINT, UUID, BOOLEAN, TEXT, TEXT) TO service_role;

-- ============================================================
-- PART 5: ROW LEVEL SECURITY (RLS) & GRANTS
-- ============================================================

-- Enable RLS
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.citizen_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.municipal_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fault_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.essential_services ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.service_areas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fault_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.report_photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.duplicate_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.priority_scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fault_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.report_feedback ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.report_votes ENABLE ROW LEVEL SECURITY;

-- Grants
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON public.users, public.citizen_profiles, public.fault_categories, public.service_areas, public.essential_services, public.fault_reports, public.report_photos, public.priority_scores, public.fault_status_history TO authenticated;
GRANT SELECT ON public.fault_categories TO anon;
GRANT SELECT, INSERT ON public.fault_reports TO authenticated, anon;
GRANT SELECT, INSERT ON public.report_photos TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE ON public.device_tokens TO authenticated;
GRANT SELECT, UPDATE ON public.notifications TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.report_votes TO authenticated;
GRANT SELECT, INSERT ON public.report_feedback TO authenticated;
GRANT SELECT ON public.fault_reports_with_votes TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE ON public.fault_reports TO service_role;
GRANT SELECT ON public.notifications, public.citizen_profiles, public.device_tokens TO service_role;
GRANT UPDATE (is_active) ON public.device_tokens TO service_role;

-- Policies: Users & Profiles
CREATE POLICY "Users can read own row" ON public.users FOR SELECT TO authenticated USING (supabase_user_id = auth.uid());
CREATE POLICY "Citizens can manage own profile" ON public.citizen_profiles FOR ALL TO authenticated USING (user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid()));
CREATE POLICY "Municipal can read all citizen profiles" ON public.citizen_profiles FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.users WHERE supabase_user_id = auth.uid() AND role = 'Municipal'));

-- Policies: Lookups
CREATE POLICY "Anyone can read active fault categories" ON public.fault_categories FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Anyone can read service areas" ON public.service_areas FOR SELECT TO anon, authenticated USING (is_active = true);
CREATE POLICY "Anyone can read essential services" ON public.essential_services FOR SELECT TO anon, authenticated USING (true);

-- Policies: Fault Reports
CREATE POLICY "Citizens can insert own reports" ON public.fault_reports FOR INSERT TO authenticated WITH CHECK (citizen_id IS NOT NULL AND guest_auth_user_id IS NULL AND EXISTS (SELECT 1 FROM public.citizen_profiles cp JOIN public.users u ON u.user_id = cp.user_id WHERE cp.citizen_profile_id = fault_reports.citizen_id AND u.supabase_user_id = auth.uid() AND u.role = 'Citizen' AND u.is_active = true));
CREATE POLICY "Guests can insert own reports" ON public.fault_reports FOR INSERT TO anon, authenticated WITH CHECK (citizen_id IS NULL AND guest_auth_user_id = auth.uid() AND COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = true);
CREATE POLICY "Owners can read own reports" ON public.fault_reports FOR SELECT TO authenticated, anon USING ((citizen_id IS NOT NULL AND EXISTS (SELECT 1 FROM public.citizen_profiles cp JOIN public.users u ON u.user_id = cp.user_id WHERE cp.citizen_profile_id = fault_reports.citizen_id AND u.supabase_user_id = auth.uid() AND u.role = 'Citizen' AND u.is_active = true)) OR (citizen_id IS NULL AND guest_auth_user_id = auth.uid()));
CREATE POLICY "Authenticated users can read non-resolved reports" ON public.fault_reports FOR SELECT TO authenticated USING (status <> 'Fixed' AND COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = false);
CREATE POLICY "Municipal can read all reports" ON public.fault_reports FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.users WHERE supabase_user_id = auth.uid() AND role = 'Municipal' AND is_active = true));

-- Policies: Report Photos
CREATE POLICY "Owners can insert own report photos" ON public.report_photos FOR INSERT TO authenticated, anon WITH CHECK (EXISTS (SELECT 1 FROM public.fault_reports fr LEFT JOIN public.citizen_profiles cp ON cp.citizen_profile_id = fr.citizen_id LEFT JOIN public.users u ON u.user_id = cp.user_id WHERE fr.report_id = report_photos.report_id AND ((u.supabase_user_id = auth.uid() AND u.role = 'Citizen' AND u.is_active = true) OR (fr.guest_auth_user_id = auth.uid()))));
CREATE POLICY "Owners can read own report photos" ON public.report_photos FOR SELECT TO authenticated, anon USING (EXISTS (SELECT 1 FROM public.fault_reports fr LEFT JOIN public.citizen_profiles cp ON cp.citizen_profile_id = fr.citizen_id LEFT JOIN public.users u ON u.user_id = cp.user_id WHERE fr.report_id = report_photos.report_id AND ((u.supabase_user_id = auth.uid() AND u.role = 'Citizen' AND u.is_active = true) OR (fr.guest_auth_user_id = auth.uid()))));
CREATE POLICY "Municipal can read all report photos" ON public.report_photos FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.users WHERE supabase_user_id = auth.uid() AND role = 'Municipal' AND is_active = true));

-- Policies: Status History & Scores
CREATE POLICY "Citizens can read own report history" ON public.fault_status_history FOR SELECT TO authenticated USING (report_id IN (SELECT fr.report_id FROM public.fault_reports fr JOIN public.citizen_profiles cp ON cp.citizen_profile_id = fr.citizen_id JOIN public.users u ON u.user_id = cp.user_id WHERE u.supabase_user_id = auth.uid()));
CREATE POLICY "Municipal can read all report history" ON public.fault_status_history FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.users WHERE supabase_user_id = auth.uid() AND role = 'Municipal'));
CREATE POLICY "Authenticated can read priority scores" ON public.priority_scores FOR SELECT TO authenticated USING (true);

-- Policies: Notifications
CREATE POLICY "Citizens can read own notifications" ON public.notifications FOR SELECT TO authenticated USING (citizen_id IN (SELECT citizen_profile_id FROM public.citizen_profiles WHERE user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid())));
CREATE POLICY "Citizens can update own notifications" ON public.notifications FOR UPDATE TO authenticated USING (citizen_id IN (SELECT citizen_profile_id FROM public.citizen_profiles WHERE user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid()))) WITH CHECK (citizen_id IN (SELECT citizen_profile_id FROM public.citizen_profiles WHERE user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid())));
CREATE POLICY "Municipal can read all notifications" ON public.notifications FOR SELECT TO authenticated USING (EXISTS (SELECT 1 FROM public.users WHERE supabase_user_id = auth.uid() AND role = 'Municipal'));

-- Policies: Device Tokens
CREATE POLICY "Users manage own device tokens" ON public.device_tokens FOR ALL TO authenticated USING (user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid())) WITH CHECK (user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid()));

-- Policies: Report Votes (Upvoting)
CREATE POLICY "Citizens can read report votes" ON public.report_votes FOR SELECT TO authenticated USING (COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = false);
CREATE POLICY "Citizens can insert own votes" ON public.report_votes FOR INSERT TO authenticated WITH CHECK (COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = false AND citizen_id IN (SELECT citizen_profile_id FROM public.citizen_profiles WHERE user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid())));
CREATE POLICY "Citizens can delete own votes" ON public.report_votes FOR DELETE TO authenticated USING (COALESCE((auth.jwt() ->> 'is_anonymous')::boolean, false) = false AND citizen_id IN (SELECT citizen_profile_id FROM public.citizen_profiles WHERE user_id IN (SELECT user_id FROM public.users WHERE supabase_user_id = auth.uid())));

-- Policies: Storage
CREATE POLICY "Authenticated users can upload report photos" ON storage.objects FOR INSERT TO authenticated, anon WITH CHECK (bucket_id = 'reports');
CREATE POLICY "Anyone can read report photos" ON storage.objects FOR SELECT USING (bucket_id = 'reports');

-- ============================================================
-- END OF SCRIPT
-- ============================================================
