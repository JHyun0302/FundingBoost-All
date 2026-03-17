package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_ITEM;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.MyPageMemberDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.wishList.BookmarkItemDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.wishList.MyBookmarkListDto;
import kcs.funding.fundingboost.domain.entity.Bookmark;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.bookmark.BookmarkRepository;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("BookmarkService")
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    @Counted("BookmarkService.getMyBookmark")
    public MyBookmarkListDto getMyBookmark(Long memberId) {

        List<Bookmark> bookmarks = bookmarkRepository.findAllByMemberId(memberId);
        List<Long> itemIds = bookmarks.stream()
                .map(bookmark -> bookmark.getItem().getItemId())
                .distinct()
                .toList();

        List<Item> items = Optional.ofNullable(itemRepository.findAllById(itemIds))
                .orElse(Collections.emptyList());

        Map<Long, Item> itemById = items.stream()
                .collect(Collectors.toMap(Item::getItemId, Function.identity()));

        List<BookmarkItemDto> bookmarkItemDtos = bookmarks.stream()
                .map(bookmark -> {
                    Long itemId = bookmark.getItem().getItemId();
                    Item item = itemById.get(itemId);
                    if (item == null) {
                        return BookmarkItemDto.fromEntity(bookmark.getItem());
                    }
                    return BookmarkItemDto.fromEntity(item);
                })
                .toList();

        MyPageMemberDto myPageMemberDto = MyPageMemberDto.fromEntity(
                memberRepository.findById(memberId).orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER)));

        return MyBookmarkListDto.fromEntity(myPageMemberDto, bookmarkItemDtos);
    }

    @Counted("BookmarkService.toggleItemLike")
    @Transactional
    public CommonSuccessDto toggleItemLike(Long memberId, Long itemId) {
        Optional<Bookmark> optionalBookmark = bookmarkRepository.findBookmarkByMemberAndItem(memberId, itemId);

        if (optionalBookmark.isPresent()) {
            bookmarkRepository.delete(optionalBookmark.get());
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));

            Bookmark bookmark = Bookmark.createBookmark(member, item);
            bookmarkRepository.save(bookmark);
        }
        return CommonSuccessDto.fromEntity(true);
    }
}
