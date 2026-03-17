package kcs.funding.fundingboost.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kcs.funding.fundingboost.domain.dto.response.admin.AdminDashboardDto;
import kcs.funding.fundingboost.domain.entity.FriendPayBarcodeTokenStatus;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.entity.member.MemberRole;
import kcs.funding.fundingboost.domain.repository.FriendPayBarcodeTokenRepository;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.OrderRepository;
import kcs.funding.fundingboost.domain.repository.contributor.ContributorRepository;
import kcs.funding.fundingboost.domain.repository.funding.FundingRepository;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {
    private static final String FUNDING_SIGNAL_SQL = "select item_id, count(*) from funding_item group by item_id";
    private static final String ORDER_SIGNAL_SQL = "select item_id, coalesce(sum(quantity), 0) from order_item group by item_id";
    private static final String WISHLIST_SIGNAL_SQL = "select item_id, count(*) from bookmark group by item_id";


    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ContributorRepository contributorRepository;

    @Mock
    private FriendPayBarcodeTokenRepository friendPayBarcodeTokenRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query fundingSignalQuery;

    @Mock
    private Query orderSignalQuery;

    @Mock
    private Query wishlistSignalQuery;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private Map<Long, Item> itemsById;

    @BeforeEach
    void setUp() throws Exception {
        itemsById = new HashMap<>();
        itemsById.put(1L, createItem(1L, "item-1", "뷰티"));
        itemsById.put(2L, createItem(2L, "item-2", "식품"));
        itemsById.put(3L, createItem(3L, "item-3", "테크"));

        when(memberRepository.count()).thenReturn(0L);
        when(memberRepository.countByMemberRole(any(MemberRole.class))).thenReturn(0L);
        when(fundingRepository.count()).thenReturn(0L);
        when(fundingRepository.countByFundingStatus(true)).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.countByCreatedDateBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(orderRepository.sumTotalPrice()).thenReturn(0L);
        when(orderRepository.sumTotalPriceByCreatedDateBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(fundingRepository.findExpiringFundings(any(LocalDateTime.class), any(LocalDateTime.class), any())).thenReturn(List.of());
        when(memberRepository.findRecentMembers(any())).thenReturn(List.of());
        when(friendPayBarcodeTokenRepository.findRecentTokens(any())).thenReturn(List.of());
        when(friendPayBarcodeTokenRepository.countByTokenStatus(any(FriendPayBarcodeTokenStatus.class))).thenReturn(0L);

        when(entityManager.createNativeQuery(eq(FUNDING_SIGNAL_SQL)))
                .thenReturn(fundingSignalQuery);
        when(entityManager.createNativeQuery(eq(ORDER_SIGNAL_SQL)))
                .thenReturn(orderSignalQuery);
        when(entityManager.createNativeQuery(eq(WISHLIST_SIGNAL_SQL)))
                .thenReturn(wishlistSignalQuery);

        when(fundingSignalQuery.getResultList()).thenReturn(List.of(
                new Object[]{1L, 1L},
                new Object[]{2L, 1L}
        ));
        when(orderSignalQuery.getResultList()).thenReturn(List.of());
        when(wishlistSignalQuery.getResultList()).thenReturn(List.of(
                new Object[]{1L, 1L},
                new Object[]{2L, 10L},
                new Object[]{3L, 3L}
        ));

        when(itemRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<Item> items = new ArrayList<>();
            for (Long id : ids) {
                Item item = itemsById.get(id);
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        });
    }

    @Test
    void getDashboard_sortsTopItemsByDescendingScore() {
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        assertThat(dashboard.topItems().stream().map(topItem -> topItem.itemId()).toList())
                .containsExactly(2L, 1L, 3L);
        assertThat(dashboard.topItems().stream().map(topItem -> topItem.score()).toList())
                .containsExactly(13L, 4L, 3L);
    }

    @Test
    void getDashboard_filtersMissingItemsBeforeTopLimit() {
        List<Object[]> fundingSignals = new ArrayList<>();
        for (long missingItemId = 1000L; missingItemId <= 1010L; missingItemId++) {
            fundingSignals.add(new Object[]{missingItemId, 10L});
        }
        fundingSignals.add(new Object[]{1L, 1L});

        when(fundingSignalQuery.getResultList()).thenReturn(fundingSignals);
        when(orderSignalQuery.getResultList()).thenReturn(List.of());
        when(wishlistSignalQuery.getResultList()).thenReturn(List.of());

        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        assertThat(dashboard.topItems()).hasSize(1);
        assertThat(dashboard.topItems().get(0).itemId()).isEqualTo(1L);
        assertThat(dashboard.topItems().get(0).score()).isEqualTo(3L);
    }

    private Item createItem(Long itemId, String itemName, String category) throws Exception {
        Item item = Item.createItem(
                itemName,
                1000,
                "https://example.com/" + itemId + ".jpg",
                "브랜드",
                category,
                "기본 옵션"
        );

        Field idField = Item.class.getDeclaredField("itemId");
        idField.setAccessible(true);
        idField.set(item, itemId);
        return item;
    }
}
