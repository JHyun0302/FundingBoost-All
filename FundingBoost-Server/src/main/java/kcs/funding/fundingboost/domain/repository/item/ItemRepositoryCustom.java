package kcs.funding.fundingboost.domain.repository.item;

import kcs.funding.fundingboost.domain.entity.Item;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;

public interface ItemRepositoryCustom {

    Slice<Item> findItemsByCategory(Long lastItemId, @Param("category") String category, Pageable pageable);

    Slice<Item> findItemsBySlice(Long lastItemId, Pageable pageable);

    Slice<Item> findItems(Long lastItemId, String category, String keyword, Pageable pageable);

    List<String> findDistinctCategories();
}
