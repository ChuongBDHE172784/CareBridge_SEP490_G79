-- Emergency notification records created before care-group scoping could be
-- delivered by FCM but remained invisible to the Family dashboard, which only
-- queries alerts within an accepted care group. Backfill the single stable
-- active membership for each existing recipient without exposing alerts across
-- unrelated groups.
UPDATE notification_records notification
   SET care_group_id = (
       SELECT member.care_group_id
         FROM safety_events emergency_session
         JOIN care_groups care_group
           ON care_group.owner_user_id = emergency_session.user_id
          AND care_group.status = 'ACTIVE'
         JOIN care_group_members member
           ON member.care_group_id = care_group.care_group_id
          AND member.user_id = notification.user_id
          AND member.invitation_status = 'ACCEPTED'
        WHERE emergency_session.safety_event_id = notification.reference_id
          AND emergency_session.record_type = 'EMERGENCY_SESSION'
        ORDER BY member.care_group_id ASC
        LIMIT 1
   )
 WHERE notification.type = 'EMERGENCY'
   AND notification.reference_type = 'EMERGENCY_SESSION'
   AND notification.care_group_id IS NULL
   AND EXISTS (
       SELECT 1
         FROM safety_events emergency_session
         JOIN care_groups care_group
           ON care_group.owner_user_id = emergency_session.user_id
          AND care_group.status = 'ACTIVE'
         JOIN care_group_members member
           ON member.care_group_id = care_group.care_group_id
          AND member.user_id = notification.user_id
          AND member.invitation_status = 'ACCEPTED'
        WHERE emergency_session.safety_event_id = notification.reference_id
          AND emergency_session.record_type = 'EMERGENCY_SESSION'
   );
