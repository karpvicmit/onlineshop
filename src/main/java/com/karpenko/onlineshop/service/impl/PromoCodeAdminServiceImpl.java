package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.promo.PromoCodeDto;
import com.karpenko.onlineshop.entity.PromoCode;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.mapper.PromoCodeMapper;
import com.karpenko.onlineshop.repository.PromoCodeRepository;
import com.karpenko.onlineshop.service.PromoCodeAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeAdminServiceImpl implements PromoCodeAdminService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeMapper promoCodeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PromoCodeDto> getAllPromoCodes() {
        log.debug("Fetching all promo codes");
        return promoCodeRepository.findAll().stream()
                .map(promoCodeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromoCodeDto getPromoCodeById(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
        return promoCodeMapper.toDto(promoCode);
    }

    @Override
    @Transactional
    public PromoCode savePromoCode(PromoCode promoCode) {
        log.info("Saving promo code: {}", promoCode.getCode());

        if (promoCode.getCode() != null) {
            promoCode.setCode(promoCode.getCode().trim().toUpperCase());
        }

        if (promoCode.getValidFrom() == null) {
            promoCode.setValidFrom(LocalDateTime.now());
        }

        if (promoCode.getUsedCount() == null) {
            promoCode.setUsedCount(0);
        }

        PromoCode saved = promoCodeRepository.save(promoCode);
        log.info("Promo code saved with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deletePromoCode(Long id) {
        log.info("Deleting promo code with ID: {}", id);
        if (!promoCodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Promo code not found");
        }
        promoCodeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));
        promoCode.setActive(!Boolean.TRUE.equals(promoCode.getActive()));
        promoCodeRepository.save(promoCode);
        log.info("Promo code {} active status toggled to {}",
                promoCode.getCode(), promoCode.getActive());
    }
}