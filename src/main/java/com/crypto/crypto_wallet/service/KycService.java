package com.crypto.crypto_wallet.service;

import com.crypto.crypto_wallet.dto.KycRequest;
import com.crypto.crypto_wallet.dto.KycResponse;
import org.springframework.web.multipart.MultipartFile;

public interface KycService {
    KycResponse submitKyc(Long userId, KycRequest request,
                          MultipartFile frontImage,
                          MultipartFile backImage,
                          MultipartFile selfieImage);
    KycResponse getKycStatus(Long userId);
    java.util.List<KycResponse> getPendingKyc();
    KycResponse approveKyc(Long kycId);
    KycResponse rejectKyc(Long kycId, String reason);
}
