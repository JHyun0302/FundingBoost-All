package kcs.funding.crawler.catalog.controller;

import java.util.List;
import kcs.funding.crawler.catalog.api.CatalogItemSnapshotDto;
import kcs.funding.crawler.catalog.api.HomeRankingItemDto;
import kcs.funding.crawler.catalog.api.ItemDetailDto;
import kcs.funding.crawler.catalog.api.ResponseDto;
import kcs.funding.crawler.catalog.api.ShopDto;
import kcs.funding.crawler.catalog.api.SliceResponseDto;
import kcs.funding.crawler.catalog.service.CatalogQueryService;
import kcs.funding.crawler.catalog.service.JwtMemberResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3")
public class CatalogV3Controller {

    private final CatalogQueryService catalogQueryService;
    private final JwtMemberResolver jwtMemberResolver;

    @GetMapping("/search")
    public ResponseDto<SliceResponseDto<ShopDto>> searchItems(
            @RequestParam(name = "keyword") String keyword,
            Pageable pageable
    ) {
        return ResponseDto.ok(SliceResponseDto.fromSlice(catalogQueryService.searchV3Items(keyword, pageable)));
    }

    @GetMapping("/items")
    public ResponseDto<SliceResponseDto<ShopDto>> getItems(
            @RequestParam(name = "category", required = false) String category,
            Pageable pageable
    ) {
        return ResponseDto.ok(SliceResponseDto.fromSlice(catalogQueryService.getV3Items(category, pageable)));
    }

    @GetMapping("/items/cursor")
    public ResponseDto<SliceResponseDto<ShopDto>> getItemsWithCursor(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "lastItemId", required = false) Long lastItemId,
            Pageable pageable
    ) {
        return ResponseDto.ok(SliceResponseDto.fromSlice(catalogQueryService.getV1Items(lastItemId, category, keyword, pageable)));
    }

    @GetMapping("/items/categories")
    public ResponseDto<List<String>> getCategories() {
        return ResponseDto.ok(catalogQueryService.getCategories());
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<ResponseDto<ItemDetailDto>> getItemDetail(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable(name = "itemId") Long itemId
    ) {
        Long memberId = jwtMemberResolver.resolveMemberId(authorization).orElse(null);
        return catalogQueryService.getItemDetail(itemId, memberId)
                .map(item -> ResponseEntity.ok(ResponseDto.ok(item)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDto.fail(40401, "상품을 찾을 수 없습니다.")));
    }

    @GetMapping("/items/snapshot/{itemId}")
    public ResponseEntity<ResponseDto<CatalogItemSnapshotDto>> getItemSnapshot(
            @PathVariable(name = "itemId") Long itemId
    ) {
        return catalogQueryService.getItemSnapshot(itemId)
                .map(snapshot -> ResponseEntity.ok(ResponseDto.ok(snapshot)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseDto.fail(40401, "상품을 찾을 수 없습니다.")));
    }

    @GetMapping("/items/snapshots")
    public ResponseDto<List<CatalogItemSnapshotDto>> getItemSnapshots(
            @RequestParam(name = "itemIds") List<Long> itemIds
    ) {
        return ResponseDto.ok(catalogQueryService.getItemSnapshots(itemIds));
    }

    @GetMapping("/home/rankings")
    public ResponseEntity<ResponseDto<List<HomeRankingItemDto>>> getHomeRankings(
            @RequestParam(name = "rankingType", defaultValue = "funding") String rankingType,
            @RequestParam(name = "audience", defaultValue = "all") String audience,
            @RequestParam(name = "priceRange", defaultValue = "all") String priceRange,
            @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        CatalogQueryService.HomeRankingResult rankingResult = catalogQueryService.getHomeRankings(
                rankingType,
                audience,
                priceRange,
                size
        );

        return ResponseEntity.ok()
                .header("X-FundingBoost-Fallback-Applied", Boolean.toString(rankingResult.fallbackApplied()))
                .body(ResponseDto.ok(rankingResult.items()));
    }
}
