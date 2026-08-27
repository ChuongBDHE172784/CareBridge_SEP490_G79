package com.carebridge.backend.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContentSource {
    @Column(name = "source_title", nullable = false, length = 500)
    private String title;
    @Column(name = "source_url", length = 2000)
    private String url;
    @Column(name = "source_publisher", length = 255)
    private String publisher;
}
