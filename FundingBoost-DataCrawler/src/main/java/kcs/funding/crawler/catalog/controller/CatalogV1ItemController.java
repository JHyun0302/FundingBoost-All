package kcs.funding.crawler.catalog.controller;

import java.util.List;
import kcs.funding.crawler.catalog.api.ItemDetailDto;
import kcs.funding.crawler.catalog.api.ResponseDto;
import kcs.funding.crawler.catalog.api.ShopDto;
import kcs.funding.crawler.catalog.service.CatalogQueryService;
import kcs.funding.crawler.catalog.service.JwtMemberResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
@RequestMapping("/api/v1/items")
public class CatalogV1ItemController {

    private final CatalogQueryService catalogQueryService;
    private final JwtMemberResolver jwtMemberResolver;

    @GetMapping("")
    public ResponseDto<Slice<ShopDto>> getItems(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "lastItemId", required = false) Long lastItemId,
            Pageable pageable
    ) {
        return ResponseDto.ok(catalogQueryService.getV1Items(lastItemId, category, keyword, pageable));
    }

    @GetMapping("/categories")
    public ResponseDto<List<String>> getCategories() {
        return ResponseDto.ok(catalogQueryService.getCategories());
    }

    @GetMapping("/{itemId}")
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
}
