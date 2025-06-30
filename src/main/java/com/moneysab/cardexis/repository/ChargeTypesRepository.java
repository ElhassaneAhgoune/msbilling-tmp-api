package com.moneysab.cardexis.repository;

import com.moneysab.cardexis.domain.entity.ChargeTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChargeTypesRepository extends JpaRepository<ChargeTypes, UUID> {
    /**
     * Find a charge type by its code.
     *
     * @param code the charge type code
     * @return the charge type entity if found, otherwise null
     */
    ChargeTypes findByCode(String code);
    /**
     * Find a charge type by its charge type name.
     *
     * @param chargeType the charge type name
     * @return the charge type entity if found, otherwise null
     */
    ChargeTypes findByChargeType(String chargeType);
}
