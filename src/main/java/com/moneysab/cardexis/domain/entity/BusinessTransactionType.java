package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "business_transaction_type")
public class BusinessTransactionType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name ="label", nullable = false)
    private String label;

    public BusinessTransactionType() {}

    public BusinessTransactionType(String code, String label) {
        this.code = code;
        this.label = label;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
