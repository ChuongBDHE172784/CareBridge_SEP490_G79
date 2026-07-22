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
@Table(name = "hospitals", indexes = {
 @Index(name = "idx_hospitals_province", columnList = "province_id"),
 @Index(name = "idx_hospitals_search", columnList = "name, province_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {
 @Id
 @Column(name = "hospital_id", length = 8, nullable = false, updatable = false)
 private String hospitalId;

 @Column(name = "name", length = 200, nullable = false)
 private String name;

 @Column(name = "province_id", length = 2, nullable = false)
 private String provinceId;

 @Column(name = "district_id", length = 4)
 private String districtId;

 @Column(name = "address", columnDefinition = "text")
 private String address;

 @Column(name = "level", length = 20)
 private String level; // e.g., "Hạng I", "Hạng II", "Hạng III"

 @Column(name = "type", length = 30)
 private String type; // e.g., "Công lập", "Tư nhân", "Đa khoa khu vực"

 @Column(name = "phone", length = 20)
 private String phone;

 @Column(name = "is_active", nullable = false)
 private Boolean isActive = true;
}
