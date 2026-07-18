package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.KycRequest;
import com.crypto.crypto_wallet.dto.KycResponse;
import com.crypto.crypto_wallet.entity.*;
import com.crypto.crypto_wallet.exception.BadRequestException;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.KycDocumentRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;

    @Value("${app.kyc.upload-dir:uploads/kyc}")
    private String uploadDir;

    @Override
    @Transactional
    public KycResponse submitKyc(Long userId, KycRequest request,
                                  MultipartFile frontImage,
                                  MultipartFile backImage,
                                  MultipartFile selfieImage) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getKycStatus() == KycStatus.VERIFIED) {
            throw new BadRequestException("KYC is already verified");
        }

        String frontPath   = saveFile(frontImage,  userId, "front");
        String backPath    = saveFile(backImage,   userId, "back");
        String selfiePath  = saveFile(selfieImage, userId, "selfie");

        KycDocument doc = kycDocumentRepository.findByUserId(userId)
                .orElse(KycDocument.builder().user(user).build());

        doc.setFirstName(request.getFirstName());
        doc.setLastName(request.getLastName());
        doc.setDateOfBirth(request.getDateOfBirth());
        doc.setCountry(request.getCountry());
        doc.setAddress(request.getAddress());
        doc.setDocumentType(request.getDocumentType());
        doc.setDocumentNumber(request.getDocumentNumber());
        doc.setFrontImagePath(frontPath);
        doc.setBackImagePath(backPath);
        doc.setSelfieImagePath(selfiePath);
        doc.setStatus(KycStatus.SUBMITTED);

        kycDocumentRepository.save(doc);

        // Update user KYC status
        user.setKycStatus(KycStatus.SUBMITTED);
        userRepository.save(user);

        return toResponse(doc);
    }

    @Override
    public KycResponse getKycStatus(Long userId) {
        KycDocument doc = kycDocumentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No KYC submission found"));
        return toResponse(doc);
    }

    private String saveFile(MultipartFile file, Long userId, String suffix) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path dir = Paths.get(uploadDir, String.valueOf(userId));
            Files.createDirectories(dir);
            String filename = suffix + "_" + UUID.randomUUID() +
                    getExtension(file.getOriginalFilename());
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/kyc/" + userId + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    @Override
    public java.util.List<KycResponse> getPendingKyc() {
        return kycDocumentRepository.findAll().stream()
                .filter(doc -> doc.getStatus() == KycStatus.SUBMITTED)
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public KycResponse approveKyc(Long kycId) {
        KycDocument doc = kycDocumentRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));
        doc.setStatus(KycStatus.VERIFIED);
        doc.setReviewedAt(java.time.LocalDateTime.now());
        kycDocumentRepository.save(doc);

        User user = doc.getUser();
        user.setKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        return toResponse(doc);
    }

    @Override
    @Transactional
    public KycResponse rejectKyc(Long kycId, String reason) {
        KycDocument doc = kycDocumentRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));
        doc.setStatus(KycStatus.REJECTED);
        doc.setRejectionReason(reason);
        doc.setReviewedAt(java.time.LocalDateTime.now());
        kycDocumentRepository.save(doc);

        User user = doc.getUser();
        user.setKycStatus(KycStatus.REJECTED);
        userRepository.save(user);

        return toResponse(doc);
    }

    private KycResponse toResponse(KycDocument doc) {
        return KycResponse.builder()
                .id(doc.getId())
                .firstName(doc.getFirstName())
                .lastName(doc.getLastName())
                .dateOfBirth(doc.getDateOfBirth())
                .country(doc.getCountry())
                .address(doc.getAddress())
                .documentType(doc.getDocumentType())
                .documentNumber(doc.getDocumentNumber())
                .status(doc.getStatus())
                .rejectionReason(doc.getRejectionReason())
                .submittedAt(doc.getSubmittedAt())
                .frontImagePath(doc.getFrontImagePath())
                .backImagePath(doc.getBackImagePath())
                .selfieImagePath(doc.getSelfieImagePath())
                .build();
    }
}
