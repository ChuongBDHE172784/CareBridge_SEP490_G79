package com.carebridge.backend.content.repository;

import java.util.UUID;

public interface TemplateItemCount {
    UUID getTemplateId();
    long getItemCount();
}
