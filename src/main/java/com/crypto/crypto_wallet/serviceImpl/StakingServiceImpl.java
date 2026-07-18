package com.crypto.crypto_wallet.serviceImpl;

import com.crypto.crypto_wallet.dto.StakeRequest;
import com.crypto.crypto_wallet.dto.StakeResponse;
import com.crypto.crypto_wallet.entity.*;
import com.crypto.crypto_wallet.exception.BadRequestException;
import com.crypto.crypto_wallet.exception.InsufficientBalanceException;
import com.crypto.crypto_wallet.exception.ResourceNotFoundException;
import com.crypto.crypto_wallet.repository.StakeRepository;
import com.crypto.crypto_wallet.repository.UserRepository;
import com.crypto.crypto_wallet.repository.WalletRepository;
import com.crypto.crypto_wallet.service.StakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StakingServiceImpl implements StakingService {

    private final StakeRepository stakeRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public StakeResponse stake(Long userId, StakeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserIdAndCoinSymbol(userId, request.getCoinSymbol())
                .orElseThrow(() -> new InsufficientBalanceException(
                        "No " + request.getCoinSymbol() + " wallet found"));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient " + request.getCoinSymbol() + " balance for staking");
        }

        // Lock the amount
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = startDate.plusDays(request.getDurationDays());

        Stake stake = Stake.builder()
                .user(user)
                .coinSymbol(request.getCoinSymbol().toUpperCase())
                .amount(request.getAmount())
                .apr(request.getApr())
                .durationDays(request.getDurationDays())
                .startDate(startDate)
                .endDate(endDate)
                .status(StakeStatus.ACTIVE)
                .build();

        stakeRepository.save(stake);
        return toResponse(stake);
    }

    @Override
    public List<StakeResponse> getMyStakes(Long userId) {
        return stakeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StakeResponse cancelStake(Long userId, Long stakeId) {
        Stake stake = stakeRepository.findById(stakeId)
                .orElseThrow(() -> new ResourceNotFoundException("Stake not found"));

        if (!stake.getUser().getId().equals(userId)) {
            throw new BadRequestException("Stake does not belong to this user");
        }
        if (stake.getStatus() != StakeStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE stakes can be cancelled");
        }

        // Return the locked amount
        Wallet wallet = walletRepository.findByUserIdAndCoinSymbol(userId, stake.getCoinSymbol())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setBalance(wallet.getBalance().add(stake.getAmount()));
        walletRepository.save(wallet);

        stake.setStatus(StakeStatus.CANCELLED);
        stakeRepository.save(stake);
        return toResponse(stake);
    }

    private StakeResponse toResponse(Stake s) {
        return StakeResponse.builder()
                .id(s.getId())
                .coinSymbol(s.getCoinSymbol())
                .amount(s.getAmount())
                .apr(s.getApr())
                .durationDays(s.getDurationDays())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus())
                .build();
    }
}
