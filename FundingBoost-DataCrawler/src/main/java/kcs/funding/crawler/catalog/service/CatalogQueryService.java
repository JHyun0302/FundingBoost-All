package kcs.funding.crawler.catalog.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import kcs.funding.crawler.catalog.api.CatalogItemSnapshotDto;
import kcs.funding.crawler.catalog.api.HomeRankingItemDto;
import kcs.funding.crawler.catalog.api.ItemDetailDto;
import kcs.funding.crawler.catalog.api.ShopDto;
import kcs.funding.crawler.entity.Item;
import kcs.funding.crawler.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RANKING_SIZE = 50;
    private static final int RANKING_SCAN_SIZE = 500;

    private final ItemRepository itemRepository;
    private final BookmarkReadService bookmarkReadService;
    private final CatalogRankingReadService catalogRankingReadService;

    public Slice<ShopDto> getV1Items(Long lastItemId, String category, String keyword, Pageable pageable) {
        int pageSize = normalizePageSize(pageable.getPageSize());
        List<Item> candidates = itemRepository.findForCursorPaging(
                lastItemId,
                normalizeBlank(category),
                normalizeBlank(keyword),
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "itemId"))
        );

        boolean hasNext = candidates.size() > pageSize;
        if (hasNext) {
            candidates = candidates.subList(0, pageSize);
        }

        List<ShopDto> content = candidates.stream()
                .map(ShopDto::fromEntity)
                .toList();

        return new SliceImpl<>(content, PageRequest.of(0, pageSize), hasNext);
    }

    public Slice<ShopDto> getV3Items(String category, Pageable pageable) {
        Pageable sortedPageable = withDefaultSort(pageable);
        Slice<Item> slice = itemRepository.findSliceByCategory(normalizeBlank(category), sortedPageable);
        return slice.map(ShopDto::fromEntity);
    }

    public Slice<ShopDto> searchV3Items(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeBlank(keyword);
        if (normalizedKeyword == null) {
            return new SliceImpl<>(List.of(), withDefaultSort(pageable), false);
        }

        Slice<Item> slice = itemRepository.searchByKeyword(normalizedKeyword, withDefaultSort(pageable));
        return slice.map(ShopDto::fromEntity);
    }

    public List<String> getCategories() {
        return itemRepository.findDistinctCategories();
    }

    public Optional<ItemDetailDto> getItemDetail(Long itemId, Long memberId) {
        return itemRepository.findById(itemId)
                .map(item -> ItemDetailDto.fromEntity(
                        item,
                        bookmarkReadService.isBookmarked(memberId, item.getItemId())
                ));
    }

    public Optional<CatalogItemSnapshotDto> getItemSnapshot(Long itemId) {
        return itemRepository.findById(itemId).map(CatalogItemSnapshotDto::fromEntity);
    }

    public List<CatalogItemSnapshotDto> getItemSnapshots(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, CatalogItemSnapshotDto> byItemId = itemRepository.findAllByItemIdIn(itemIds).stream()
                .map(CatalogItemSnapshotDto::fromEntity)
                .collect(Collectors.toMap(CatalogItemSnapshotDto::itemId, snapshot -> snapshot));

        return itemIds.stream()
                .map(byItemId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public HomeRankingResult getHomeRankings(
            String rankingType,
            String audience,
            String priceRange,
            int size
    ) {
        int limit = Math.min(Math.max(size, 1), MAX_RANKING_SIZE);
        String normalizedType = normalizeRankingType(rankingType);
        String normalizedPriceRange = normalizePriceRange(priceRange);
        String normalizedAudience = normalizeAudience(audience);

        List<HomeRankingItemDto> ranked = rankItems(normalizedType, normalizedAudience, normalizedPriceRange, limit);
        if (!ranked.isEmpty()) {
            return new HomeRankingResult(ranked, false);
        }

        if (!shouldUseFallback(normalizedAudience, normalizedPriceRange)) {
            return new HomeRankingResult(List.of(), false);
        }

        List<Item> latestItems = itemRepository.findAll(
                PageRequest.of(0, RANKING_SCAN_SIZE, Sort.by(Sort.Direction.DESC, "itemId"))
        ).getContent();

        List<HomeRankingItemDto> fallback = latestItems.stream()
                .limit(limit)
                .map(item -> new ScoredItem(item, 0L))
                .map(new RankingMapper())
                .toList();
        return new HomeRankingResult(fallback, !fallback.isEmpty());
    }

    private List<HomeRankingItemDto> rankItems(
            String rankingType,
            String audience,
            String priceRange,
            int limit
    ) {
        List<CatalogRankingReadService.RankingScore> rankingScores =
                catalogRankingReadService.findRankingScores(rankingType, audience, priceRange, limit);
        if (rankingScores.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = rankingScores.stream()
                .map(CatalogRankingReadService.RankingScore::itemId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, Item> itemById = itemRepository.findAllByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(Item::getItemId, item -> item));

        java.util.ArrayList<HomeRankingItemDto> rankings = new java.util.ArrayList<>(itemIds.size());
        int rank = 1;
        for (CatalogRankingReadService.RankingScore rankingScore : rankingScores) {
            Item item = itemById.get(rankingScore.itemId());
            if (item == null) {
                continue;
            }
            rankings.add(HomeRankingItemDto.fromEntity(item, rankingScore.score(), rank));
            rank++;
        }
        return rankings;
    }

    private boolean isInPriceRange(int price, String priceRange) {
        return switch (priceRange) {
            case "under10k" -> price < 10_000;
            case "10kto30k" -> price >= 10_000 && price < 30_000;
            case "30kto50k" -> price >= 30_000 && price < 50_000;
            case "over50k" -> price >= 50_000;
            default -> true;
        };
    }

    private Pageable withDefaultSort(Pageable pageable) {
        int size = normalizePageSize(pageable.getPageSize());
        if (pageable.getSort().isSorted()) {
            return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
        }
        return PageRequest.of(pageable.getPageNumber(), size, Sort.by(Sort.Direction.DESC, "itemId"));
    }

    private int normalizePageSize(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRankingType(String rankingType) {
        if (rankingType == null) {
            return "funding";
        }
        String normalized = rankingType.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("purchase") || normalized.equals("wish")) {
            return normalized;
        }
        return "funding";
    }

    private String normalizeAudience(String audience) {
        if (audience == null) {
            return "all";
        }
        String normalized = audience.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("woman") || normalized.equals("man")
                || normalized.equals("female") || normalized.equals("male")) {
            return normalized;
        }
        return "all";
    }

    private String normalizePriceRange(String priceRange) {
        if (priceRange == null) {
            return "all";
        }
        String normalized = priceRange.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "under10k", "10kto30k", "30kto50k", "over50k" -> normalized;
            default -> "all";
        };
    }

    public record HomeRankingResult(List<HomeRankingItemDto> items, boolean fallbackApplied) {
    }

    private boolean shouldUseFallback(String audience, String priceRange) {
        return "all".equals(audience) && "all".equals(priceRange);
    }

    private static final class RankingMapper implements java.util.function.Function<ScoredItem, HomeRankingItemDto> {

        private int rank = 1;

        @Override
        public HomeRankingItemDto apply(ScoredItem scoredItem) {
            HomeRankingItemDto dto = HomeRankingItemDto.fromEntity(scoredItem.item(), scoredItem.score(), rank);
            rank++;
            return dto;
        }
    }

    private record ScoredItem(Item item, long score) {
    }
}
