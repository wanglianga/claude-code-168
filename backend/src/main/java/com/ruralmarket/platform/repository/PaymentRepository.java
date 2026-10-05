package com.ruralmarket.platform.repository;

import com.ruralmarket.platform.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByVendorIdOrderByCreatedAtDesc(Long vendorId);
    List<Payment> findByResultId(Long resultId);
    List<Payment> findByEventId(Long eventId);
}
