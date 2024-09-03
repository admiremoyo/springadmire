package com.phegondev.usersmanagementsystem.service;

import com.phegondev.usersmanagementsystem.dto.ReqRes;
import com.phegondev.usersmanagementsystem.entity.OurUsers;
import com.phegondev.usersmanagementsystem.entity.Payment;
import com.phegondev.usersmanagementsystem.repository.PaymentRepository;
import com.phegondev.usersmanagementsystem.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
public class UsersManagementService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;
    public Integer getUserIdByEmail(String email) {
        Optional<OurUsers> userOptional = usersRepo.findByEmail(email);
        return userOptional.map(OurUsers::getId).orElse(null);
    }
    // Utility methods to avoid code duplication
    private BigDecimal calculateTotalPayments(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateBalance(BigDecimal standPrice, BigDecimal totalPayments) {
        return standPrice.subtract(totalPayments);
    }

    public ReqRes register(ReqRes registrationRequest) {
        ReqRes resp = new ReqRes();
        try {
            OurUsers ourUser = new OurUsers();
            ourUser.setFileNumber(registrationRequest.getFileNumber());
            ourUser.setStandNumber(registrationRequest.getStandNumber());
            ourUser.setYearOfPurchase(registrationRequest.getYearOfPurchase());
            ourUser.setDirector(registrationRequest.getDirector());
            ourUser.setSquareMetres(registrationRequest.getSquareMetres());
            ourUser.setDescriptionOfStand(registrationRequest.getDescriptionOfStand());
            ourUser.setCellNumber(registrationRequest.getCellNumber());
            ourUser.setEmail(registrationRequest.getEmail());
            ourUser.setRole(registrationRequest.getRole());
            ourUser.setName(registrationRequest.getName());
            ourUser.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
            ourUser.setStandPrice(registrationRequest.getStandPrice());

            // Save the user first
            OurUsers savedUser = usersRepo.save(ourUser);

            // Create and save the payment if provided
            if (registrationRequest.getPayments() != null && !registrationRequest.getPayments().isEmpty()) {
                for (Payment payment : registrationRequest.getPayments()) {
                    payment.setUser(savedUser);
                    paymentRepository.save(payment);
                }
            }

            // Fetch the payments for the saved user
            List<Payment> payments = paymentRepository.findByUserId(savedUser.getId());
            savedUser.setPayments(payments);

            // Calculate total payments and balance
            BigDecimal totalPayments = calculateTotalPayments(payments);
            BigDecimal balance = calculateBalance(savedUser.getStandPrice(), totalPayments);

            resp.setOurUsers(savedUser);
            resp.setTotalPayments(totalPayments);
            resp.setStandPrice(savedUser.getStandPrice());
            resp.setBalance(balance);
            resp.setMessage("User and payments saved successfully");
            resp.setStatusCode(200);
        } catch (Exception e) {
            resp.setStatusCode(500);
            resp.setError(e.getMessage());
        }
        return resp;
    }

    public ReqRes updateUser(Integer userId, OurUsers updatedUser) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers existingUser = userOptional.get();
                existingUser.setEmail(updatedUser.getEmail());
                existingUser.setName(updatedUser.getName());
                existingUser.setRole(updatedUser.getRole());
                existingUser.setFileNumber(updatedUser.getFileNumber());
                existingUser.setStandNumber(updatedUser.getStandNumber());
                existingUser.setYearOfPurchase(updatedUser.getYearOfPurchase());
                existingUser.setDirector(updatedUser.getDirector());
                existingUser.setSquareMetres(updatedUser.getSquareMetres());
                existingUser.setDescriptionOfStand(updatedUser.getDescriptionOfStand());
                existingUser.setCellNumber(updatedUser.getCellNumber());
                existingUser.setStandPrice(updatedUser.getStandPrice());

                if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                    existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                }

                // Save updated user
                OurUsers savedUser = usersRepo.save(existingUser);

                // Handle payments
                List<Payment> payments = updatedUser.getPayments();
                if (payments != null) {
                    for (Payment payment : payments) {
                        payment.setUser(savedUser); // Associate payment with the updated user
                        paymentRepository.save(payment); // Save new payments or update existing ones
                    }
                }

                // Fetch updated payments
                payments = paymentRepository.findByUserId(savedUser.getId());
                savedUser.setPayments(payments);

                // Calculate total payments and balance
                BigDecimal totalPayments = calculateTotalPayments(payments);
                BigDecimal balance = calculateBalance(savedUser.getStandPrice(), totalPayments);

                reqRes.setOurUsers(savedUser);
                reqRes.setTotalPayments(totalPayments);
                reqRes.setStandPrice(savedUser.getStandPrice());
                reqRes.setBalance(balance);
                reqRes.setStatusCode(200);
                reqRes.setMessage("User updated successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for update");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while updating user: " + e.getMessage());
        }
        return reqRes;
    }



    public ReqRes login(ReqRes loginRequest) {
        ReqRes response = new ReqRes();
        try {
            authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));
            OurUsers user = usersRepo.findByEmail(loginRequest.getEmail()).orElseThrow();
            String jwt = jwtUtils.generateToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(new HashMap<>(), user);

            response.setStatusCode(200);
            response.setToken(jwt);
            response.setRole(user.getRole());
            response.setRefreshToken(refreshToken);
            response.setExpirationTime("24Hrs");
            response.setMessage("Successfully Logged In");

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public ReqRes refreshToken(ReqRes refreshTokenRequest) {
        ReqRes response = new ReqRes();
        try {
            String ourEmail = jwtUtils.extractUsername(refreshTokenRequest.getToken());
            OurUsers users = usersRepo.findByEmail(ourEmail).orElseThrow();
            if (jwtUtils.isTokenValid(refreshTokenRequest.getToken(), users)) {
                String jwt = jwtUtils.generateToken(users);
                response.setStatusCode(200);
                response.setToken(jwt);
                response.setRefreshToken(refreshTokenRequest.getToken());
                response.setExpirationTime("24Hr");
                response.setMessage("Successfully Refreshed Token");
            } else {
                response.setStatusCode(401);
                response.setMessage("Invalid Token");
            }
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public ReqRes getAllUsers() {
        ReqRes reqRes = new ReqRes();
        try {
            List<OurUsers> users = usersRepo.findAll();

            // Loop through users to set payments and calculate totals
            for (OurUsers user : users) {
                List<Payment> payments = paymentRepository.findByUserId(user.getId());
                user.setPayments(payments);

                // Calculate total payments and balance
                BigDecimal totalPayments = calculateTotalPayments(payments);
                BigDecimal balance = calculateBalance(user.getStandPrice(), totalPayments);

                user.setTotalPayments(totalPayments);
                user.setBalance(balance);
            }

            reqRes.setOurUsersList(users);
            reqRes.setStatusCode(200);
            reqRes.setMessage("Users fetched successfully");
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while fetching users: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes getUsersById(Integer userId) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers user = userOptional.get();
                List<Payment> payments = paymentRepository.findByUserId(user.getId());
                user.setPayments(payments);

                // Calculate total payments and balance
                BigDecimal totalPayments = calculateTotalPayments(payments);
                BigDecimal balance = calculateBalance(user.getStandPrice(), totalPayments);

                reqRes.setOurUsers(user);
                reqRes.setTotalPayments(totalPayments);
                reqRes.setStandPrice(user.getStandPrice());
                reqRes.setBalance(balance);
                reqRes.setStatusCode(200);
                reqRes.setMessage("User fetched successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while fetching user: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes deleteUser(Integer userId) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                usersRepo.deleteById(userId);
                reqRes.setStatusCode(200);
                reqRes.setMessage("User deleted successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for deletion");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while deleting user: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes addPayment(Integer userId, Payment payment) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers user = userOptional.get();
                payment.setUser(user);
                paymentRepository.save(payment);

                // Fetch the updated payments
                List<Payment> payments = paymentRepository.findByUserId(user.getId());
                user.setPayments(payments);

                // Calculate total payments and balance
                BigDecimal totalPayments = calculateTotalPayments(payments);
                BigDecimal balance = calculateBalance(user.getStandPrice(), totalPayments);

                reqRes.setOurUsers(user);
                reqRes.setTotalPayments(totalPayments);
                reqRes.setStandPrice(user.getStandPrice());
                reqRes.setBalance(balance);
                reqRes.setStatusCode(200);
                reqRes.setMessage("Payment added successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for adding payment");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while adding payment: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes updatePayment(Integer paymentId, Payment updatedPayment) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<Payment> paymentOptional = paymentRepository.findById(paymentId);
            if (paymentOptional.isPresent()) {
                Payment existingPayment = paymentOptional.get();
                existingPayment.setAmount(updatedPayment.getAmount());
                paymentRepository.save(existingPayment);

                // Fetch the updated payments
                OurUsers user = existingPayment.getUser();
                List<Payment> payments = paymentRepository.findByUserId(user.getId());
                user.setPayments(payments);

                // Calculate total payments and balance
                BigDecimal totalPayments = calculateTotalPayments(payments);
                BigDecimal balance = calculateBalance(user.getStandPrice(), totalPayments);

                reqRes.setOurUsers(user);
                reqRes.setTotalPayments(totalPayments);
                reqRes.setStandPrice(user.getStandPrice());
                reqRes.setBalance(balance);
                reqRes.setStatusCode(200);
                reqRes.setMessage("Payment updated successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("Payment not found for update");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while updating payment: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes getPaymentsForUser(Integer userId) {
        ReqRes reqRes = new ReqRes();
        try {
            Optional<OurUsers> userOptional = usersRepo.findById(userId);
            if (userOptional.isPresent()) {
                OurUsers user = userOptional.get();
                List<Payment> payments = paymentRepository.findByUserId(userId);
                user.setPayments(payments);

                // Calculate total payments and balance
                BigDecimal totalPayments = calculateTotalPayments(payments);
                BigDecimal balance = calculateBalance(user.getStandPrice(), totalPayments);

                reqRes.setOurUsers(user);
                reqRes.setTotalPayments(totalPayments);
                reqRes.setStandPrice(user.getStandPrice());
                reqRes.setBalance(balance);
                reqRes.setStatusCode(200);
                reqRes.setMessage("Payments fetched successfully");
            } else {
                reqRes.setStatusCode(404);
                reqRes.setMessage("User not found for fetching payments");
            }
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while fetching payments: " + e.getMessage());
        }
        return reqRes;
    }

    public ReqRes getMyInfo(String userEmail) {
        ReqRes reqRes = new ReqRes();
        try {
            OurUsers user = usersRepo.findByEmail(userEmail).orElseThrow();

            // Fetch the payments for the user
            List<Payment> payments = paymentRepository.findByUserId(user.getId());
            user.setPayments(payments);

            // Calculate total payments and balance
            BigDecimal totalPayments = calculateTotalPayments(payments);
            BigDecimal balance = calculateBalance(user.getStandPrice(), totalPayments);

            // Set the calculated fields in the user object
            user.setTotalPayments(totalPayments);
            user.setBalance(balance);

            // Set the user object in the response
            reqRes.setOurUsers(user);
            reqRes.setStatusCode(200);
            reqRes.setMessage("User profile fetched successfully");
        } catch (Exception e) {
            reqRes.setStatusCode(500);
            reqRes.setMessage("Error occurred while fetching user profile: " + e.getMessage());
        }
        return reqRes;
    }

}
