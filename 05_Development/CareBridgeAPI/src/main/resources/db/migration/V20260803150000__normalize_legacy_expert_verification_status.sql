-- VERIFIED was the legacy name for a positively reviewed expert profile.
-- The canonical application enum uses APPROVED; normalize existing rows so
-- the JPA enum mapper can safely load the complete admin directory.
UPDATE users
SET verification_status = 'APPROVED'
WHERE role = 'EXPERT'
  AND verification_status = 'VERIFIED';
