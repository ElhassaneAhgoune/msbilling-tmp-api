package com.moneysab.cardexis.repository;


import com.moneysab.cardexis.domain.entity.BusinessTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessTransactionTypeRepository extends JpaRepository<BusinessTransactionType, UUID> {

    Optional<BusinessTransactionType> findByCode(String code);

}