package kcs.funding.fundingboost.catalog.infra;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import kcs.funding.fundingboost.catalog.application.CatalogItemReader;
import kcs.funding.fundingboost.domain.entity.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.catalog", name = "reader", havingValue = "remote")
public class RemoteCatalogItemReader implements CatalogItemReader {

    private final RestClient catalogRestClient;
    private final CatalogClientProperties catalogClientProperties;

    @Qualifier("localCatalogItemReader")
    private final CatalogItemReader localCatalogItemReader;

    @Override
    public Optional<Item> findById(Long itemId) {
        return executeWithFallback(
                () -> fetchById(itemId),
                () -> localCatalogItemReader.findById(itemId),
                "findById"
        );
    }

    @Override
    public List<Item> findAllById(Iterable<Long> itemIds) {
        List<Long> ids = toDistinctIds(itemIds);
        return executeWithFallback(
                () -> fetchByIds(ids),
                () -> localCatalogItemReader.findAllById(ids),
                "findAllById"
        );
    }

    @Override
    public List<Item> findItemsByItemIds(List<Long> itemIds) {
        List<Long> ids = toDistinctIds(itemIds);
        return executeWithFallback(
                () -> fetchByIds(ids),
                () -> localCatalogItemReader.findItemsByItemIds(ids),
                "findItemsByItemIds"
        );
    }

    @Override
    public Slice<Item> findItems(Long lastItemId, String category, String keyword, Pageable pageable) {
        int size = normalizeSize(pageable);
        return executeWithFallback(
                () -> fetchItemsByCursor(lastItemId, category, keyword, size),
                () -> localCatalogItemReader.findItems(lastItemId, category, keyword, pageable),
                "findItems"
        );
    }

    @Override
    public Slice<Item> findItemsBySlice(Long lastItemId, Pageable pageable) {
        int size = normalizeSize(pageable);
        return executeWithFallback(
                () -> fetchItemsByCursor(lastItemId, null, null, size),
                () -> localCatalogItemReader.findItemsBySlice(lastItemId, pageable),
                "findItemsBySlice"
        );
    }

    @Override
    public List<String> findDistinctCategories() {
        return executeWithFallback(
                this::fetchCategories,
                localCatalogItemReader::findDistinctCategories,
                "findDistinctCategories"
        );
    }

    private Optional<Item> fetchById(Long itemId) {
        if (itemId == null) {
            return Optional.empty();
        }

        try {
            CatalogApiResponse<CatalogItemSnapshotPayload> response = catalogRestClient.get()
                    .uri("/api/v3/items/snapshot/{itemId}", itemId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<CatalogApiResponse<CatalogItemSnapshotPayload>>() {
                    });

            CatalogItemSnapshotPayload data = requireSuccess(response, "fetchById");
            return data == null ? Optional.empty() : Optional.of(toItem(data));
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }

    private List<Item> fetchByIds(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return List.of();
        }

        CatalogApiResponse<List<CatalogItemSnapshotPayload>> response = catalogRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v3/items/snapshots")
                        .queryParam("itemIds", itemIds.toArray())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<CatalogApiResponse<List<CatalogItemSnapshotPayload>>>() {
                });

        List<CatalogItemSnapshotPayload> payloads = requireSuccess(response, "fetchByIds");
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        return payloads.stream()
                .filter(Objects::nonNull)
                .map(this::toItem)
                .toList();
    }

    private Slice<Item> fetchItemsByCursor(Long lastItemId, String category, String keyword, int size) {
        CatalogApiResponse<SlicePayload<ShopPayload>> response = catalogRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v3/items/cursor")
                            .queryParam("size", size);
                    if (lastItemId != null) {
                        uriBuilder.queryParam("lastItemId", lastItemId);
                    }
                    if (category != null && !category.isBlank()) {
                        uriBuilder.queryParam("category", category);
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        uriBuilder.queryParam("keyword", keyword);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<CatalogApiResponse<SlicePayload<ShopPayload>>>() {
                });

        SlicePayload<ShopPayload> payload = requireSuccess(response, "fetchItemsByCursor");
        if (payload == null || payload.content() == null) {
            return new SliceImpl<>(List.of(), PageRequest.of(0, size), false);
        }
        List<Item> items = payload.content().stream()
                .filter(Objects::nonNull)
                .map(this::toItem)
                .toList();
        return new SliceImpl<>(items, PageRequest.of(0, size), payload.hasNext());
    }

    private List<String> fetchCategories() {
        CatalogApiResponse<List<String>> response = catalogRestClient.get()
                .uri("/api/v3/items/categories")
                .retrieve()
                .body(new ParameterizedTypeReference<CatalogApiResponse<List<String>>>() {
                });

        List<String> categories = requireSuccess(response, "fetchCategories");
        return categories == null ? List.of() : categories;
    }

    private int normalizeSize(Pageable pageable) {
        if (pageable == null || pageable.getPageSize() <= 0) {
            return 20;
        }
        return Math.min(pageable.getPageSize(), 100);
    }

    private List<Long> toDistinctIds(Iterable<Long> itemIds) {
        if (itemIds == null) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(itemIds.spliterator(), false)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Item toItem(CatalogItemSnapshotPayload payload) {
        return Item.rehydrate(
                payload.itemId(),
                payload.itemName(),
                payload.itemPrice(),
                payload.itemImageUrl(),
                payload.brandName(),
                payload.category(),
                payload.optionName()
        );
    }

    private Item toItem(ShopPayload payload) {
        return Item.rehydrate(
                payload.itemId(),
                payload.itemName(),
                payload.price(),
                payload.itemImageUrl(),
                payload.brandName(),
                payload.category(),
                null
        );
    }

    private <T> T requireSuccess(CatalogApiResponse<T> response, String operation) {
        if (response == null) {
            throw new IllegalStateException("catalog response is null: operation=" + operation);
        }
        if (!Boolean.TRUE.equals(response.success())) {
            String message = response.error() == null ? "unknown error" : response.error().message();
            throw new IllegalStateException("catalog response failed: operation=" + operation + ", message=" + message);
        }
        return response.data();
    }

    private <T> T executeWithFallback(Supplier<T> remoteCall, Supplier<T> localCall, String operation) {
        try {
            return remoteCall.get();
        } catch (Exception exception) {
            if (catalogClientProperties.getRemote().isFallbackToLocal()) {
                log.warn("catalog remote failed; fallback to local. op={}, message={}", operation, exception.getMessage());
                return localCall.get();
            }
            throw exception;
        }
    }

    private record CatalogApiResponse<T>(Boolean success, T data, ApiError error) {
    }

    private record ApiError(Integer code, String message) {
    }

    private record SlicePayload<T>(List<T> content, boolean hasNext) {
    }

    private record ShopPayload(
            Long itemId,
            String itemName,
            String category,
            int price,
            String itemImageUrl,
            String brandName
    ) {
    }

    private record CatalogItemSnapshotPayload(
            Long itemId,
            String itemName,
            int itemPrice,
            String itemImageUrl,
            String brandName,
            String category,
            String optionName
    ) {
    }
}
