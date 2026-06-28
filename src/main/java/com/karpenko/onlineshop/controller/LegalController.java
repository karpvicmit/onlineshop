package com.karpenko.onlineshop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class LegalController {

    @GetMapping("/impressum")
    public String impressum() {
        log.debug("Serving Impressum page");
        return "legal/impressum";
    }

    @GetMapping("/agb")
    public String agb() {
        log.debug("Serving AGB page");
        return "legal/agb";
    }

    @GetMapping("/datenschutz")
    public String datenschutz() {
        log.debug("Serving Datenschutz page");
        return "legal/datenschutz";
    }

    @GetMapping("/widerruf")
    public String widerruf() {
        log.debug("Serving Widerruf page");
        return "legal/widerruf";
    }
}