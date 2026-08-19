package com.infosys.auth_service.repository;

import com.infosys.auth_service.entity.Teller;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TellerRepo extends CrudRepository<Teller, Integer> {
    Teller findByUsername(String username);
}