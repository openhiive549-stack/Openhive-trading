package com.crypto.crypto_wallet.controller;

import com.crypto.crypto_wallet.dto.ApiResponse;
import com.crypto.crypto_wallet.dto.KycRequest;
import com.crypto.crypto_wallet.dto.KycResponse;
import com.crypto.crypto_wallet.entity.DocumentType;
import com.crypto.crypto_wallet.service.KycService;
import com.crypto.crypto_wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    private final UserService userService;

    /**
     * Submit KYC — multipart/form-data with JSON fields + 3 image files
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycResponse>> submitKyc(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String dateOfBirth,
            @RequestParam String country,
            @RequestParam String address,
            @RequestParam String documentType,
            @RequestParam String documentNumber,
            @RequestParam(value = "frontImage",  required = false) MultipartFile frontImage,
            @RequestParam(value = "backImage",   required = false) MultipartFile backImage,
            @RequestParam(value = "selfieImage", required = false) MultipartFile selfieImage) {

        Long userId = userService.getByEmail(userDetails.getUsername()).getId();

        KycRequest request = new KycRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setDateOfBirth(dateOfBirth);
        request.setCountry(country);
        request.setAddress(address);
        request.setDocumentType(DocumentType.valueOf(documentType.toUpperCase()));
        request.setDocumentNumber(documentNumber);

        KycResponse response = kycService.submitKyc(userId, request, frontImage, backImage, selfieImage);
        return ResponseEntity.ok(ApiResponse.ok("KYC submitted successfully", response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<KycResponse>> getStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ApiResponse.ok(kycService.getKycStatus(userId)));
    }
}
