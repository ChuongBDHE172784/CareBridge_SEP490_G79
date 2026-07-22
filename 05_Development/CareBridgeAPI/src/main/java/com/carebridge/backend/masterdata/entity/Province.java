package com.carebridge.backend.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "provinces")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Province {
 @Id
 @Column(name = "province_id", length = 2, nullable = false, updatable = false)
 private String provinceId;

 @Column(name = "name", length = 100, nullable = false)
 private String name;

 @Column(name = "name_en", length = 100)
 private String nameEn;

 @Column(name = "region", length = 20)
 private String region;

 @Column(name = "is_active", nullable = false)
 private Boolean isActive = true;
}
