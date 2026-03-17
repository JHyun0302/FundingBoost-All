package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.BAD_REQUEST_PARAMETER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_RESOURCE;

import java.util.List;
import java.util.Objects;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.admin.AdminSupportFaqUpsertRequestDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.support.MyPageSupportFaqDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.support.MyPageSupportFaqListDto;
import kcs.funding.fundingboost.domain.entity.SupportFaq;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.SupportFaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportFaqService {

    private final SupportFaqRepository supportFaqRepository;

    @Transactional
    public MyPageSupportFaqListDto getFaqs() {
        List<MyPageSupportFaqDto> faqs = normalizeSortOrders().stream()
                .map(MyPageSupportFaqDto::fromEntity)
                .toList();
        return MyPageSupportFaqListDto.fromEntity(faqs);
    }

    @Transactional
    public MyPageSupportFaqDto createFaq(AdminSupportFaqUpsertRequestDto request) {
        List<SupportFaq> orderedFaqs = normalizeSortOrders();
        int sortOrder = resolveSortOrder(request.sortOrder(), true, null, orderedFaqs.size());

        for (SupportFaq existingFaq : orderedFaqs) {
            if (existingFaq.getSortOrder() >= sortOrder) {
                existingFaq.update(
                        existingFaq.getQuestion(),
                        existingFaq.getAnswer(),
                        existingFaq.getSortOrder() + 1
                );
            }
        }
        supportFaqRepository.saveAll(orderedFaqs);

        SupportFaq faq = SupportFaq.createFaq(
                request.question().trim(),
                request.answer().trim(),
                sortOrder
        );
        SupportFaq savedFaq = supportFaqRepository.save(faq);
        return MyPageSupportFaqDto.fromEntity(savedFaq);
    }

    @Transactional
    public MyPageSupportFaqDto updateFaq(Long faqId, AdminSupportFaqUpsertRequestDto request) {
        List<SupportFaq> orderedFaqs = normalizeSortOrders();
        SupportFaq faq = orderedFaqs.stream()
                .filter(existingFaq -> Objects.equals(existingFaq.getFaqId(), faqId))
                .findFirst()
                .orElseThrow(() -> new CommonException(NOT_FOUND_RESOURCE));

        int currentSortOrder = faq.getSortOrder();
        int sortOrder = resolveSortOrder(request.sortOrder(), false, currentSortOrder, orderedFaqs.size());

        if (sortOrder < currentSortOrder) {
            for (SupportFaq existingFaq : orderedFaqs) {
                if (Objects.equals(existingFaq.getFaqId(), faqId)) {
                    continue;
                }
                if (existingFaq.getSortOrder() >= sortOrder && existingFaq.getSortOrder() < currentSortOrder) {
                    existingFaq.update(
                            existingFaq.getQuestion(),
                            existingFaq.getAnswer(),
                            existingFaq.getSortOrder() + 1
                    );
                }
            }
        } else if (sortOrder > currentSortOrder) {
            for (SupportFaq existingFaq : orderedFaqs) {
                if (Objects.equals(existingFaq.getFaqId(), faqId)) {
                    continue;
                }
                if (existingFaq.getSortOrder() > currentSortOrder && existingFaq.getSortOrder() <= sortOrder) {
                    existingFaq.update(
                            existingFaq.getQuestion(),
                            existingFaq.getAnswer(),
                            existingFaq.getSortOrder() - 1
                    );
                }
            }
        }
        supportFaqRepository.saveAll(orderedFaqs);

        faq.update(
                request.question().trim(),
                request.answer().trim(),
                sortOrder
        );
        return MyPageSupportFaqDto.fromEntity(faq);
    }

    @Transactional
    public CommonSuccessDto deleteFaq(Long faqId) {
        List<SupportFaq> orderedFaqs = normalizeSortOrders();
        SupportFaq faq = orderedFaqs.stream()
                .filter(existingFaq -> Objects.equals(existingFaq.getFaqId(), faqId))
                .findFirst()
                .orElseThrow(() -> new CommonException(NOT_FOUND_RESOURCE));

        int deletedSortOrder = faq.getSortOrder();
        supportFaqRepository.delete(faq);

        List<SupportFaq> shiftedFaqs = orderedFaqs.stream()
                .filter(existingFaq -> !Objects.equals(existingFaq.getFaqId(), faqId))
                .toList();

        for (SupportFaq existingFaq : shiftedFaqs) {
            if (existingFaq.getSortOrder() > deletedSortOrder) {
                existingFaq.update(
                        existingFaq.getQuestion(),
                        existingFaq.getAnswer(),
                        existingFaq.getSortOrder() - 1
                );
            }
        }
        supportFaqRepository.saveAll(shiftedFaqs);

        return CommonSuccessDto.fromEntity(true);
    }

    private int resolveSortOrder(
            Integer requestedSortOrder,
            boolean isCreate,
            Integer fallbackSortOrder,
            int faqCount
    ) {
        long maxAllowedLong = isCreate ? faqCount + 1L : faqCount;
        long normalizedMaxAllowed = Math.max(maxAllowedLong, 1L);

        int resolvedSortOrder;
        if (requestedSortOrder == null) {
            if (fallbackSortOrder != null) {
                resolvedSortOrder = fallbackSortOrder;
            } else {
                resolvedSortOrder = (int) Math.min(normalizedMaxAllowed, Integer.MAX_VALUE);
            }
        } else {
            resolvedSortOrder = requestedSortOrder;
        }

        if (resolvedSortOrder < 1 || resolvedSortOrder > normalizedMaxAllowed) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        return resolvedSortOrder;
    }

    private List<SupportFaq> normalizeSortOrders() {
        List<SupportFaq> orderedFaqs = supportFaqRepository.findAllByOrderBySortOrderAscCreatedDateDesc();
        boolean changed = false;

        for (int index = 0; index < orderedFaqs.size(); index++) {
            SupportFaq faq = orderedFaqs.get(index);
            int normalizedSortOrder = index + 1;
            if (faq.getSortOrder() != normalizedSortOrder) {
                faq.update(
                        faq.getQuestion(),
                        faq.getAnswer(),
                        normalizedSortOrder
                );
                changed = true;
            }
        }

        if (changed) {
            supportFaqRepository.saveAll(orderedFaqs);
        }

        return orderedFaqs;
    }
}
