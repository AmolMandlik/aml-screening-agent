package com.bfsi.aml.repository;

import com.bfsi.aml.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByFullNameIgnoreCase(String fullName);

    List<Customer> findByKycStatus(Customer.KycStatus status);
}
