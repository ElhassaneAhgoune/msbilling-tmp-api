package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "charge_types")
public class ChargeTypes {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name ="charge_type", nullable = false)
    private String chargeType;

    public ChargeTypes() {}

    public ChargeTypes(String code, String chargeType) {
        this.code = code;
        this.chargeType = chargeType;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

}
