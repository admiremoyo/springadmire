package com.phegondev.usersmanagementsystem.repository;

import com.phegondev.usersmanagementsystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByUserId(Integer userId); // Use Integer
}



