package kcs.funding.fundingboost.catalog.application;

import java.util.List;
import java.util.Optional;
import kcs.funding.fundingboost.domain.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CatalogItemReader {

    Optional<Item> findById(Long itemId);

    List<Item> findAllById(Iterable<Long> itemIds);

    List<Item> findItemsByItemIds(List<Long> itemIds);

    Slice<Item> findItems(Long lastItemId, String category, String keyword, Pageable pageable);

    Slice<Item> findItemsBySlice(Long lastItemId, Pageable pageable);

    List<String> findDistinctCategories();
}
