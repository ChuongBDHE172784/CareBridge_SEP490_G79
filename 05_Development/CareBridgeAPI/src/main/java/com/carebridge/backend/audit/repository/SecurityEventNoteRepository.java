package com.carebridge.backend.audit.repository;

import com.carebridge.backend.audit.entity.SecurityEventNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityEventNoteRepository extends JpaRepository<SecurityEventNote, UUID> {

    List<SecurityEventNote> findByEventIdOrderByCreatedAtAsc(Long eventId);
}
