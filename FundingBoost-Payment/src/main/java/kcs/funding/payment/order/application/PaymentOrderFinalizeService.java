package kcs.funding.payment.order.application;

import static kcs.funding.payment.exception.ErrorCode.BAD_REQUEST_PARAMETER;
import static kcs.funding.payment.exception.ErrorCode.INVALID_FUNDINGITEM_STATUS;
import static kcs.funding.payment.exception.ErrorCode.INVALID_ITEM_QUANTITY;
import static kcs.funding.payment.exception.ErrorCode.INVALID_POINT_LACK;
import static kcs.funding.payment.exception.ErrorCode.NOT_FOUND_DELIVERY;
import static kcs.funding.payment.exception.ErrorCode.NOT_FOUND_FUNDING_ITEM;
import static kcs.funding.payment.exception.ErrorCode.NOT_FOUND_ITEM;
import static kcs.funding.payment.exception.ErrorCode.NOT_FOUND_MEMBER;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kcs.funding.payment.exception.CommonException;
import kcs.funding.payment.friend.domain.PaymentCatalogItem;
import kcs.funding.payment.friend.domain.PaymentCatalogItemRepository;
import kcs.funding.payment.friend.domain.PaymentFunding;
import kcs.funding.payment.friend.domain.PaymentFundingItem;
import kcs.funding.payment.friend.domain.PaymentFundingItemRepository;
import kcs.funding.payment.friend.domain.PaymentMember;
import kcs.funding.payment.friend.domain.PaymentMemberRepository;
import kcs.funding.payment.nativeflow.NativeOrderFinalizeResultDto;
import kcs.funding.payment.order.domain.PaymentDelivery;
import kcs.funding.payment.order.domain.PaymentDeliveryRepository;
import kcs.funding.payment.order.domain.PaymentGiftHubItem;
import kcs.funding.payment.order.domain.PaymentGiftHubItemRepository;
import kcs.funding.payment.order.domain.PaymentOrder;
import kcs.funding.payment.order.domain.PaymentOrderItem;
import kcs.funding.payment.order.domain.PaymentOrderItemRepository;
import kcs.funding.payment.order.domain.PaymentOrderRepository;
import kcs.funding.payment.order.dto.PaymentItemPayDto;
import kcs.funding.payment.order.dto.PaymentItemPayNowDto;
import kcs.funding.payment.order.dto.PaymentMyPayDto;
import kcs.funding.payment.order.dto.PaymentPayRemainDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentOrderFinalizeService {

    private final PaymentDeliveryRepository deliveryRepository;
    private final PaymentMemberRepository memberRepository;
    private final PaymentCatalogItemRepository catalogItemRepository;
    private final PaymentOrderRepository orderRepository;
    private final PaymentOrderItemRepository orderItemRepository;
    private final PaymentGiftHubItemRepository giftHubItemRepository;
    private final PaymentFundingItemRepository fundingItemRepository;

    @Transactional
    public NativeOrderFinalizeResultDto finalizeOrderNow(
            Long memberId,
            PaymentItemPayNowDto itemPayNowDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        return orderRepository.findByPaymentIntentKey(paymentIntentKey)
                .map(this::toFinalizeResult)
                .orElseGet(() -> createOrderNow(
                        memberId,
                        itemPayNowDto,
                        paymentIntentKey,
                        totalAmount,
                        pointAmount,
                        pgAmount,
                        fundingSupportedAmount
                ));
    }

    @Transactional
    public NativeOrderFinalizeResultDto finalizeOrder(
            Long memberId,
            PaymentMyPayDto myPayDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        return orderRepository.findByPaymentIntentKey(paymentIntentKey)
                .map(this::toFinalizeResult)
                .orElseGet(() -> createOrder(
                        memberId,
                        myPayDto,
                        paymentIntentKey,
                        totalAmount,
                        pointAmount,
                        pgAmount,
                        fundingSupportedAmount
                ));
    }

    @Transactional
    public NativeOrderFinalizeResultDto finalizeFunding(
            Long memberId,
            Long fundingItemId,
            PaymentPayRemainDto payRemainDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        return orderRepository.findByPaymentIntentKey(paymentIntentKey)
                .map(this::toFinalizeResult)
                .orElseGet(() -> createFundingOrder(
                        memberId,
                        fundingItemId,
                        payRemainDto,
                        paymentIntentKey,
                        totalAmount,
                        pointAmount,
                        pgAmount,
                        fundingSupportedAmount
                ));
    }

    private NativeOrderFinalizeResultDto createOrderNow(
            Long memberId,
            PaymentItemPayNowDto itemPayNowDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        PaymentMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        PaymentDelivery delivery = deliveryRepository.findById(itemPayNowDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (itemPayNowDto.quantity() <= 0) {
            throw new CommonException(INVALID_ITEM_QUANTITY);
        }
        PaymentCatalogItem item = catalogItemRepository.findById(itemPayNowDto.itemId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));

        PaymentOrder order = PaymentOrder.createOrder(member, delivery);
        PaymentOrderItem orderItem = PaymentOrderItem.createOrderItem(
                order,
                item,
                itemPayNowDto.quantity(),
                resolveOptionName(itemPayNowDto.optionName(), null, item.getOptionName())
        );
        validatePaymentBreakdown(order, totalAmount, pointAmount, pgAmount, fundingSupportedAmount);

        deductPointsIfPossible(member, pointAmount);
        order.applyPaymentBreakdown(pointAmount, pgAmount, fundingSupportedAmount, null);
        order.linkPaymentIntentKey(paymentIntentKey);
        orderRepository.save(order);
        orderItemRepository.save(orderItem);
        return toFinalizeResult(order);
    }

    private NativeOrderFinalizeResultDto createOrder(
            Long memberId,
            PaymentMyPayDto myPayDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        PaymentMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        PaymentDelivery delivery = deliveryRepository.findById(myPayDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (myPayDto.itemPayDtoList() == null || myPayDto.itemPayDtoList().isEmpty()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        List<Long> itemIds = myPayDto.itemPayDtoList().stream()
                .map(PaymentItemPayDto::itemId)
                .toList();
        Map<Long, PaymentCatalogItem> itemMap = catalogItemRepository.findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(PaymentCatalogItem::getItemId, item -> item));
        Map<Long, PaymentGiftHubItem> giftHubItemMap = extractValidatedGiftHubItems(myPayDto.itemPayDtoList(), memberId);

        PaymentOrder order = PaymentOrder.createOrder(member, delivery);
        List<PaymentOrderItem> orderItems = myPayDto.itemPayDtoList().stream()
                .map(itemPayDto -> buildOrderItem(order, itemMap, giftHubItemMap, itemPayDto))
                .toList();

        validatePaymentBreakdown(order, totalAmount, pointAmount, pgAmount, fundingSupportedAmount);

        deductPointsIfPossible(member, pointAmount);
        order.applyPaymentBreakdown(pointAmount, pgAmount, fundingSupportedAmount, null);
        order.linkPaymentIntentKey(paymentIntentKey);

        if (!giftHubItemMap.isEmpty()) {
            giftHubItemRepository.deleteAllById(giftHubItemMap.keySet());
        }
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        return toFinalizeResult(order);
    }

    private NativeOrderFinalizeResultDto createFundingOrder(
            Long memberId,
            Long fundingItemId,
            PaymentPayRemainDto payRemainDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        PaymentFundingItem fundingItem = fundingItemRepository.findFundingItemByFundingItemId(fundingItemId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING_ITEM));
        PaymentMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        PaymentDelivery delivery = deliveryRepository.findById(payRemainDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (!Objects.equals(fundingItem.getFunding().getMember().getMemberId(), memberId)) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (!fundingItem.isFinishedStatus()) {
            throw new CommonException(INVALID_FUNDINGITEM_STATUS);
        }

        int requestedUsingPoint = sanitizeUsingPoint(payRemainDto.usingPoint());
        Long itemReferenceId = fundingItem.getItemReferenceId();
        if (itemReferenceId == null) {
            throw new CommonException(NOT_FOUND_ITEM);
        }
        PaymentCatalogItem item = catalogItemRepository.findById(itemReferenceId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));

        PaymentOrder order = PaymentOrder.createOrder(member, delivery);
        PaymentOrderItem orderItem = PaymentOrderItem.createOrderItem(order, item, 1, resolveOptionName(null, null, item.getOptionName()));

        int expectedFundingSupportedAmount = Math.min(fundingItem.getFunding().getCollectPrice(), order.getTotalPrice());
        int payableAfterFundingAmount = Math.max(order.getTotalPrice() - expectedFundingSupportedAmount, 0);
        int expectedPointAmount = resolveApplicablePoint(member, requestedUsingPoint, payableAfterFundingAmount);
        validateFundingPaymentBreakdown(
                order,
                totalAmount,
                pointAmount,
                pgAmount,
                fundingSupportedAmount,
                expectedPointAmount,
                expectedFundingSupportedAmount
        );

        fundingItem.finishFundingItem();
        updateFundingFinishedStatus(fundingItem.getFunding());
        deductPointsIfPossible(member, pointAmount);
        order.applyPaymentBreakdown(pointAmount, pgAmount, fundingSupportedAmount, fundingItem.getFunding().getFundingId());
        order.linkPaymentIntentKey(paymentIntentKey);
        orderRepository.save(order);
        orderItemRepository.save(orderItem);
        return toFinalizeResult(order);
    }

    private PaymentOrderItem buildOrderItem(
            PaymentOrder order,
            Map<Long, PaymentCatalogItem> itemMap,
            Map<Long, PaymentGiftHubItem> giftHubItemMap,
            PaymentItemPayDto itemPayDto
    ) {
        PaymentCatalogItem item = itemMap.get(itemPayDto.itemId());
        if (item == null) {
            throw new CommonException(NOT_FOUND_ITEM);
        }
        if (itemPayDto.quantity() <= 0) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        PaymentGiftHubItem giftHubItem = null;
        if (itemPayDto.giftHubId() != null) {
            giftHubItem = giftHubItemMap.get(itemPayDto.giftHubId());
            if (giftHubItem == null) {
                throw new CommonException(BAD_REQUEST_PARAMETER);
            }
            if (!Objects.equals(giftHubItem.getItemReferenceId(), itemPayDto.itemId())) {
                throw new CommonException(BAD_REQUEST_PARAMETER);
            }
        }
        return PaymentOrderItem.createOrderItem(
                order,
                item,
                itemPayDto.quantity(),
                resolveOptionName(
                        itemPayDto.optionName(),
                        giftHubItem != null ? giftHubItem.getOptionName() : null,
                        item.getOptionName()
                )
        );
    }

    private void validatePaymentBreakdown(PaymentOrder order, int totalAmount, int pointAmount, int pgAmount, int fundingSupportedAmount) {
        if (fundingSupportedAmount != 0) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (order.getTotalPrice() != totalAmount) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (pointAmount < 0 || pgAmount < 0) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (pointAmount + pgAmount != totalAmount) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
    }

    private void validateFundingPaymentBreakdown(
            PaymentOrder order,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            int expectedPointAmount,
            int expectedFundingSupportedAmount
    ) {
        if (order.getTotalPrice() != totalAmount) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (pointAmount < 0 || pgAmount < 0 || fundingSupportedAmount < 0) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (pointAmount != expectedPointAmount) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (fundingSupportedAmount != expectedFundingSupportedAmount) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (pointAmount + pgAmount + fundingSupportedAmount != totalAmount) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
    }

    private void validateDeliveryOwnership(PaymentMember member, PaymentDelivery delivery) {
        if (!Objects.equals(delivery.getMember().getMemberId(), member.getMemberId())) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
    }

    private int sanitizeUsingPoint(int requestedPoint) {
        if (requestedPoint < 0) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        return requestedPoint;
    }

    private int resolveApplicablePoint(PaymentMember member, int requestedPoint, int payableAmount) {
        int safePayableAmount = Math.max(payableAmount, 0);
        int safeMemberPoint = Math.max(member.getPoint(), 0);
        return Math.min(Math.min(requestedPoint, safeMemberPoint), safePayableAmount);
    }

    private Map<Long, PaymentGiftHubItem> extractValidatedGiftHubItems(List<PaymentItemPayDto> itemPayDtoList, Long memberId) {
        List<Long> giftHubItemIds = itemPayDtoList.stream()
                .map(PaymentItemPayDto::giftHubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (giftHubItemIds.isEmpty()) {
            return Map.of();
        }
        long ownedCount = giftHubItemRepository.countByGiftHubItemIdInAndMember_MemberId(giftHubItemIds, memberId);
        if (ownedCount != giftHubItemIds.size()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        List<PaymentGiftHubItem> giftHubItems = giftHubItemRepository.findAllById(giftHubItemIds);
        if (giftHubItems.size() != giftHubItemIds.size()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        return giftHubItems.stream().collect(Collectors.toMap(PaymentGiftHubItem::getGiftHubItemId, giftHubItem -> giftHubItem));
    }

    private String resolveOptionName(String requestOptionName, String giftHubOptionName, String itemOptionName) {
        String normalizedGiftHubOption = normalizeOptionName(giftHubOptionName);
        if (normalizedGiftHubOption != null) {
            return normalizedGiftHubOption;
        }
        String normalizedRequestOption = normalizeOptionName(requestOptionName);
        if (normalizedRequestOption != null) {
            return normalizedRequestOption;
        }
        String normalizedItemOption = normalizeOptionName(itemOptionName);
        if (normalizedItemOption != null) {
            return normalizedItemOption;
        }
        return "기본 옵션";
    }

    private String normalizeOptionName(String optionName) {
        if (optionName == null) {
            return null;
        }
        String trimmed = optionName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private void deductPointsIfPossible(PaymentMember member, int points) {
        if (points == 0) {
            return;
        }
        if (member.getPoint() - points < 0) {
            throw new CommonException(INVALID_POINT_LACK);
        }
        member.minusPoint(points);
    }

    private void updateFundingFinishedStatus(PaymentFunding funding) {
        List<PaymentFundingItem> fundingItems = fundingItemRepository.findAllByFundingIdOrderByItemSequence(funding.getFundingId());
        boolean hasFinishedItem = fundingItems.stream().anyMatch(PaymentFundingItem::isFinishedStatus);
        if (!hasFinishedItem) {
            funding.finish();
        }
    }

    private NativeOrderFinalizeResultDto toFinalizeResult(PaymentOrder order) {
        return new NativeOrderFinalizeResultDto(
                order.getOrderId(),
                order.getMember().getMemberId(),
                order.getPaymentIntentKey(),
                order.getTotalPrice(),
                order.getPointUsedAmount(),
                order.getDirectPaidAmount(),
                order.getFundingSupportedAmount(),
                order.getSourceFundingId()
        );
    }
}
