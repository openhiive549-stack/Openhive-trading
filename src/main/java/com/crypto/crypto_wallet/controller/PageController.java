package com.crypto.crypto_wallet.controller;


import com.crypto.crypto_wallet.dto.DashboardResponse;
import com.crypto.crypto_wallet.dto.TransactionResponse;
import com.crypto.crypto_wallet.dto.UserResponse;
import com.crypto.crypto_wallet.repository.TransactionRepository;
import com.crypto.crypto_wallet.service.AuthService;
import com.crypto.crypto_wallet.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/home")
@RequiredArgsConstructor
public class PageController {

    private final AuthService authService;
    private final DashboardService dashboardService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/landing_page")
    public String landingPage() {
        return "landing";
    }

    @GetMapping("/signin")
    public String signinPage() {
        return "signin";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }



    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            DashboardResponse dashboardResponse = dashboardService.getDashboard(currentUser.getId());
            model.addAttribute("dashboard", dashboardResponse);
            model.addAttribute("user", currentUser);
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "dashboard";
    }

    @GetMapping("/market")
    public String marketPage() {
        return "market";
    }

    @GetMapping("/trade")
    public String tradePage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            DashboardResponse dashboardResponse = dashboardService.getDashboard(currentUser.getId());
            model.addAttribute("dashboard", dashboardResponse);
            model.addAttribute("user", currentUser);
            // KYC check commented out - not needed now
            // model.addAttribute("kycVerified",
            //     currentUser.getKycStatus() != null &&
            //     currentUser.getKycStatus().name().equals("VERIFIED"));
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "trade";
    }

//    @GetMapping("/trade-history")
//    public String tradeHistoryPage() {
//        return "trade-history";
//    }

    @GetMapping("/assets")
    public String assetsPage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            DashboardResponse dashboardResponse = dashboardService.getDashboard(currentUser.getId());
            model.addAttribute("dashboard", dashboardResponse);
            model.addAttribute("user", currentUser);
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "assets";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            DashboardResponse dashboardResponse = dashboardService.getDashboard(currentUser.getId());
            model.addAttribute("user", currentUser);
            model.addAttribute("dashboard", dashboardResponse);
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "profile";
    }

    @GetMapping("/deposit")
    public String depositPage() {
        return "deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage() {
        return "withdraw";
    }

    @GetMapping("/staking")
    public String stakingPage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            DashboardResponse dashboardResponse = dashboardService.getDashboard(currentUser.getId());
            model.addAttribute("dashboard", dashboardResponse);
            model.addAttribute("user", currentUser);
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "staking";
    }

    @GetMapping("/setting")
    public String kycPage() {
        return "setting";
    }

    @GetMapping("/history")
    public String historyPage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            model.addAttribute("user", currentUser);
            // Inject ALL transactions (no limit)
            java.util.List<TransactionResponse> allTx = transactionRepository
                    .findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                    .stream()
                    .map(tx -> TransactionResponse.builder()
                            .id(tx.getId())
                            .type(tx.getType())
                            .coinSymbol(tx.getCoinSymbol())
                            .amount(tx.getAmount())
                            .txHash(tx.getTxHash())
                            .toAddress(tx.getToAddress())
                            .status(tx.getStatus())
                            .createdAt(tx.getCreatedAt())
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("transactions", allTx);
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "history";
    }

    @GetMapping("/referral")
    public String referralPage() {
        return "referral";
    }

    @GetMapping("/admin")
    public String adminDashboardPage(Model model) {
        try {
            UserResponse currentUser = authService.getCurrentUser();
            if (currentUser.getRole() != com.crypto.crypto_wallet.entity.UserRole.ADMIN) {
                return "redirect:/home/dashboard";
            }
            model.addAttribute("user", currentUser);
        } catch (Exception e) {
            return "redirect:/home/signin";
        }
        return "admin-dashboard";
    }


}
