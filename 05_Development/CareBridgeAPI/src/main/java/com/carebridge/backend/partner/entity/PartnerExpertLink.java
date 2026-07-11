package com.carebridge.backend.partner.entity;
import jakarta.persistence.*;import java.util.UUID;import lombok.*;
@Entity @Table(name="partner_expert_links") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PartnerExpertLink{@Id @Column(name="partner_expert_link_id")private UUID id;@Column(name="partner_id",nullable=false)private UUID partnerId;@Column(nullable=false,length=20)private String status;}
