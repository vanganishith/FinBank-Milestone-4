package com.infosys.customer_service.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.infosys.customer_service.entity.Customer;

@Repository
public interface CustomerRepo extends CrudRepository<Customer, Integer> {
    Customer findByUsername(String username);
    Customer findByEmail(String email);
    Customer findByPhone(String phone);
}