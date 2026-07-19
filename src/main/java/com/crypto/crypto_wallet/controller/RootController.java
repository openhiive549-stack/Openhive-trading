package com.crypto.crypto_wallet.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles the root URL "/".
 * Any visitor who types just the domain (e.g. openhive.com) is
 * redirected to the public landing page.
 */
@Controller
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/home/landing_page";
    }
}
