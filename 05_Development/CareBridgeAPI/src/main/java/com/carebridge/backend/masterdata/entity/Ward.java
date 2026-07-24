package com.carebridge.backend.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ward {

    @Id
    @Column(name = "ward_id", length = 6)
    @Comment("Ward code (6 chars)")
    private String wardId;

    @Column(name = "district_id", length = 4, nullable = false)
    @Comment("District code (4 chars)")
    private String districtId;

    @Column(name = "province_id", length = 2, nullable = false)
    @Comment("Province code (2 chars)")
    private String provinceId;

    @Column(name = "name", length = 100, nullable = false)
    @Comment("Ward name (VN)")
    private String name;

    @Column(name = "name_en", length = 100)
    @Comment("Ward name (EN)")
    private String nameEn;

    @Column(name = "is_active", nullable = false)
    @Comment("Active flag")
    private Boolean isActive = true;
}