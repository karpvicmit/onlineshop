package com.karpenko.onlineshop.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility class for resolving localized messages from MessageSource.
 * Uses the current request locale from LocaleContextHolder.
 */
@Component
public class MessageUtil {

    private final MessageSource messageSource;

    public MessageUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Gets a localized message using the current request locale.
     *
     * @param code message key (e.g., "admin.products.save.success")
     * @param args optional arguments for placeholders (e.g., {0}, {1})
     * @return localized message string
     */
    public String get(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * Gets a localized message using the current request locale with a default fallback.
     *
     * @param code         message key
     * @param defaultMessage fallback if key not found
     * @param args         optional arguments
     * @return localized message string
     */
    public String getOrDefault(String code, String defaultMessage, Object... args) {
        return messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * Gets a localized message for a specific locale.
     *
     * @param code   message key
     * @param locale target locale
     * @param args   optional arguments
     * @return localized message string
     */
    public String getForLocale(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }
}