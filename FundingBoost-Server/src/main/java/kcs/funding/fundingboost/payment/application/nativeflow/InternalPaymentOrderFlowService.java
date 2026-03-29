package kcs.funding.fundingboost.payment.application.nativeflow;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.BAD_REQUEST_PARAMETER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_FUNDINGITEM_STATUS;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_ITEM_QUANTITY;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_DELIVERY;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_FUNDING_ITEM;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_ITEM;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kcs.funding.fundingboost.catalog.application.CatalogItemReader;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayNowDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.MyPayDto;
import kcs.funding.fundingboost.domain.entity.Delivery;
import kcs.funding.fundingboost.domain.entity.FundingItem;
import kcs.funding.fundingboost.domain.entity.GiftHubItem;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.entity.Order;
import kcs.funding.fundingboost.domain.entity.OrderItem;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.DeliveryRepository;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.OrderRepository;
import kcs.funding.fundingboost.domain.repository.fundingItem.FundingItemRepository;
import kcs.funding.fundingboost.domain.repository.giftHubItem.GiftHubItemRepository;
import kcs.funding.fundingboost.domain.repository.orderItem.OrderItemRepository;
import kcs.funding.fundingboost.domain.service.utils.PayUtils;
import kcs.funding.fundingboost.payment.application.PaymentIntentKeyResolver;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderFinalizeResultDto;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderPreparationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalPaymentOrderFlowService {

    private final DeliveryRepository deliveryRepository;
    private final MemberRepository memberRepository;
    private final CatalogItemReader catalogItemReader;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final GiftHubItemRepository giftHubItemRepository;
    private final FundingItemRepository fundingItemRepository;

    @Transactional(readOnly = true)
    public OrderPreparationDto prepareOrderNow(Long memberId, ItemPayNowDto itemPayNowDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(itemPayNowDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (itemPayNowDto.quantity() <= 0) {
            throw new CommonException(INVALID_ITEM_QUANTITY);
        }
        int requestedUsingPoint = sanitizeUsingPoint(itemPayNowDto.usingPoint());
        Item item = catalogItemReader.findById(itemPayNowDto.itemId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));

        Order draftOrder = Order.createOrder(member, delivery);
        OrderItem.createOrderItem(
                draftOrder,
                item,
                itemPayNowDto.quantity(),
                resolveOptionName(itemPayNowDto.optionName(), null, item.getOptionName())
        );

        int pointAmount = resolveApplicablePoint(member, requestedUsingPoint, draftOrder.getTotalPrice());
        int pgAmount = Math.max(draftOrder.getTotalPrice() - pointAmount, 0);
        return new OrderPreparationDto(
                memberId,
                itemPayNowDto.itemId(),
                "KRW",
                draftOrder.getTotalPrice(),
                pointAmount,
                pgAmount,
                0
        );
    }

    @Transactional(readOnly = true)
    public OrderPreparationDto prepareOrder(Long memberId, MyPayDto myPayDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(myPayDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (myPayDto.itemPayDtoList().isEmpty()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        int requestedUsingPoint = sanitizeUsingPoint(myPayDto.usingPoint());
        List<Long> itemIds = myPayDto.itemPayDtoList().stream()
                .map(ItemPayDto::itemId)
                .toList();
        Map<Long, Item> itemMap = catalogItemReader.findItemsByItemIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));
        Map<Long, GiftHubItem> giftHubItemMap = extractValidatedGiftHubItems(myPayDto.itemPayDtoList(), memberId);

        Order draftOrder = Order.createOrder(member, delivery);
        myPayDto.itemPayDtoList().forEach(itemPayDto -> buildOrderItem(draftOrder, itemMap, giftHubItemMap, itemPayDto));

        int pointAmount = resolveApplicablePoint(member, requestedUsingPoint, draftOrder.getTotalPrice());
        int pgAmount = Math.max(draftOrder.getTotalPrice() - pointAmount, 0);
        return new OrderPreparationDto(
                memberId,
                null,
                "KRW",
                draftOrder.getTotalPrice(),
                pointAmount,
                pgAmount,
                0
        );
    }

    @Transactional(readOnly = true)
    public OrderPreparationDto prepareFunding(Long memberId, Long fundingItemId, kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto payRemainDto, String idempotencyKey) {
        FundingItem fundingItem = findFundingItem(fundingItemId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(payRemainDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (!fundingItem.isFinishedStatus()) {
            return resolveExistingFundingPreparation(memberId, fundingItemId, idempotencyKey)
                    .orElseThrow(() -> new CommonException(INVALID_FUNDINGITEM_STATUS));
        }

        int requestedUsingPoint = sanitizeUsingPoint(payRemainDto.usingPoint());
        Item item = findCatalogItem(fundingItem);
        Order draftOrder = Order.createOrder(member, delivery);
        OrderItem.createOrderItem(draftOrder, item, 1);

        int fundingSupportedAmount = Math.min(fundingItem.getFunding().getCollectPrice(), draftOrder.getTotalPrice());
        int payableAfterFundingAmount = Math.max(draftOrder.getTotalPrice() - fundingSupportedAmount, 0);
        int pointAmount = resolveApplicablePoint(member, requestedUsingPoint, payableAfterFundingAmount);
        int pgAmount = Math.max(payableAfterFundingAmount - pointAmount, 0);
        return new OrderPreparationDto(
                memberId,
                fundingItemId,
                "KRW",
                draftOrder.getTotalPrice(),
                pointAmount,
                pgAmount,
                fundingSupportedAmount
        );
    }

    @Transactional
    public OrderFinalizeResultDto finalizeOrderNow(
            Long memberId,
            ItemPayNowDto itemPayNowDto,
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
    public OrderFinalizeResultDto finalizeOrder(
            Long memberId,
            MyPayDto myPayDto,
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
    public OrderFinalizeResultDto finalizeFunding(
            Long memberId,
            Long fundingItemId,
            kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto payRemainDto,
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

    private OrderFinalizeResultDto createOrderNow(
            Long memberId,
            ItemPayNowDto itemPayNowDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(itemPayNowDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (itemPayNowDto.quantity() <= 0) {
            throw new CommonException(INVALID_ITEM_QUANTITY);
        }
        Item item = catalogItemReader.findById(itemPayNowDto.itemId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));

        Order order = Order.createOrder(member, delivery);
        OrderItem orderItem = OrderItem.createOrderItem(
                order,
                item,
                itemPayNowDto.quantity(),
                resolveOptionName(itemPayNowDto.optionName(), null, item.getOptionName())
        );
        validatePaymentBreakdown(order, totalAmount, pointAmount, pgAmount, fundingSupportedAmount);

        PayUtils.deductPointsIfPossible(member, pointAmount);
        order.applyPaymentBreakdown(pointAmount, pgAmount, fundingSupportedAmount, null);
        order.linkPaymentIntentKey(paymentIntentKey);
        orderRepository.save(order);
        orderItemRepository.save(orderItem);
        return toFinalizeResult(order);
    }

    private OrderFinalizeResultDto createOrder(
            Long memberId,
            MyPayDto myPayDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(myPayDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (myPayDto.itemPayDtoList().isEmpty()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        List<Long> itemIds = myPayDto.itemPayDtoList().stream()
                .map(ItemPayDto::itemId)
                .toList();
        Map<Long, Item> itemMap = catalogItemReader.findItemsByItemIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));
        Map<Long, GiftHubItem> giftHubItemMap = extractValidatedGiftHubItems(myPayDto.itemPayDtoList(), memberId);

        Order order = Order.createOrder(member, delivery);
        List<OrderItem> orderItems = myPayDto.itemPayDtoList().stream()
                .map(itemPayDto -> buildOrderItem(order, itemMap, giftHubItemMap, itemPayDto))
                .toList();

        validatePaymentBreakdown(order, totalAmount, pointAmount, pgAmount, fundingSupportedAmount);

        PayUtils.deductPointsIfPossible(member, pointAmount);
        order.applyPaymentBreakdown(pointAmount, pgAmount, fundingSupportedAmount, null);
        order.linkPaymentIntentKey(paymentIntentKey);

        if (!giftHubItemMap.isEmpty()) {
            giftHubItemRepository.deleteAllById(giftHubItemMap.keySet());
        }
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        return toFinalizeResult(order);
    }

    private OrderFinalizeResultDto createFundingOrder(
            Long memberId,
            Long fundingItemId,
            kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto payRemainDto,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        FundingItem fundingItem = findFundingItem(fundingItemId);
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(payRemainDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));
        validateDeliveryOwnership(member, delivery);

        if (!Objects.equals(fundingItem.getFunding().getMember().getMemberId(), memberId)) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        if (!fundingItem.isFinishedStatus()) {
            throw new CommonException(INVALID_FUNDINGITEM_STATUS);
        }

        int requestedUsingPoint = sanitizeUsingPoint(payRemainDto.usingPoint());
        Item item = findCatalogItem(fundingItem);

        Order order = Order.createOrder(member, delivery);
        OrderItem orderItem = OrderItem.createOrderItem(order, item, 1);

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
        PayUtils.deductPointsIfPossible(member, pointAmount);
        order.applyPaymentBreakdown(
                pointAmount,
                pgAmount,
                fundingSupportedAmount,
                fundingItem.getFunding().getFundingId()
        );
        order.linkPaymentIntentKey(paymentIntentKey);
        orderRepository.save(order);
        orderItemRepository.save(orderItem);
        return toFinalizeResult(order);
    }

    private OrderItem buildOrderItem(
            Order order,
            Map<Long, Item> itemMap,
            Map<Long, GiftHubItem> giftHubItemMap,
            ItemPayDto itemPayDto
    ) {
        Item item = itemMap.get(itemPayDto.itemId());
        if (item == null) {
            throw new CommonException(NOT_FOUND_ITEM);
        }
        if (itemPayDto.quantity() <= 0) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        GiftHubItem giftHubItem = null;
        if (itemPayDto.giftHubId() != null) {
            giftHubItem = giftHubItemMap.get(itemPayDto.giftHubId());
            if (giftHubItem == null) {
                throw new CommonException(BAD_REQUEST_PARAMETER);
            }
            if (!Objects.equals(giftHubItem.getItem().getItemId(), itemPayDto.itemId())) {
                throw new CommonException(BAD_REQUEST_PARAMETER);
            }
        }
        return OrderItem.createOrderItem(
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

    private void validatePaymentBreakdown(Order order, int totalAmount, int pointAmount, int pgAmount, int fundingSupportedAmount) {
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
            Order order,
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

    private void validateDeliveryOwnership(Member member, Delivery delivery) {
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

    private int resolveApplicablePoint(Member member, int requestedPoint, int payableAmount) {
        int safePayableAmount = Math.max(payableAmount, 0);
        int safeMemberPoint = Math.max(member.getPoint(), 0);
        return Math.min(Math.min(requestedPoint, safeMemberPoint), safePayableAmount);
    }

    private FundingItem findFundingItem(Long fundingItemId) {
        return fundingItemRepository.findFundingItemByFundingItemId(fundingItemId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING_ITEM));
    }

    private Item findCatalogItem(FundingItem fundingItem) {
        Long itemReferenceId = fundingItem.getItemReferenceId();
        if (itemReferenceId == null) {
            throw new CommonException(NOT_FOUND_ITEM);
        }
        return catalogItemReader.findById(itemReferenceId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));
    }

    private java.util.Optional<OrderPreparationDto> resolveExistingFundingPreparation(Long memberId, Long fundingItemId, String idempotencyKey) {
        return PaymentIntentKeyResolver.resolveFromIdempotencyKey(idempotencyKey)
                .flatMap(orderRepository::findByPaymentIntentKey)
                .filter(order -> Objects.equals(order.getMember().getMemberId(), memberId))
                .map(order -> new OrderPreparationDto(
                        memberId,
                        fundingItemId,
                        "KRW",
                        order.getTotalPrice(),
                        order.getPointUsedAmount(),
                        order.getDirectPaidAmount(),
                        order.getFundingSupportedAmount()
                ));
    }

    private Map<Long, GiftHubItem> extractValidatedGiftHubItems(List<ItemPayDto> itemPayDtoList, Long memberId) {
        List<Long> giftHubItemIds = itemPayDtoList.stream()
                .map(ItemPayDto::giftHubId)
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
        List<GiftHubItem> giftHubItems = giftHubItemRepository.findAllById(giftHubItemIds);
        if (giftHubItems.size() != giftHubItemIds.size()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        return giftHubItems.stream().collect(Collectors.toMap(GiftHubItem::getGiftHubItemId, giftHubItem -> giftHubItem));
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

    private OrderFinalizeResultDto toFinalizeResult(Order order) {
        return new OrderFinalizeResultDto(
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
