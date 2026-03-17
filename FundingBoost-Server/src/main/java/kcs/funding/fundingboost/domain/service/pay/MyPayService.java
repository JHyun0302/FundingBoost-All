package kcs.funding.fundingboost.domain.service.pay;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.BAD_REQUEST_PARAMETER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_FUNDINGITEM_STATUS;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_ITEM_QUANTITY;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_DELIVERY;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_FUNDING_ITEM;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_ITEM;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.ONGOING_FUNDING_ERROR;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayNowDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.MyPayDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.deliveryManage.DeliveryDto;
import kcs.funding.fundingboost.domain.dto.response.pay.myPay.MyFundingPayViewDto;
import kcs.funding.fundingboost.domain.dto.response.pay.myPay.MyOrderPayViewDto;
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
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import kcs.funding.fundingboost.domain.repository.orderItem.OrderItemRepository;
import kcs.funding.fundingboost.domain.service.utils.PayUtils;
import kcs.funding.fundingboost.payment.application.PaymentExecutionCommand;
import kcs.funding.fundingboost.payment.application.PaymentExecutionResult;
import kcs.funding.fundingboost.payment.application.PaymentIntentKeyResolver;
import kcs.funding.fundingboost.payment.application.PaymentIntentOrchestrator;
import kcs.funding.fundingboost.payment.domain.PaymentIntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("MyPayService")
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPayService {

    private final DeliveryRepository deliveryRepository;
    private final MemberRepository memberRepository;
    private final FundingItemRepository fundingItemRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final GiftHubItemRepository giftHubItemRepository;
    private final PaymentIntentOrchestrator paymentIntentOrchestrator;

    @Counted("MyPayService.myFundingPayView")
    public MyFundingPayViewDto myFundingPayView(Long fundingItemId, Long memberId) {
        FundingItem fundingItem = fundingItemRepository.findFundingItemByFundingItemId(fundingItemId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING_ITEM));

        List<DeliveryDto> deliveryDtoList = deliveryRepository.findAllByMemberId(memberId)
                .stream()
                .map(DeliveryDto::fromEntity)
                .toList();
        // 펀딩 아이템이 이미 배송지 입력이 완료되었거나 포인트로 전환 했거나 전여 금액 결제했을 경우
        if (!fundingItem.isFinishedStatus()) {
            throw new CommonException(INVALID_FUNDINGITEM_STATUS);
        }
        // 펀딩 진행중일 때
        if (fundingItem.getFunding().getDeadline().isAfter(LocalDateTime.now())) {
            throw new CommonException(ONGOING_FUNDING_ERROR);
        }
        // 로그인 한 사용자와 일치하는 지 확인
        if (!fundingItem.getFunding().getMember().getMemberId().equals(memberId)) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        return MyFundingPayViewDto.fromEntity(fundingItem.getFunding(), deliveryDtoList);
    }

    public MyOrderPayViewDto myOrderPayView(Long memberId) {
        List<DeliveryDto> deliveryDtoList = deliveryRepository.findAllByMemberId(memberId)
                .stream()
                .map(DeliveryDto::fromEntity)
                .toList();

        int point = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER))
                .getPoint();

        return MyOrderPayViewDto.fromEntity(deliveryDtoList, point);
    }

    @Counted("MyPayService.payMyItem")
    @Transactional
    public CommonSuccessDto payMyItem(MyPayDto myPayDto, Long memberId) {
        return payMyItem(myPayDto, memberId, null);
    }

    @Counted("MyPayService.payMyItem")
    @Transactional
    public CommonSuccessDto payMyItem(MyPayDto myPayDto, Long memberId, String idempotencyKey) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

        Delivery delivery = deliveryRepository.findById(myPayDto.deliveryId())
                .orElseThrow(() -> new CommonException((NOT_FOUND_DELIVERY)));

        if (!Objects.equals(delivery.getMember().getMemberId(), member.getMemberId())) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        if (myPayDto.itemPayDtoList().isEmpty()) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }
        int requestedUsingPoint = sanitizeUsingPoint(myPayDto.usingPoint());

        List<Long> itemIds = myPayDto.itemPayDtoList().stream()
                .map(ItemPayDto::itemId)
                .toList();

        Map<Long, Item> itemMap = itemRepository.findItemsByItemIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));
        Map<Long, GiftHubItem> giftHubItemMap = extractValidatedGiftHubItems(myPayDto.itemPayDtoList(), memberId);

        Order order = Order.createOrder(member, delivery);
        List<OrderItem> orderItems = myPayDto.itemPayDtoList().stream()
                .map(itemPayDto -> {
                    Item item = itemMap.get(itemPayDto.itemId());
                    if (item == null) {
                        throw new CommonException(NOT_FOUND_ITEM);
                    }
                    int quantity = itemPayDto.quantity();
                    if (quantity <= 0) {
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
                            quantity,
                            resolveOptionName(
                                    itemPayDto.optionName(),
                                    giftHubItem != null ? giftHubItem.getOptionName() : null,
                                    item.getOptionName()
                            )
                    );
                }).toList();

        List<Long> giftHubIdList = giftHubItemMap.keySet().stream().toList();

        int pointUsedAmount = resolveApplicablePoint(member, requestedUsingPoint, order.getTotalPrice());
        int directPaidAmount = Math.max(order.getTotalPrice() - pointUsedAmount, 0);
        PaymentExecutionResult paymentExecutionResult = executePayment(
                memberId,
                PaymentIntentType.ORDER_CART,
                null,
                idempotencyKey,
                order.getTotalPrice(),
                pointUsedAmount,
                directPaidAmount,
                0
        );
        if (isAlreadyProcessed(paymentExecutionResult.intentKey())) {
            return CommonSuccessDto.fromEntity(true);
        }
        PayUtils.deductPointsIfPossible(member, pointUsedAmount);
        order.applyPaymentBreakdown(pointUsedAmount, directPaidAmount, 0, null);
        order.linkPaymentIntentKey(paymentExecutionResult.intentKey());

        if (!giftHubIdList.isEmpty()) {
            giftHubItemRepository.deleteAllById(giftHubIdList);
        }
        orderRepository.save(order);
        paymentIntentOrchestrator.attachOrderId(paymentExecutionResult.intentKey(), order.getOrderId());
        orderItemRepository.saveAll(orderItems);

        return CommonSuccessDto.fromEntity(true);
    }

    @Counted("MyPayService.payMyItemNow")
    @Transactional
    public CommonSuccessDto payMyItemNow(ItemPayNowDto itemPayNowDto, Long memberId) {
        return payMyItemNow(itemPayNowDto, memberId, null);
    }

    @Counted("MyPayService.payMyItemNow")
    @Transactional
    public CommonSuccessDto payMyItemNow(ItemPayNowDto itemPayNowDto, Long memberId, String idempotencyKey) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

        Delivery delivery = deliveryRepository.findById(itemPayNowDto.deliveryId())
                .orElseThrow(() -> new CommonException((NOT_FOUND_DELIVERY)));

        if (!Objects.equals(delivery.getMember().getMemberId(), member.getMemberId())) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        if (itemPayNowDto.quantity() == 0) {
            throw new CommonException(INVALID_ITEM_QUANTITY);
        }
        int requestedUsingPoint = sanitizeUsingPoint(itemPayNowDto.usingPoint());
        Item item = itemRepository.findById(itemPayNowDto.itemId()).orElseThrow(
                () -> new CommonException(NOT_FOUND_ITEM));
        Order order = Order.createOrder(member, delivery);
        OrderItem orderItem = OrderItem.createOrderItem(
                order,
                item,
                itemPayNowDto.quantity(),
                resolveOptionName(itemPayNowDto.optionName(), null, item.getOptionName())
        );
        int pointUsedAmount = resolveApplicablePoint(member, requestedUsingPoint, order.getTotalPrice());
        int directPaidAmount = Math.max(order.getTotalPrice() - pointUsedAmount, 0);
        PaymentExecutionResult paymentExecutionResult = executePayment(
                memberId,
                PaymentIntentType.ORDER_NOW,
                itemPayNowDto.itemId(),
                idempotencyKey,
                order.getTotalPrice(),
                pointUsedAmount,
                directPaidAmount,
                0
        );
        if (isAlreadyProcessed(paymentExecutionResult.intentKey())) {
            return CommonSuccessDto.fromEntity(true);
        }
        PayUtils.deductPointsIfPossible(member, pointUsedAmount);
        order.applyPaymentBreakdown(pointUsedAmount, directPaidAmount, 0, null);
        order.linkPaymentIntentKey(paymentExecutionResult.intentKey());
        orderRepository.save(order);
        paymentIntentOrchestrator.attachOrderId(paymentExecutionResult.intentKey(), order.getOrderId());
        orderItemRepository.save(orderItem);
        return CommonSuccessDto.fromEntity(true);
    }

    @Counted("MyPayService.payMyFunding")
    @Transactional
    public CommonSuccessDto payMyFunding(Long fundingItemId, PayRemainDto payRemainDto, Long memberId) {
        return payMyFunding(fundingItemId, payRemainDto, memberId, null);
    }

    @Counted("MyPayService.payMyFunding")
    @Transactional
    public CommonSuccessDto payMyFunding(Long fundingItemId, PayRemainDto payRemainDto, Long memberId, String idempotencyKey) {
        FundingItem fundingItem = Optional.ofNullable(fundingItemRepository.findFundingItemAndItemByFundingItemId(fundingItemId))
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING_ITEM));
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Delivery delivery = deliveryRepository.findById(payRemainDto.deliveryId())
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));

        if (!Objects.equals(delivery.getMember().getMemberId(), member.getMemberId())) {
            throw new CommonException(BAD_REQUEST_PARAMETER);
        }

        if (!fundingItem.isFinishedStatus()) {
            if (isAlreadyProcessedByIdempotencyKey(memberId, idempotencyKey)) {
                return CommonSuccessDto.fromEntity(true);
            }
            throw new CommonException(INVALID_FUNDINGITEM_STATUS);
        } else {
            fundingItem.finishFundingItem();
        }
        int requestedUsingPoint = sanitizeUsingPoint(payRemainDto.usingPoint());

        Order order = Order.createOrder(member, delivery);
        OrderItem orderItem = OrderItem.createOrderItem(order, fundingItem.getItem(), 1);
        int fundingSupportedAmount = Math.min(fundingItem.getFunding().getCollectPrice(), order.getTotalPrice());
        int payableAfterFundingAmount = Math.max(order.getTotalPrice() - fundingSupportedAmount, 0);
        int pointUsedAmount = resolveApplicablePoint(member, requestedUsingPoint, payableAfterFundingAmount);
        int directPaidAmount = Math.max(payableAfterFundingAmount - pointUsedAmount, 0);
        PaymentExecutionResult paymentExecutionResult = executePayment(
                memberId,
                PaymentIntentType.FUNDING_REMAIN,
                fundingItem.getFundingItemId(),
                idempotencyKey,
                order.getTotalPrice(),
                pointUsedAmount,
                directPaidAmount,
                fundingSupportedAmount
        );
        if (isAlreadyProcessed(paymentExecutionResult.intentKey())) {
            return CommonSuccessDto.fromEntity(true);
        }
        PayUtils.deductPointsIfPossible(member, pointUsedAmount);
        order.applyPaymentBreakdown(
                pointUsedAmount,
                directPaidAmount,
                fundingSupportedAmount,
                fundingItem.getFunding().getFundingId()
        );
        order.linkPaymentIntentKey(paymentExecutionResult.intentKey());
        orderRepository.save(order);
        paymentIntentOrchestrator.attachOrderId(paymentExecutionResult.intentKey(), order.getOrderId());
        orderItemRepository.save(orderItem);

        return CommonSuccessDto.fromEntity(true);
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

    private PaymentExecutionResult executePayment(
            Long memberId,
            PaymentIntentType paymentIntentType,
            Long referenceId,
            String idempotencyKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        return paymentIntentOrchestrator.execute(
                new PaymentExecutionCommand(
                        memberId,
                        paymentIntentType,
                        referenceId,
                        idempotencyKey,
                        totalAmount,
                        pointAmount,
                        pgAmount,
                        fundingSupportedAmount,
                        "KRW"
                )
        );
    }

    private boolean isAlreadyProcessed(String intentKey) {
        return orderRepository.findByPaymentIntentKey(intentKey).isPresent();
    }

    private boolean isAlreadyProcessedByIdempotencyKey(Long memberId, String idempotencyKey) {
        return paymentIntentKeyFromIdempotencyKey(idempotencyKey)
                .flatMap(orderRepository::findByPaymentIntentKey)
                .filter(order -> order.getMember().getMemberId().equals(memberId))
                .isPresent();
    }

    private Optional<String> paymentIntentKeyFromIdempotencyKey(String idempotencyKey) {
        return PaymentIntentKeyResolver.resolveFromIdempotencyKey(idempotencyKey);
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
}
