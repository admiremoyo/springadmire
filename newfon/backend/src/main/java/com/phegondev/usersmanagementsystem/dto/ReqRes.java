package com.phegondev.usersmanagementsystem.dto;

import com.phegondev.usersmanagementsystem.entity.OurUsers;
import com.phegondev.usersmanagementsystem.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReqRes {
    private Integer statusCode;
    private String message;
    private String error;
    private OurUsers ourUsers;
    private BigDecimal standPrice;
    private BigDecimal totalPayments;
    private BigDecimal balance;
    private List<Payment> payments;
    private List<OurUsers> ourUsersList;
    private String token;
    private String refreshToken;
    private String expirationTime;

    // Fields that might be used in registration
    private String fileNumber;
    private String standNumber;
    private String yearOfPurchase;
    private String director;
    private String squareMetres;
    private String descriptionOfStand;
    private String cellNumber;
    private String email;
    private String role;
    private String name;
    private String password;
}
