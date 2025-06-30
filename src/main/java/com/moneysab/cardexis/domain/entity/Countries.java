package com.moneysab.cardexis.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "countries")
public class Countries {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "country_code", unique = true, nullable = false)
    private String countryCode;

    @Column(name ="country_name", nullable = false)
    private String countryName;

    public Countries() {}

    public Countries(String countryCode, String countryName) {
        this.countryCode = countryCode;
        this.countryName = countryName;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
}
