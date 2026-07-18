package com.crypto.crypto_wallet.config;

import com.crypto.crypto_wallet.entity.DepositAddress;
import com.crypto.crypto_wallet.repository.DepositAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DepositAddressRepository depositAddressRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed Deposit Addresses if the table is empty
        if (depositAddressRepository.count() == 0) {
            List<DepositAddress> initialAddresses = List.of(
                DepositAddress.builder()
                        .coinSymbol("BTC")
                        .coinName("Bitcoin")
                        .network("Bitcoin Network")
                        .address("bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh")
                        .build(),
                DepositAddress.builder()
                        .coinSymbol("ETH")
                        .coinName("Ethereum")
                        .network("ERC-20")
                        .address("0x71C7656EC7ab88b098defB751B7401B5f6d8976F")
                        .build(),
                DepositAddress.builder()
                        .coinSymbol("SOL")
                        .coinName("Solana")
                        .network("Solana")
                        .address("5xot9PVkphiX2adznghwrAuxGs2zeWisNSxMW6hU6Hkj")
                        .build(),
                DepositAddress.builder()
                        .coinSymbol("BNB")
                        .coinName("BNB")
                        .network("BEP-20")
                        .address("bnb1grpf0955h0ykzq3ar5nmum7y6gdfl6lxfn46h2")
                        .build(),
                DepositAddress.builder()
                        .coinSymbol("USDT")
                        .coinName("Tether")
                        .network("TRC-20")
                        .address("TV6MuMXfmLbBqPZvBHdwFsDnQeVfnmiuSi")
                        .build()
            );
            depositAddressRepository.saveAll(initialAddresses);
            System.out.println("✅ Initialized default deposit addresses in DB.");
        }
    }
}
