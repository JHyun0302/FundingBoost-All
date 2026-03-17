package kcs.funding.fundingboost.elasticsearch.controller;

import java.util.List;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.response.common.SliceResponseDto;
import kcs.funding.fundingboost.domain.dto.response.home.HomeRankingItemDto;
import kcs.funding.fundingboost.domain.dto.response.shopping.ShopDto;
import kcs.funding.fundingboost.domain.dto.response.shoppingDetail.ItemDetailDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.elasticsearch.service.ItemIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3")
@RequiredArgsConstructor
public class SearchController {

    private final ItemIndexService itemIndexService;

    @GetMapping("/search")
    public ResponseDto<SliceResponseDto<ShopDto>> searchItem(
            @RequestParam(name = "keyword", required = true) String keyword,
            Pageable pageable
    ) {
        return ResponseDto.ok(SliceResponseDto.fromSlice(itemIndexService.searchWithCategoryAndName(keyword, pageable)));
    }

    @GetMapping("/items")
    public ResponseDto<SliceResponseDto<ShopDto>> searchCategory(
            @RequestParam(name = "category", required = false) String category,
            Pageable pageable
    ) {
        return ResponseDto.ok(SliceResponseDto.fromSlice(itemIndexService.searchWithCategory(category, pageable)));
    }

    @GetMapping("/items/categories")
    public ResponseDto<List<String>> getCategories() {
        return ResponseDto.ok(itemIndexService.getCategories());
    }

    @GetMapping("/items/{itemId}")
    public ResponseDto<ItemDetailDto> getItemDetail(@Login Long memberId,
                                                    @PathVariable(name = "itemId") Long itemId) {
        return ResponseDto.ok(itemIndexService.getItemDetail(memberId, itemId));
    }

    @GetMapping("/home/rankings")
    public ResponseEntity<ResponseDto<List<HomeRankingItemDto>>> getHomeRankings(
            @RequestParam(name = "rankingType", defaultValue = "funding") String rankingType,
            @RequestParam(name = "audience", defaultValue = "all") String audience,
            @RequestParam(name = "priceRange", defaultValue = "all") String priceRange,
            @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        ItemIndexService.HomeRankingResult result = itemIndexService.getHomeRankings(rankingType, audience, priceRange, size);
        return ResponseEntity.ok()
                .header("X-FundingBoost-Fallback-Applied", Boolean.toString(result.fallbackApplied()))
                .body(ResponseDto.ok(result.items()));
    }
}
