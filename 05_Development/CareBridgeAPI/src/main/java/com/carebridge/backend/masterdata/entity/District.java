package com.carebridge.backend.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "districts", indexes = {
 @Index(name = "idx_districts_province", columnList = "province_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class District {
 @Id
 @Column(name = "district_id", length = 4, nullable = false, updatable = false)
 private String districtId;

 @Column(name = "province_id", length = 2, nullable = false)
 private String provinceId;

 @Column(name = "name", length = 100, nullable = false)
 private String name;

 @Column(name = "name_en", length = 100)
 private String nameEn;

 @Column(name = "is_active", nullable = false)
 private Boolean isActive = true;
}
