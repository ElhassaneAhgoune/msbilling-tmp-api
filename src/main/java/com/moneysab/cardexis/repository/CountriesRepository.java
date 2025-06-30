package com.moneysab.cardexis.repository;

import com.moneysab.cardexis.domain.entity.Countries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CountriesRepository extends JpaRepository<Countries, UUID> {

    /**
     * Find a country by its code.
     *
     * @param countryCode the country code
     * @return the country entity if found, otherwise null
     */
    Optional<Countries> findByCountryCode(String countryCode);

    /**
     * Find a country by its name.
     *
     * @param countryName the country name
     * @return the country entity if found, otherwise null
     */
    Optional<Countries> findByCountryName(String countryName);
}
