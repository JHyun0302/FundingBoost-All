package kcs.funding.fundingboost.catalog.infra;

import java.util.List;
import java.util.Optional;
import kcs.funding.fundingboost.catalog.application.CatalogItemReader;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component("localCatalogItemReader")
@RequiredArgsConstructor
public class LocalCatalogItemReader implements CatalogItemReader {

    private final ItemRepository itemRepository;

    @Override
    public Optional<Item> findById(Long itemId) {
        return itemRepository.findById(itemId);
    }

    @Override
    public List<Item> findAllById(Iterable<Long> itemIds) {
        return itemRepository.findAllById(itemIds);
    }

    @Override
    public List<Item> findItemsByItemIds(List<Long> itemIds) {
        return itemRepository.findItemsByItemIds(itemIds);
    }

    @Override
    public Slice<Item> findItems(Long lastItemId, String category, String keyword, Pageable pageable) {
        return itemRepository.findItems(lastItemId, category, keyword, pageable);
    }

    @Override
    public Slice<Item> findItemsBySlice(Long lastItemId, Pageable pageable) {
        return itemRepository.findItemsBySlice(lastItemId, pageable);
    }

    @Override
    public List<String> findDistinctCategories() {
        return itemRepository.findDistinctCategories();
    }
}
