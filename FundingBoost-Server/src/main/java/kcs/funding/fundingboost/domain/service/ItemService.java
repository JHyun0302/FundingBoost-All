package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_ITEM;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import kcs.funding.fundingboost.domain.dto.response.shopping.ShopDto;
import kcs.funding.fundingboost.domain.dto.response.shoppingDetail.ItemDetailDto;
import kcs.funding.fundingboost.domain.entity.Bookmark;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.bookmark.BookmarkRepository;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("ItemService")
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final BookmarkRepository bookmarkRepository;

    @Counted("ItemService.getItems")
    public Slice<ShopDto> getItems(Long lastItemId, String category, String keyword, Pageable pageable) {
        Slice<Item> items = itemRepository.findItems(lastItemId, category, keyword, pageable);
        List<ShopDto> shopDtoList = items.stream().map(ShopDto::fromEntity).toList();
        return new SliceImpl<>(shopDtoList, pageable, items.hasNext());
    }

    @Counted("ItemService.getItemCategories")
    public List<String> getItemCategories() {
        return itemRepository.findDistinctCategories();
    }

    @Counted("ItemService.getItemDetail")
    public ItemDetailDto getItemDetail(Long memberId, Long itemId) {
        if (memberId != null) {
            Optional<Bookmark> bookmark = bookmarkRepository.findBookmarkByMemberAndItem(memberId, itemId);
            if (bookmark.isPresent()) {
                return ItemDetailDto.fromEntity(bookmark.get().getItem(), true);
            } else {
                Item item = itemRepository.findById(itemId)
                        .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));
                return ItemDetailDto.fromEntity(item, false);
            }
        }
        return ItemDetailDto.fromEntity(itemRepository.findById(itemId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM)), false);
    }
}
