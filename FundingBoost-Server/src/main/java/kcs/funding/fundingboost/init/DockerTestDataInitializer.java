package kcs.funding.fundingboost.init;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;
import kcs.funding.fundingboost.domain.entity.Bookmark;
import kcs.funding.fundingboost.domain.entity.Contributor;
import kcs.funding.fundingboost.domain.entity.Delivery;
import kcs.funding.fundingboost.domain.entity.Funding;
import kcs.funding.fundingboost.domain.entity.FundingItem;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.entity.Order;
import kcs.funding.fundingboost.domain.entity.OrderItem;
import kcs.funding.fundingboost.domain.entity.Relationship;
import kcs.funding.fundingboost.domain.entity.Review;
import kcs.funding.fundingboost.domain.entity.Tag;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.entity.member.MemberGender;
import kcs.funding.fundingboost.domain.entity.member.MemberRole;
import kcs.funding.fundingboost.domain.repository.DeliveryRepository;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.OrderRepository;
import kcs.funding.fundingboost.domain.repository.bookmark.BookmarkRepository;
import kcs.funding.fundingboost.domain.repository.contributor.ContributorRepository;
import kcs.funding.fundingboost.domain.repository.funding.FundingRepository;
import kcs.funding.fundingboost.domain.repository.fundingItem.FundingItemRepository;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import kcs.funding.fundingboost.domain.repository.orderItem.OrderItemRepository;
import kcs.funding.fundingboost.domain.repository.relationship.RelationshipRepository;
import kcs.funding.fundingboost.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.testdata", name = "enabled", havingValue = "true")
@Slf4j
public class DockerTestDataInitializer {

    private static final String QA_PASSWORD = "Test1234!";
    private static final String ADMIN_SEED_EMAIL = "qa1@fundingboost.test";
    private static final long RANDOM_SEED = 20260228L;
    private static final int DEFAULT_SEED_POINT = 50_000;
    private static final List<SeedMemberDefinition> SEED_MEMBERS = List.of(
            new SeedMemberDefinition("마리오", "qa1@fundingboost.test", 100_000, MemberGender.MAN, "seed_qa1", "/test-members/mario.png"),
            new SeedMemberDefinition("루이지", "qa2@fundingboost.test", 100_000, MemberGender.MAN, "seed_qa2", "/test-members/luigi.png"),
            new SeedMemberDefinition("피치공주", "qa3@fundingboost.test", DEFAULT_SEED_POINT, MemberGender.WOMAN, "seed_qa3", "/test-members/princess-peach.png"),
            new SeedMemberDefinition("키노피오", "qa4@fundingboost.test", DEFAULT_SEED_POINT, MemberGender.MAN, "seed_qa4", "/test-members/toad.png"),
            new SeedMemberDefinition("요시", "qa5@fundingboost.test", DEFAULT_SEED_POINT, MemberGender.WOMAN, "seed_qa5", "/test-members/yoshi.png")
    );
    private static final List<Tag> FUNDING_TAGS = List.of(
            Tag.BIRTHDAY, Tag.GRADUATE, Tag.ETC, Tag.BIRTHDAY, Tag.ETC
    );
    private static final int TARGET_HISTORY_FUNDING_COUNT = 20;
    private static final int TARGET_REVIEW_COUNT = 12;
    private static final List<String> REVIEW_COMMENTS = List.of(
            "배송이 빨랐고 상품 상태도 만족스러웠어요.",
            "선물용으로 좋았습니다. 다음에도 다시 구매할 것 같아요.",
            "사진과 거의 동일했고 포장 상태가 깔끔했습니다.",
            "가성비가 좋고 만족도가 높아요.",
            "실사용해보니 기대한 만큼 괜찮았습니다."
    );

    private final MemberRepository memberRepository;
    private final RelationshipRepository relationshipRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FundingRepository fundingRepository;
    private final FundingItemRepository fundingItemRepository;
    private final ContributorRepository contributorRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    private volatile boolean initialized = false;

    @Scheduled(
            initialDelayString = "${app.testdata.retry-delay-ms:10000}",
            fixedDelayString = "${app.testdata.retry-delay-ms:10000}"
    )
    @Transactional
    public synchronized void bootstrap() {
        if (initialized) {
            return;
        }

        List<Item> items;
        try {
            items = itemRepository.findAll(Sort.by(Sort.Order.asc("category"), Sort.Order.asc("itemId")));
        } catch (RuntimeException ex) {
            log.info("item 스키마 준비 전입니다. 테스트 데이터 시드를 대기합니다.");
            return;
        }

        List<Member> existingSeedMembers = loadSeedMembers();
        if (!existingSeedMembers.isEmpty()) {
            if (existingSeedMembers.size() != SEED_MEMBERS.size()) {
                log.warn("테스트 계정이 일부만 존재합니다. 수동 정리 후 재기동이 필요합니다. 발견된 계정 수={}", existingSeedMembers.size());
                return;
            }
            refreshSeedMemberProfiles();
            backfillSeedDeliveries(existingSeedMembers);
            backfillSeedOrders(existingSeedMembers);
            ensureMarioFundedOrder(existingSeedMembers.get(0));
            ensureHistoricalFundings(existingSeedMembers, items);
            ensureSeedReviews(existingSeedMembers, items);
            initialized = true;
            log.info("테스트 데이터가 이미 존재합니다. 누락된 보조 데이터만 보정했습니다.");
            return;
        }

        List<List<Item>> bookmarkPools = buildBookmarkPools(items);
        if (bookmarkPools == null) {
            return;
        }

        List<Member> members = seedMembers();
        createRelationships(members);

        Map<Member, List<Item>> memberBookmarks = createBookmarks(members, bookmarkPools);
        List<Funding> fundings = createFundings(members, memberBookmarks);
        createContributors(members, fundings);
        createDeliveriesAndOrders(members, memberBookmarks);
        ensureMarioFundedOrder(members.get(0));
        ensureHistoricalFundings(members, items);
        ensureSeedReviews(members, items);

        initialized = true;
        log.info("Docker 테스트 데이터 생성 완료: members={}, bookmarks={}, fundings={}",
                members.size(),
                memberBookmarks.values().stream().mapToInt(List::size).sum(),
                fundings.size());
    }

    private List<Member> seedMembers() {
        String encodedPassword = passwordEncoder.encode(QA_PASSWORD);
        List<Member> members = new ArrayList<>();

        for (SeedMemberDefinition seedMember : SEED_MEMBERS) {
            Member member = Member.createMemberWithPoint(
                    seedMember.name(),
                    seedMember.email(),
                    encodedPassword,
                    seedMember.profileImgUrl(),
                    seedMember.initialPoint(),
                    seedMember.kakaoId(),
                    seedMember.gender()
            );
            if (isAdminSeed(seedMember.email())) {
                member.changeMemberRole(MemberRole.ROLE_ADMIN);
            }
            members.add(member);
        }

        return memberRepository.saveAll(members);
    }

    private void refreshSeedMemberProfiles() {
        for (SeedMemberDefinition seedMember : SEED_MEMBERS) {
            memberRepository.findByEmail(seedMember.email()).ifPresent(existingMember -> {
                if (!seedMember.profileImgUrl().equals(existingMember.getProfileImgUrl())) {
                    existingMember.changeProfileImgUrl(seedMember.profileImgUrl());
                }
                if (existingMember.getGender() != seedMember.gender()) {
                    existingMember.changeGender(seedMember.gender());
                }
                MemberRole expectedRole = isAdminSeed(seedMember.email())
                        ? MemberRole.ROLE_ADMIN
                        : MemberRole.ROLE_USER;
                if (existingMember.getMemberRole() != expectedRole) {
                    existingMember.changeMemberRole(expectedRole);
                }
            });
        }
    }

    private boolean isAdminSeed(String email) {
        return ADMIN_SEED_EMAIL.equalsIgnoreCase(email);
    }

    private List<Member> loadSeedMembers() {
        List<Member> members = new ArrayList<>();
        for (SeedMemberDefinition seedMember : SEED_MEMBERS) {
            memberRepository.findByEmail(seedMember.email()).ifPresent(members::add);
        }
        return members;
    }

    private void createRelationships(List<Member> members) {
        List<Relationship> relationships = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                relationships.addAll(Relationship.createRelationships(members.get(i), members.get(j)));
            }
        }

        relationshipRepository.saveAll(relationships);
    }

    private Map<Member, List<Item>> createBookmarks(List<Member> members, List<List<Item>> bookmarkPools) {
        Map<Member, List<Item>> memberBookmarks = new LinkedHashMap<>();
        List<Bookmark> bookmarks = new ArrayList<>();

        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            Member member = members.get(memberIndex);
            List<Item> selectedItems = new ArrayList<>();

            for (int categoryIndex = 0; categoryIndex < bookmarkPools.size(); categoryIndex++) {
                List<Item> itemsInCategory = new ArrayList<>(bookmarkPools.get(categoryIndex));
                Collections.shuffle(itemsInCategory, new Random(RANDOM_SEED + (memberIndex * 31L) + (categoryIndex * 17L)));

                selectedItems.add(itemsInCategory.get(0));
                selectedItems.add(itemsInCategory.get(1));
            }

            memberBookmarks.put(member, selectedItems);
            for (Item selectedItem : selectedItems) {
                bookmarks.add(Bookmark.createBookmark(member, selectedItem));
            }
        }

        bookmarkRepository.saveAll(bookmarks);
        return memberBookmarks;
    }

    private void createDeliveriesAndOrders(List<Member> members, Map<Member, List<Item>> memberBookmarks) {
        List<Delivery> deliveries = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            deliveries.add(Delivery.createDelivery(
                    "서울특별시 송파구 올림픽로 " + (300 + i),
                    String.format("010-9000-10%02d", i + 1),
                    member.getNickName(),
                    buildPostalCode(i),
                    buildDeliveryMemo(i),
                    member
            ));
        }

        deliveries = deliveryRepository.saveAll(deliveries);

        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            Delivery delivery = deliveries.get(i);
            List<Item> bookmarkedItems = memberBookmarks.get(member);
            Item orderTarget = bookmarkedItems.get((i + 2) % bookmarkedItems.size());

            Order order = orderRepository.save(Order.createOrder(member, delivery));
            int quantity = (i % 2) + 1;
            OrderItem orderItem = OrderItem.createOrderItem(order, orderTarget, quantity);
            int pointUsedAmount = i == 0 ? 0 : Math.min(1_000 * ((i % 3) + 1), order.getTotalPrice() / 3);
            int directPaidAmount = Math.max(order.getTotalPrice() - pointUsedAmount, 0);
            order.applyPaymentBreakdown(pointUsedAmount, directPaidAmount, 0, null);
            orderItemRepository.save(orderItem);
        }
    }

    private void backfillSeedOrders(List<Member> members) {
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            Member member = members.get(memberIndex);
            Map<Long, Order> ordersById = new LinkedHashMap<>();

            orderItemRepository.findLastOrderByMemberId(member.getMemberId()).forEach(orderItem ->
                    ordersById.putIfAbsent(orderItem.getOrder().getOrderId(), orderItem.getOrder()));

            int orderIndex = 0;
            for (Order order : ordersById.values()) {
                if (order.getSourceFundingId() != null) {
                    orderIndex++;
                    continue;
                }

                if (order.getPointUsedAmount() == 0
                        && order.getDirectPaidAmount() == 0
                        && order.getFundingSupportedAmount() == 0) {
                    int pointUsedAmount = memberIndex == 0 && orderIndex == 0
                            ? 0
                            : Math.min(1_000 * (((memberIndex + orderIndex) % 3) + 1), order.getTotalPrice() / 3);
                    int directPaidAmount = Math.max(order.getTotalPrice() - pointUsedAmount, 0);
                    order.applyPaymentBreakdown(pointUsedAmount, directPaidAmount, 0, null);
                }
                orderIndex++;
            }
        }
    }

    private void backfillSeedDeliveries(List<Member> members) {
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            Member member = members.get(memberIndex);
            String postalCode = buildPostalCode(memberIndex);
            String deliveryMemo = buildDeliveryMemo(memberIndex);
            deliveryRepository.findAllByMemberId(member.getMemberId())
                    .forEach(delivery -> {
                        if (postalCode.equals(delivery.getPostalCode()) && deliveryMemo.equals(delivery.getDeliveryMemo())) {
                            return;
                        }
                        delivery.updateExtraInfo(postalCode, deliveryMemo);
                    });
        }
    }

    private void ensureMarioFundedOrder(Member mario) {
        boolean hasMarioFundedOrder = orderItemRepository.findLastOrderByMemberId(mario.getMemberId()).stream()
                .anyMatch(orderItem -> orderItem.getOrder().getSourceFundingId() != null);

        if (hasMarioFundedOrder) {
            return;
        }

        Funding marioFunding = fundingRepository.findFundingInfo(mario.getMemberId()).orElse(null);
        if (marioFunding == null || marioFunding.getFundingItems().isEmpty()) {
            return;
        }

        List<Delivery> deliveries = deliveryRepository.findAllByMemberId(mario.getMemberId());
        Delivery delivery = deliveries.isEmpty()
                ? deliveryRepository.save(Delivery.createDelivery(
                "서울특별시 송파구 올림픽로 999",
                "010-9000-1099",
                mario.getNickName(),
                "05555",
                "부재 시 문 앞에 두고 사진 부탁드려요.",
                mario
        ))
                : deliveries.get(0);

        Order fundedOrder = Order.createOrder(mario, delivery);
        OrderItem fundedOrderItem = OrderItem.createOrderItem(
                fundedOrder,
                marioFunding.getFundingItems().get(0).getItem(),
                1
        );

        int pointUsedAmount = Math.min(3_000, fundedOrder.getTotalPrice() / 5);
        int fundingSupportedAmount = Math.min(marioFunding.getCollectPrice(), fundedOrder.getTotalPrice());
        int directPaidAmount = Math.max(fundedOrder.getTotalPrice() - pointUsedAmount - fundingSupportedAmount, 0);
        fundedOrder.applyPaymentBreakdown(
                pointUsedAmount,
                directPaidAmount,
                fundingSupportedAmount,
                marioFunding.getFundingId()
        );

        orderRepository.save(fundedOrder);
        orderItemRepository.save(fundedOrderItem);
    }

    private List<Funding> createFundings(List<Member> members, Map<Member, List<Item>> memberBookmarks) {
        List<Funding> fundings = new ArrayList<>();
        Set<Long> usedFundingItemIds = new HashSet<>();

        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            List<Item> bookmarkedItems = memberBookmarks.get(member);
            Item fundingTarget = pickUniqueFundingTarget(bookmarkedItems, (i + 5) % bookmarkedItems.size(), usedFundingItemIds);
            usedFundingItemIds.add(fundingTarget.getItemId());

            Funding funding = fundingRepository.save(Funding.createFunding(
                    member,
                    member.getNickName() + "님의 테스트 펀딩",
                    FUNDING_TAGS.get(i % FUNDING_TAGS.size()),
                    LocalDateTime.now().plusDays(14 + i)
            ));
            fundingItemRepository.save(FundingItem.createFundingItem(funding, fundingTarget, 1));
            fundings.add(funding);
        }

        return fundings;
    }

    private String buildPostalCode(int index) {
        return String.format("05%03d", 110 + index);
    }

    private String buildDeliveryMemo(int index) {
        List<String> memos = List.of(
                "문 앞에 두고 벨 눌러주세요.",
                "경비실에 맡겨주세요.",
                "부재 시 택배함에 넣어주세요.",
                "배송 전 연락 부탁드려요.",
                "파손 주의 스티커 부탁드립니다."
        );
        return memos.get(index % memos.size());
    }

    private Item pickUniqueFundingTarget(List<Item> bookmarkedItems, int preferredIndex, Set<Long> usedFundingItemIds) {
        for (int offset = 0; offset < bookmarkedItems.size(); offset++) {
            Item candidate = bookmarkedItems.get((preferredIndex + offset) % bookmarkedItems.size());
            if (!usedFundingItemIds.contains(candidate.getItemId())) {
                return candidate;
            }
        }

        return bookmarkedItems.get(preferredIndex);
    }

    private void createContributors(List<Member> members, List<Funding> fundings) {
        List<Contributor> contributors = new ArrayList<>();

        for (int fundingIndex = 0; fundingIndex < fundings.size(); fundingIndex++) {
            Funding funding = fundings.get(fundingIndex);

            for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                if (fundingIndex == memberIndex) {
                    continue;
                }

                int contributorPrice = 3_000 + ((fundingIndex + memberIndex) % 4) * 1_000;
                contributors.add(Contributor.createContributor(contributorPrice, members.get(memberIndex), funding));
            }
        }

        contributorRepository.saveAll(contributors);
    }

    private void ensureHistoricalFundings(List<Member> members, List<Item> items) {
        if (members.isEmpty() || items.isEmpty()) {
            return;
        }

        Member mario = members.get(0);
        List<Item> candidateItems = items.stream()
                .sorted(Comparator.comparing(Item::getItemId))
                .limit(TARGET_HISTORY_FUNDING_COUNT)
                .toList();

        if (candidateItems.isEmpty()) {
            return;
        }

        int targetHistoryCount = Math.min(TARGET_HISTORY_FUNDING_COUNT, candidateItems.size());
        List<Funding> existingHistory = new ArrayList<>(fundingRepository.findFundingByMemberId(mario.getMemberId()));
        Set<Long> existingHistoryItemIds = existingHistory.stream()
                .flatMap(funding -> funding.getFundingItems().stream())
                .map(fundingItem -> fundingItem.getItem().getItemId())
                .collect(Collectors.toSet());

        boolean hasUniqueHistoryItems = existingHistory.size() == existingHistoryItemIds.size();
        boolean alreadyAligned = existingHistory.size() == targetHistoryCount && hasUniqueHistoryItems;

        if (alreadyAligned) {
            return;
        }

        if (!existingHistory.isEmpty()) {
            fundingRepository.deleteAll(existingHistory);
        }

        List<Funding> createdFundings = new ArrayList<>();
        List<Contributor> createdContributors = new ArrayList<>();

        for (int i = 0; i < targetHistoryCount; i++) {
            Item targetItem = candidateItems.get(i);
            Funding historyFunding = fundingRepository.save(Funding.createFundingForTest(
                    mario,
                    "마리오 지난 펀딩 테스트 #" + (i + 1),
                    FUNDING_TAGS.get(i % FUNDING_TAGS.size()),
                    0,
                    LocalDateTime.now().minusDays(7 + i),
                    false
            ));

            FundingItem fundingItem = FundingItem.createFundingItem(historyFunding, targetItem, 1);
            fundingItemRepository.save(fundingItem);

            List<Member> contributors = members.subList(1, members.size());
            int remainingPrice = targetItem.getItemPrice();

            for (int contributorIndex = 0; contributorIndex < contributors.size(); contributorIndex++) {
                int contributorPrice;
                if (contributorIndex == contributors.size() - 1) {
                    contributorPrice = remainingPrice;
                } else {
                    contributorPrice = targetItem.getItemPrice() / contributors.size();
                    remainingPrice -= contributorPrice;
                }
                createdContributors.add(Contributor.createContributor(contributorPrice, contributors.get(contributorIndex), historyFunding));
            }

            fundingItem.completeFunding();
            fundingItem.finishFundingItem();
            createdFundings.add(historyFunding);
        }

        contributorRepository.saveAll(createdContributors);
        log.info("마리오 지난 펀딩 이력 재구성 완료: 총 생성={}", createdFundings.size());
    }

    private void ensureSeedReviews(List<Member> members, List<Item> items) {
        if (members.isEmpty() || items.isEmpty()) {
            return;
        }

        Member mario = members.get(0);
        List<Review> existingReviews = reviewRepository.findAllByMemberIdOrderByReviewIdDesc(mario.getMemberId());

        List<Item> sortedItems = items.stream()
                .sorted(Comparator.comparing(Item::getItemId))
                .toList();
        int startIndex = sortedItems.size() > 20 ? 20 : 0;
        List<Item> candidateItems = sortedItems.stream()
                .skip(startIndex)
                .limit(TARGET_REVIEW_COUNT)
                .toList();

        if (candidateItems.isEmpty()) {
            return;
        }

        int targetReviewCount = Math.min(TARGET_REVIEW_COUNT, candidateItems.size());
        boolean alreadyAligned = existingReviews.size() == targetReviewCount
                && existingReviews.stream().map(review -> review.getItem().getItemId()).distinct().count() == targetReviewCount;

        if (alreadyAligned) {
            return;
        }

        if (!existingReviews.isEmpty()) {
            reviewRepository.deleteAll(existingReviews);
        }

        List<Review> reviews = new ArrayList<>();
        for (int i = 0; i < targetReviewCount; i++) {
            Item item = candidateItems.get(i);
            int rating = 5 - (i % 2);
            String content = REVIEW_COMMENTS.get(i % REVIEW_COMMENTS.size());
            reviews.add(Review.createReview(mario, item, rating, content));
        }

        reviewRepository.saveAll(reviews);
        log.info("마리오 리뷰 데이터 재구성 완료: 총 생성={}", reviews.size());
    }

    private List<List<Item>> buildBookmarkPools(List<Item> items) {
        if (items.isEmpty()) {
            log.info("item 데이터가 아직 비어 있어 테스트 데이터 생성을 대기합니다.");
            return null;
        }

        LinkedHashMap<String, List<Item>> categoryMap = items.stream()
                .filter(item -> item.getCategory() != null && !item.getCategory().isBlank())
                .collect(Collectors.groupingBy(
                        Item::getCategory,
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));

        List<List<Item>> bookmarkPools = categoryMap.values().stream()
                .filter(categoryItems -> categoryItems.size() >= 2)
                .limit(5)
                .toList();

        if (bookmarkPools.size() < 5) {
            log.info("카테고리별 2개 아이템 구성을 위해 5개 카테고리 이상이 필요합니다. 현재 준비된 카테고리 수={}",
                    bookmarkPools.size());
            return null;
        }

        return bookmarkPools;
    }

    private record SeedMemberDefinition(
            String name,
            String email,
            int initialPoint,
            MemberGender gender,
            String kakaoId,
            String profileImgUrl
    ) {
    }
}
