package kcs.funding.crawler.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kcs.funding.crawler.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByProductId(String productId);

    List<Item> findAllByItemIdIn(List<Long> itemIds);

    // brand+category+imageUrl 이 이미 있으면 "같은 아이템"으로 보고 insert 스킵
    boolean existsByBrandNameAndCategoryAndAndItemImageUrl(String brandName, String category, String itemImageUrl);

    // 오래된 데이터 일괄 삭제
    int deleteByModifiedDateBefore(LocalDateTime threshold);

    @Query("""
            select i
            from Item i
            where (:lastItemId is null or i.itemId < :lastItemId)
              and (:category is null or :category = '' or i.category = :category)
              and (
                   :keyword is null or :keyword = ''
                   or lower(i.itemName) like lower(concat('%', :keyword, '%'))
                   or lower(i.brandName) like lower(concat('%', :keyword, '%'))
                   or lower(i.category) like lower(concat('%', :keyword, '%'))
              )
            order by i.itemId desc
            """)
    List<Item> findForCursorPaging(
            @Param("lastItemId") Long lastItemId,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select i
            from Item i
            where :category is null or :category = '' or i.category = :category
            """)
    Slice<Item> findSliceByCategory(
            @Param("category") String category,
            Pageable pageable
    );

    @Query("""
            select i
            from Item i
            where lower(i.itemName) like lower(concat('%', :keyword, '%'))
               or lower(i.brandName) like lower(concat('%', :keyword, '%'))
               or lower(i.category) like lower(concat('%', :keyword, '%'))
            """)
    Slice<Item> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select distinct i.category
            from Item i
            where i.category is not null and i.category <> ''
            order by i.category asc
            """)
    List<String> findDistinctCategories();
}
