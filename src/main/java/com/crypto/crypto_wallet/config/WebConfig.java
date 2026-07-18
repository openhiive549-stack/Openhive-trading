package com.crypto.crypto_wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.kyc.upload-dir:uploads/kyc}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDirObj = Paths.get(uploadDir);
        String uploadPath = uploadDirObj.toFile().getAbsolutePath();
        
        // Standardize separators to forward slashes for URL compatibility
        uploadPath = uploadPath.replace("\\", "/");
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }
        if (!uploadPath.startsWith("/")) {
            uploadPath = "/" + uploadPath;
        }
        
        registry.addResourceHandler("/uploads/kyc/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
