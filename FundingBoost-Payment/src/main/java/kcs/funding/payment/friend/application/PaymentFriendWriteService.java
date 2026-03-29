package kcs.funding.payment.friend.application;

import static kcs.funding.payment.exception.ErrorCode.INVALID_ACCESS_URL;
import static kcs.funding.payment.exception.ErrorCode.INVALID_ARGUMENT;
import static kcs.funding.payment.exception.ErrorCode.INVALID_FUNDING_MONEY;
import static kcs.funding.payment.exception.ErrorCode.INVALID_FUNDING_OR_PRICE;
import static kcs.funding.payment.exception.ErrorCode.INVALID_FUNDING_STATUS;
import static kcs.funding.payment.exception.ErrorCode.INVALID_POINT_LACK;
import static kcs.funding.payment.exception.ErrorCode.NOT_FOUND_FUNDING;
import static kcs.funding.payment.exception.ErrorCode.NOT_FOUND_MEMBER;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import kcs.funding.payment.common.dto.CommonSuccessDto;
import kcs.funding.payment.event.application.OutboxEventService;
import kcs.funding.payment.exception.CommonException;
import kcs.funding.payment.friend.domain.PaymentCatalogItem;
import kcs.funding.payment.friend.domain.PaymentCatalogItemRepository;
import kcs.funding.payment.friend.domain.PaymentContributor;
import kcs.funding.payment.friend.domain.PaymentContributorRepository;
import kcs.funding.payment.friend.domain.PaymentFriendPayBarcodeToken;
import kcs.funding.payment.friend.domain.PaymentFriendPayBarcodeTokenRepository;
import kcs.funding.payment.friend.domain.PaymentFriendPayBarcodeTokenStatus;
import kcs.funding.payment.friend.domain.PaymentFunding;
import kcs.funding.payment.friend.domain.PaymentFundingItem;
import kcs.funding.payment.friend.domain.PaymentFundingItemRepository;
import kcs.funding.payment.friend.domain.PaymentFundingRepository;
import kcs.funding.payment.friend.domain.PaymentMember;
import kcs.funding.payment.friend.domain.PaymentMemberRepository;
import kcs.funding.payment.friend.dto.FriendPayBarcodeConsumeDto;
import kcs.funding.payment.friend.dto.FriendPayBarcodeIssueDto;
import kcs.funding.payment.friend.dto.FriendPayProcessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentFriendWriteService {

    private static final int BARCODE_TOKEN_EXPIRE_MINUTES = 5;
    private static final String BARCODE_TOKEN_PREFIX = "FBPAY-";

    private final PaymentMemberRepository memberRepository;
    private final PaymentFundingRepository fundingRepository;
    private final PaymentFundingItemRepository fundingItemRepository;
    private final PaymentCatalogItemRepository catalogItemRepository;
    private final PaymentContributorRepository contributorRepository;
    private final PaymentFriendPayBarcodeTokenRepository friendPayBarcodeTokenRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    public FriendPayBarcodeIssueDto issueBarcodeToken(Long memberId, Long fundingId, FriendPayProcessDto friendPayProcessDto) {
        PaymentMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        PaymentFunding funding = fundingRepository.findByIdWithMember(fundingId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING));

        int normalizedUsingPoint = normalizeUsingPoint(friendPayProcessDto.usingPoint());
        int normalizedFundingPrice = normalizeFundingPrice(friendPayProcessDto.fundingPrice());
        validatePayArguments(member, funding, normalizedUsingPoint, normalizedFundingPrice);

        expirePendingTokens(memberId, fundingId);

        PaymentFriendPayBarcodeToken token = PaymentFriendPayBarcodeToken.createToken(
                generateBarcodeToken(),
                member,
                funding,
                normalizedUsingPoint,
                normalizedFundingPrice,
                LocalDateTime.now().plusMinutes(BARCODE_TOKEN_EXPIRE_MINUTES)
        );
        friendPayBarcodeTokenRepository.save(token);

        return FriendPayBarcodeIssueDto.builder()
                .token(token.getBarcodeToken())
                .barcodeValue(token.getBarcodeToken())
                .verifyUrl(null)
                .expiresAt(token.getExpiresAt())
                .usingPoint(token.getUsingPoint())
                .fundingPrice(token.getFundingPrice())
                .build();
    }

    @Transactional
    public CommonSuccessDto fund(Long memberId, Long fundingId, FriendPayProcessDto friendPayProcessDto) {
        PaymentMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        PaymentFunding funding = fundingRepository.findByIdWithMemberForUpdate(fundingId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING));

        int normalizedUsingPoint = normalizeUsingPoint(friendPayProcessDto.usingPoint());
        int normalizedFundingPrice = normalizeFundingPrice(friendPayProcessDto.fundingPrice());
        validatePayArguments(member, funding, normalizedUsingPoint, normalizedFundingPrice);

        return executeFunding(member, funding, normalizedUsingPoint, normalizedFundingPrice);
    }

    @Transactional
    public CommonSuccessDto fundWithBarcodeToken(Long memberId, Long fundingId, FriendPayBarcodeConsumeDto consumeDto) {
        PaymentFriendPayBarcodeToken token = friendPayBarcodeTokenRepository.findByBarcodeTokenForUpdate(normalizeToken(consumeDto.token()))
                .orElseThrow(() -> new CommonException(INVALID_ACCESS_URL));

        if (!Objects.equals(token.getMember().getMemberId(), memberId)
                || !Objects.equals(token.getFunding().getFundingId(), fundingId)) {
            throw new CommonException(INVALID_ACCESS_URL);
        }
        if (token.getTokenStatus() == PaymentFriendPayBarcodeTokenStatus.USED) {
            throw new CommonException(INVALID_ACCESS_URL);
        }
        if (token.isExpired(LocalDateTime.now())) {
            token.markExpired();
            throw new CommonException(INVALID_ACCESS_URL);
        }

        PaymentMember member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        PaymentFunding funding = fundingRepository.findByIdWithMemberForUpdate(fundingId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING));

        validatePayArguments(member, funding, token.getUsingPoint(), token.getFundingPrice());

        CommonSuccessDto result = executeFunding(member, funding, token.getUsingPoint(), token.getFundingPrice());
        token.markUsed(LocalDateTime.now());
        return result;
    }

    private void expirePendingTokens(Long memberId, Long fundingId) {
        List<PaymentFriendPayBarcodeToken> pendingTokens = friendPayBarcodeTokenRepository
                .findAllByMemberMemberIdAndFundingFundingIdAndTokenStatus(
                        memberId,
                        fundingId,
                        PaymentFriendPayBarcodeTokenStatus.PENDING
                );
        pendingTokens.forEach(PaymentFriendPayBarcodeToken::markExpired);
    }

    private CommonSuccessDto executeFunding(PaymentMember member, PaymentFunding funding, int usingPoint, int fundingPrice) {
        deductPointsIfPossible(member, usingPoint);
        if (funding.getCollectPrice() + fundingPrice > funding.getTotalPrice()) {
            throw new CommonException(INVALID_FUNDING_MONEY);
        }

        PaymentContributor contributor = PaymentContributor.createContributor(fundingPrice, member, funding);
        contributorRepository.save(contributor);
        applyFundingItemCompletion(funding);

        outboxEventService.enqueuePaymentCompletedForFundingContribution(
                funding.getFundingId(),
                contributor.getContributorId(),
                member.getMemberId(),
                funding.getMember().getMemberId(),
                fundingPrice,
                usingPoint,
                funding.getCollectPrice(),
                funding.getTotalPrice()
        );
        outboxEventService.enqueueFundingContributionCreated(
                funding.getFundingId(),
                contributor.getContributorId(),
                member.getMemberId(),
                funding.getMember().getMemberId(),
                fundingPrice,
                usingPoint,
                funding.getCollectPrice(),
                funding.getTotalPrice()
        );

        return CommonSuccessDto.success();
    }

    private void applyFundingItemCompletion(PaymentFunding funding) {
        List<PaymentFundingItem> fundingItems = fundingItemRepository.findAllByFundingIdOrderByItemSequence(funding.getFundingId());
        if (fundingItems.isEmpty()) {
            throw new CommonException(INVALID_FUNDING_OR_PRICE);
        }

        Set<Long> itemIds = fundingItems.stream()
                .map(PaymentFundingItem::getItemReferenceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (itemIds.size() != fundingItems.size()) {
            throw new CommonException(INVALID_FUNDING_OR_PRICE);
        }

        Map<Long, Integer> itemPriceById = new LinkedHashMap<>();
        for (PaymentCatalogItem catalogItem : catalogItemRepository.findAllByItemIdIn(itemIds)) {
            itemPriceById.put(catalogItem.getItemId(), catalogItem.getItemPrice());
        }
        if (itemPriceById.size() != itemIds.size()) {
            throw new CommonException(INVALID_FUNDING_OR_PRICE);
        }

        int collect = funding.getCollectPrice();
        for (PaymentFundingItem fundingItem : fundingItems) {
            Integer itemPrice = itemPriceById.get(fundingItem.getItemReferenceId());
            if (itemPrice == null || itemPrice <= 0) {
                throw new CommonException(INVALID_FUNDING_OR_PRICE);
            }
            if (itemPrice <= collect) {
                fundingItem.completeFunding();
                collect -= itemPrice;
            } else {
                break;
            }
        }
    }

    private void validatePayArguments(PaymentMember member, PaymentFunding funding, int usingPoint, int fundingPrice) {
        if (!funding.isFundingStatus()) {
            throw new CommonException(INVALID_FUNDING_STATUS);
        }
        if (fundingPrice <= 0 || usingPoint < 0 || usingPoint > fundingPrice) {
            throw new CommonException(INVALID_ARGUMENT);
        }
        if (funding.getCollectPrice() + fundingPrice > funding.getTotalPrice()) {
            throw new CommonException(INVALID_FUNDING_MONEY);
        }
        if (member.getPoint() < usingPoint) {
            throw new CommonException(INVALID_POINT_LACK);
        }
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

    private int normalizeUsingPoint(int usingPoint) {
        return Math.max(usingPoint, 0);
    }

    private int normalizeFundingPrice(int fundingPrice) {
        return fundingPrice;
    }

    private String generateBarcodeToken() {
        return BARCODE_TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String normalizeToken(String rawToken) {
        if (rawToken == null) {
            throw new CommonException(INVALID_ARGUMENT);
        }
        String token = rawToken.trim();
        if (token.isEmpty()) {
            throw new CommonException(INVALID_ARGUMENT);
        }
        return token;
    }
}
