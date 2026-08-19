package com.infosys.account_service.repository;

import com.infosys.account_service.entity.Notification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepo extends CrudRepository<Notification, Integer> {
    List<Notification> findByAccIdOrderByCreatedAtDesc(Integer accId);
}