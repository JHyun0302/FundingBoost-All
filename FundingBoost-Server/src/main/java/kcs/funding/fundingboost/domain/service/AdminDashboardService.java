package kcs.funding.fundingboost.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminBarcodeTokenSummaryDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminCategoryMetricDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminDashboardDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminDashboardKpiDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminExpiringFundingDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminRecentBarcodeTokenDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminRecentMemberDto;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminTopItemDto;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeToken;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeTokenStatus;
import kcs.funding.fundingboost.domain.entity.Funding;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.entity.member.MemberRole;
import kcs.funding.fundingboost.domain.repository.FriendPayBarcodeTokenRepository;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.OrderRepository;
import kcs.funding.fundingboost.domain.repository.contributor.ContributorRepository;
import kcs.funding.fundingboost.domain.repository.funding.FundingRepository;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int EXPIRING_FUNDING_LIMIT = 8;
    private static final int RECENT_MEMBER_LIMIT = 8;
    private static final int RECENT_TOKEN_LIMIT = 12;
    private static final int TOP_ITEM_LIMIT = 10;
    private static final int FUNDING_WEIGHT = 3;
    private static final int ORDER_WEIGHT = 2;
    private static final int WISHLIST_WEIGHT = 1;
    private static final String FUNDING_SIGNAL_SQL = "select item_id, count(*) from funding_item group by item_id";
    private static final String ORDER_SIGNAL_SQL = "select item_id, coalesce(sum(quantity), 0) from order_item group by item_id";
    private static final String WISHLIST_SIGNAL_SQL = "select item_id, count(*) from bookmark group by item_id";

    private final MemberRepository memberRepository;
    private final FundingRepository fundingRepository;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final ContributorRepository contributorRepository;
    private final FriendPayBarcodeTokenRepository friendPayBarcodeTokenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminDashboardDto getDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiringTo = now.plusHours(72);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        AdminDashboardKpiDto kpi = AdminDashboardKpiDto.from(
                memberRepository.count(),
                memberRepository.countByMemberRole(MemberRole.ROLE_ADMIN),
                fundingRepository.count(),
                fundingRepository.countByFundingStatus(true),
                orderRepository.count(),
                orderRepository.countByCreatedDateBetween(todayStart, tomorrowStart),
                nullSafeLong(orderRepository.sumTotalPrice()),
                nullSafeLong(orderRepository.sumTotalPriceByCreatedDateBetween(todayStart, tomorrowStart))
        );

        List<AdminExpiringFundingDto> expiringFundings = fundingRepository.findExpiringFundings(
                        now,
                        expiringTo,
                        PageRequest.of(0, EXPIRING_FUNDING_LIMIT))
                .stream()
                .map(this::toExpiringFundingDto)
                .toList();

        List<Member> recentMemberEntities = memberRepository.findRecentMembers(PageRequest.of(0, RECENT_MEMBER_LIMIT));
        List<AdminRecentMemberDto> recentMembers = recentMemberEntities.stream()
                .map(member -> AdminRecentMemberDto.from(
                        member.getMemberId(),
                        member.getNickName(),
                        member.getEmail(),
                        member.getMemberRole().name(),
                        member.getGender() == null ? "UNKNOWN" : member.getGender().name(),
                        member.getCreatedDate()
                ))
                .toList();

        List<FriendPayBarcodeToken> recentTokenEntities = friendPayBarcodeTokenRepository.findRecentTokens(
                PageRequest.of(0, RECENT_TOKEN_LIMIT)
        );
        List<AdminRecentBarcodeTokenDto> recentBarcodeTokens = recentTokenEntities.stream()
                .map(token -> AdminRecentBarcodeTokenDto.from(
                        token.getBarcodeToken(),
                        token.getTokenStatus().name(),
                        token.getMember().getNickName(),
                        token.getFunding().getFundingId(),
                        token.getFunding().getMember().getNickName(),
                        token.getFundingPrice(),
                        token.getUsingPoint(),
                        token.getCreatedDate(),
                        token.getExpiresAt(),
                        token.getUsedAt()
                ))
                .toList();

        AdminBarcodeTokenSummaryDto barcodeTokenSummary = AdminBarcodeTokenSummaryDto.from(
                friendPayBarcodeTokenRepository.countByTokenStatus(FriendPayBarcodeTokenStatus.PENDING),
                friendPayBarcodeTokenRepository.countByTokenStatus(FriendPayBarcodeTokenStatus.USED),
                friendPayBarcodeTokenRepository.countByTokenStatus(FriendPayBarcodeTokenStatus.EXPIRED)
        );

        Map<Long, ItemSignal> itemSignals = buildItemSignals();
        List<AdminTopItemDto> topItems = buildTopItems(itemSignals);
        List<AdminCategoryMetricDto> categoryMetrics = buildCategoryMetrics(itemSignals);

        return AdminDashboardDto.from(
                kpi,
                categoryMetrics,
                topItems,
                expiringFundings,
                recentMembers,
                barcodeTokenSummary,
                recentBarcodeTokens
        );
    }

    private Map<Long, ItemSignal> buildItemSignals() {
        Map<Long, ItemSignal> signals = new HashMap<>();

        for (Object[] row : queryItemSignals(FUNDING_SIGNAL_SQL)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long itemId = ((Number) row[0]).longValue();
            ItemSignal signal = signals.computeIfAbsent(itemId, ignored -> new ItemSignal());
            signal.fundingCount = ((Number) row[1]).longValue();
        }
        for (Object[] row : queryItemSignals(ORDER_SIGNAL_SQL)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long itemId = ((Number) row[0]).longValue();
            ItemSignal signal = signals.computeIfAbsent(itemId, ignored -> new ItemSignal());
            signal.orderQuantity = ((Number) row[1]).longValue();
        }
        for (Object[] row : queryItemSignals(WISHLIST_SIGNAL_SQL)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long itemId = ((Number) row[0]).longValue();
            ItemSignal signal = signals.computeIfAbsent(itemId, ignored -> new ItemSignal());
            signal.wishlistCount = ((Number) row[1]).longValue();
        }

        return signals;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryItemSignals(String sql) {
        return entityManager.createNativeQuery(sql).getResultList();
    }

    private List<AdminTopItemDto> buildTopItems(Map<Long, ItemSignal> itemSignals) {
        Comparator<Map.Entry<Long, ItemSignal>> topItemComparator = Comparator
                .comparingLong((Map.Entry<Long, ItemSignal> entry) -> entry.getValue().score()).reversed()
                .thenComparing(Comparator.comparingLong(
                        (Map.Entry<Long, ItemSignal> entry) -> entry.getValue().fundingCount).reversed())
                .thenComparing(Comparator.comparingLong(
                        (Map.Entry<Long, ItemSignal> entry) -> entry.getValue().orderQuantity).reversed())
                .thenComparing(Comparator.comparingLong(
                        (Map.Entry<Long, ItemSignal> entry) -> entry.getValue().wishlistCount).reversed())
                .thenComparingLong(Map.Entry::getKey);

        Map<Long, Item> itemById = itemRepository.findAllById(itemSignals.keySet()).stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));
        return itemSignals.entrySet().stream()
                .filter(entry -> entry.getValue().score() > 0)
                .filter(entry -> itemById.containsKey(entry.getKey()))
                .sorted(topItemComparator)
                .limit(TOP_ITEM_LIMIT)
                .map(entry -> {
                    Item item = itemById.get(entry.getKey());
                    ItemSignal signal = entry.getValue();
                    return AdminTopItemDto.from(
                            item.getItemId(),
                            item.getItemName(),
                            item.getBrandName(),
                            item.getCategory(),
                            item.getItemImageUrl(),
                            item.getItemPrice(),
                            signal.fundingCount,
                            signal.orderQuantity,
                            signal.wishlistCount,
                            signal.score()
                    );
                })
                .toList();
    }

    private List<AdminCategoryMetricDto> buildCategoryMetrics(Map<Long, ItemSignal> itemSignals) {
        Map<Long, Item> allItems = itemRepository.findAllById(itemSignals.keySet()).stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        Map<String, CategorySignal> categorySignalMap = new LinkedHashMap<>();
        for (Map.Entry<Long, ItemSignal> entry : itemSignals.entrySet()) {
            Item item = allItems.get(entry.getKey());
            if (item == null || item.getCategory() == null || item.getCategory().isBlank()) {
                continue;
            }
            CategorySignal categorySignal = categorySignalMap.computeIfAbsent(item.getCategory(), ignored -> new CategorySignal());
            categorySignal.fundingCount += entry.getValue().fundingCount;
            categorySignal.orderQuantity += entry.getValue().orderQuantity;
            categorySignal.wishlistCount += entry.getValue().wishlistCount;
        }

        return categorySignalMap.entrySet().stream()
                .map(entry -> AdminCategoryMetricDto.from(
                        entry.getKey(),
                        entry.getValue().fundingCount,
                        entry.getValue().orderQuantity,
                        entry.getValue().wishlistCount,
                        entry.getValue().score()
                ))
                .sorted(Comparator
                        .comparingLong(AdminCategoryMetricDto::score).reversed()
                        .thenComparing(AdminCategoryMetricDto::category, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private AdminExpiringFundingDto toExpiringFundingDto(Funding funding) {
        int totalPrice = Math.max(funding.getTotalPrice(), 0);
        int collectPrice = Math.max(funding.getCollectPrice(), 0);
        int progressPercent = totalPrice > 0
                ? (int) Math.min((collectPrice * 100.0) / totalPrice, 100.0)
                : 0;
        int contributorCount = contributorRepository.countContributorsForFunding(funding.getFundingId());

        return AdminExpiringFundingDto.from(
                funding.getFundingId(),
                funding.getMember().getNickName(),
                funding.getMember().getProfileImgUrl(),
                funding.getDisplayTag(),
                totalPrice,
                collectPrice,
                progressPercent,
                contributorCount,
                funding.getDeadline()
        );
    }

    private long nullSafeLong(Long value) {
        return Objects.requireNonNullElse(value, 0L);
    }

    private static class ItemSignal {
        private long fundingCount;
        private long orderQuantity;
        private long wishlistCount;

        private long score() {
            return (fundingCount * FUNDING_WEIGHT)
                    + (orderQuantity * ORDER_WEIGHT)
                    + (wishlistCount * WISHLIST_WEIGHT);
        }
    }

    private static class CategorySignal {
        private long fundingCount;
        private long orderQuantity;
        private long wishlistCount;

        private long score() {
            return (fundingCount * FUNDING_WEIGHT)
                    + (orderQuantity * ORDER_WEIGHT)
                    + (wishlistCount * WISHLIST_WEIGHT);
        }
    }
}
