package kcs.funding.fundingboost.domain.service.pay;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_ACCESS_URL;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_ARGUMENT;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_FUNDING_MONEY;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_FUNDING_STATUS;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_POINT_LACK;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_FUNDING;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import kcs.funding.fundingboost.domain.aop.lock.RedisLock;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayBarcodeConsumeDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayProcessDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendFundingPayingDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeIssueDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeVerifyDto;
import kcs.funding.fundingboost.domain.entity.Contributor;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeToken;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeTokenStatus;
import kcs.funding.fundingboost.domain.entity.Funding;
import kcs.funding.fundingboost.domain.entity.FundingItem;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.FriendPayBarcodeTokenRepository;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.contributor.ContributorRepository;
import kcs.funding.fundingboost.domain.repository.funding.FundingRepository;
import kcs.funding.fundingboost.domain.service.utils.PayUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Timed("FriendPayService")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendPayService {

    private static final int BARCODE_TOKEN_EXPIRE_MINUTES = 5;
    private static final String BARCODE_TOKEN_PREFIX = "FBPAY-";

    private final MemberRepository memberRepository;
    private final FundingRepository fundingRepository;
    private final ContributorRepository contributorRepository;
    private final FriendPayBarcodeTokenRepository friendPayBarcodeTokenRepository;

    public FriendFundingPayingDto getFriendFundingPay(Long fundingId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Funding friendFunding = fundingRepository.findById(fundingId).orElseThrow();

        return FriendFundingPayingDto.fromEntity(friendFunding, member.getPoint());
    }

    @Transactional
    public FriendPayBarcodeIssueDto issueBarcodeToken(Long memberId, Long fundingId, FriendPayProcessDto friendPayProcessDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING));

        int normalizedUsingPoint = normalizeUsingPoint(friendPayProcessDto.usingPoint());
        int normalizedFundingPrice = normalizeFundingPrice(friendPayProcessDto.fundingPrice());
        validatePayArguments(member, funding, normalizedUsingPoint, normalizedFundingPrice);

        expirePendingTokens(memberId, fundingId);

        String tokenValue = generateBarcodeToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(BARCODE_TOKEN_EXPIRE_MINUTES);
        FriendPayBarcodeToken token = FriendPayBarcodeToken.createToken(
                tokenValue,
                member,
                funding,
                normalizedUsingPoint,
                normalizedFundingPrice,
                expiresAt
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
    public FriendPayBarcodeVerifyDto verifyBarcodeToken(String rawToken) {
        String token = normalizeToken(rawToken);
        FriendPayBarcodeToken barcodeToken = friendPayBarcodeTokenRepository.findByBarcodeToken(token)
                .orElseThrow(() -> new CommonException(INVALID_ACCESS_URL));

        if (barcodeToken.isExpired(LocalDateTime.now())) {
            barcodeToken.markExpired();
        }

        boolean expired = barcodeToken.getTokenStatus() == FriendPayBarcodeTokenStatus.EXPIRED;
        boolean used = barcodeToken.getTokenStatus() == FriendPayBarcodeTokenStatus.USED;

        return FriendPayBarcodeVerifyDto.builder()
                .token(barcodeToken.getBarcodeToken())
                .status(barcodeToken.getTokenStatus().name())
                .fundingId(barcodeToken.getFunding().getFundingId())
                .friendName(barcodeToken.getFunding().getMember().getNickName())
                .usingPoint(barcodeToken.getUsingPoint())
                .fundingPrice(barcodeToken.getFundingPrice())
                .expiresAt(barcodeToken.getExpiresAt())
                .expired(expired)
                .used(used)
                .build();
    }

    @Counted("FriendPayService.fund")
    @RedisLock(key = "lock:friend-funding", argIndex = 1)
    @Transactional
    public CommonSuccessDto fund(Long memberId, Long fundingId,
                                 FriendPayProcessDto friendPayProcessDto) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING));

        int normalizedUsingPoint = normalizeUsingPoint(friendPayProcessDto.usingPoint());
        int normalizedFundingPrice = normalizeFundingPrice(friendPayProcessDto.fundingPrice());
        validatePayArguments(member, funding, normalizedUsingPoint, normalizedFundingPrice);

        return executeFunding(member, funding, normalizedUsingPoint, normalizedFundingPrice);
    }

    @Counted("FriendPayService.fundWithBarcodeToken")
    @RedisLock(key = "lock:friend-funding", argIndex = 1)
    @Transactional
    public CommonSuccessDto fundWithBarcodeToken(
            Long memberId,
            Long fundingId,
            FriendPayBarcodeConsumeDto consumeDto
    ) {
        String tokenValue = normalizeToken(consumeDto.token());
        FriendPayBarcodeToken token = friendPayBarcodeTokenRepository.findByBarcodeToken(tokenValue)
                .orElseThrow(() -> new CommonException(INVALID_ACCESS_URL));

        if (!token.getMember().getMemberId().equals(memberId) || !token.getFunding().getFundingId().equals(fundingId)) {
            throw new CommonException(INVALID_ACCESS_URL);
        }

        if (token.getTokenStatus() == FriendPayBarcodeTokenStatus.USED) {
            throw new CommonException(INVALID_ACCESS_URL);
        }
        if (token.isExpired(LocalDateTime.now())) {
            token.markExpired();
            throw new CommonException(INVALID_ACCESS_URL);
        }

        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_FUNDING));

        validatePayArguments(member, funding, token.getUsingPoint(), token.getFundingPrice());

        CommonSuccessDto result = executeFunding(member, funding, token.getUsingPoint(), token.getFundingPrice());
        token.markUsed(LocalDateTime.now());
        return result;
    }

    private void expirePendingTokens(Long memberId, Long fundingId) {
        List<FriendPayBarcodeToken> pendingTokens =
                friendPayBarcodeTokenRepository.findAllByMemberMemberIdAndFundingFundingIdAndTokenStatus(
                        memberId,
                        fundingId,
                        FriendPayBarcodeTokenStatus.PENDING
                );
        pendingTokens.forEach(FriendPayBarcodeToken::markExpired);
    }

    private CommonSuccessDto executeFunding(Member member, Funding funding, int usingPoint, int fundingPrice) {
        PayUtils.deductPointsIfPossible(member, usingPoint);

        if (funding.getCollectPrice() + fundingPrice > funding.getTotalPrice()) {
            throw new CommonException(INVALID_FUNDING_MONEY);
        }

        Contributor contributor = Contributor.createContributor(fundingPrice, member, funding);
        contributorRepository.save(contributor);

        List<FundingItem> fundingItemList = funding.getFundingItems();
        int collect = funding.getCollectPrice();
        for (FundingItem fundingItem : fundingItemList) {
            if (fundingItem.getItem().getItemPrice() <= collect) {
                fundingItem.completeFunding();
                collect -= fundingItem.getItem().getItemPrice();
            } else {
                break;
            }
        }
        return CommonSuccessDto.fromEntity(true);
    }

    private void validatePayArguments(Member member, Funding funding, int usingPoint, int fundingPrice) {
        if (!funding.isFundingStatus()) {
            throw new CommonException(INVALID_FUNDING_STATUS);
        }
        if (fundingPrice <= 0) {
            throw new CommonException(INVALID_ARGUMENT);
        }
        if (usingPoint < 0) {
            throw new CommonException(INVALID_ARGUMENT);
        }
        if (usingPoint > fundingPrice) {
            throw new CommonException(INVALID_ARGUMENT);
        }
        if (funding.getCollectPrice() + fundingPrice > funding.getTotalPrice()) {
            throw new CommonException(INVALID_FUNDING_MONEY);
        }
        if (member.getPoint() < usingPoint) {
            throw new CommonException(INVALID_POINT_LACK);
        }
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
