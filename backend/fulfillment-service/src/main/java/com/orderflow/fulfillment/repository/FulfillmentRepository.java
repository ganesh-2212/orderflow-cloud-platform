package com.orderflow.fulfillment.repository;

import com.orderflow.fulfillment.entity.Fulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long> {
    Optional<Fulfillment> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}
