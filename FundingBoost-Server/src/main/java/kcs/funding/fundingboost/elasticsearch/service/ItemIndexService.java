package kcs.funding.fundingboost.elasticsearch.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_ITEM;
import static kcs.funding.fundingboost.domain.entity.QBookmark.bookmark;
import static kcs.funding.fundingboost.domain.entity.QFundingItem.fundingItem;
import static kcs.funding.fundingboost.domain.entity.QItem.item;
import static kcs.funding.fundingboost.domain.entity.QOrderItem.orderItem;
import static kcs.funding.fundingboost.domain.entity.member.QMember.member;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.core.annotation.Counted;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import kcs.funding.fundingboost.domain.dto.response.home.HomeRankingItemDto;
import kcs.funding.fundingboost.domain.dto.response.shopping.ShopDto;
import kcs.funding.fundingboost.domain.dto.response.shoppingDetail.ItemDetailDto;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.entity.member.MemberGender;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.bookmark.BookmarkRepository;
import kcs.funding.fundingboost.domain.repository.item.ItemRepository;
import kcs.funding.fundingboost.elasticsearch.entity.SearchKeywordLog;
import kcs.funding.fundingboost.elasticsearch.index.ItemIndex;
import kcs.funding.fundingboost.elasticsearch.repository.ItemIndexRepository;
import kcs.funding.fundingboost.elasticsearch.repository.SearchKeywordLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemIndexService {
    private static final Duration INDEX_SYNC_RETRY_INTERVAL = Duration.ofMinutes(5);
    private static final int MAX_SEARCH_EXPANSION = 16;
    private static final int MIN_INTENT_KEYWORD_HITS = 3;
    private static final int MIN_SEARCH_KEYWORD_LENGTH = 2;
    private static final int MAX_AUTO_DICTIONARY_TOP_N = 500;
    private static final int MAX_AUTO_SYNONYMS_PER_KEYWORD = 10;
    private static final Set<String> HIGH_LEVEL_INTENTS = Set.of("뷰티", "디지털", "식품", "패션", "리빙", "스포츠");
    private static final Set<String> CATEGORY_FIRST_INTENTS = Set.of(
            "뷰티", "디지털", "식품", "패션", "리빙", "스포츠",
            "옷", "화장품", "배터리", "과자", "간식", "장난감", "거울", "핸드폰", "휴대폰", "스마트폰", "쓰레기통", "휴지통",
            "전자제품", "가전",
            "생활용품", "인테리어", "주방용품", "문구", "학용품", "반려용품", "유아용품",
            "디저트", "음료", "빵", "초콜릿", "의류", "신발", "지갑", "액세서리", "악세서리", "주얼리",
            "스킨케어", "메이크업", "향수", "핸드크림", "캠핑", "운동용품", "자전거"
    );
    private static final Set<String> CATEGORY_FALLBACK_INTENTS = Set.of(
            "뷰티", "디지털", "식품", "패션", "리빙", "스포츠",
            "옷", "화장품", "배터리", "과자", "간식", "가방", "텀블러", "고기",
            "장난감", "거울", "핸드폰", "휴대폰", "스마트폰", "쓰레기통", "휴지통",
            "전자제품", "가전",
            "생활용품", "인테리어", "주방용품", "문구", "학용품", "반려용품", "유아용품",
            "디저트", "음료", "빵", "초콜릿", "의류", "신발", "지갑", "액세서리", "악세서리", "주얼리",
            "스킨케어", "메이크업", "향수", "핸드크림", "캠핑", "운동용품", "자전거"
    );
    private static final Set<String> SYNONYM_FIRST_KEYWORDS = Set.of(
            "반지", "마이크", "옷", "커피", "화장품", "배터리", "과자", "간식", "가방", "텀블러", "고기",
            "장난감", "거울", "핸드폰", "휴대폰", "스마트폰", "쓰레기통", "휴지통",
            "전자제품", "가전", "노트북", "태블릿", "이어폰", "헤드폰", "키보드", "마우스",
            "생활용품", "인테리어", "주방용품", "문구", "학용품", "반려용품", "유아용품",
            "디저트", "음료", "빵", "초콜릿",
            "의류", "신발", "지갑", "액세서리", "악세서리", "주얼리",
            "스킨케어", "메이크업", "향수", "핸드크림",
            "캠핑", "운동용품", "자전거",
            "뷰티", "디지털", "식품", "패션", "리빙", "스포츠"
    );
    private static final Map<String, List<String>> SEARCH_SYNONYMS = Map.ofEntries(
            Map.entry("반지", List.of("주얼리", "쥬얼리", "목걸이", "네크리스", "necklace", "ring")),
            Map.entry("목걸이", List.of("네크리스", "necklace", "펜던트", "주얼리", "쥬얼리")),
            Map.entry("아이폰", List.of("iphone", "아이폰케이스", "케이스", "애플")),
            Map.entry("케이스", List.of("case", "커버", "아이폰", "갤럭시", "파우치")),
            Map.entry("마이크", List.of("스피커", "블루투스", "마이크2개", "오디오", "microphone", "노래방세트")),
            Map.entry("옷", List.of("의류", "티셔츠", "후드", "후디", "트레이닝복", "반팔티")),
            Map.entry("커피", List.of("네스프레소", "캡슐", "에스프레소", "카페", "커피머신")),
            Map.entry("화장품", List.of("립스틱", "립밤", "파운데이션", "향수", "핸드크림", "코롱", "뷰티")),
            Map.entry("배터리", List.of("보조배터리", "고속충전", "충전", "충전기", "디지털")),
            Map.entry("과자", List.of("초콜릿", "쿠키", "츄파춥스", "젤리", "아이스크림", "간식")),
            Map.entry("간식", List.of("초콜릿", "쿠키", "츄파춥스", "젤리", "아이스크림", "과자")),
            Map.entry("가방", List.of("핸드백", "토트백", "숄더백", "백팩", "크로스백", "파우치")),
            Map.entry("텀블러", List.of("머그", "보틀", "컵", "머그컵", "스텐컵")),
            Map.entry("고기", List.of("한우", "소고기", "스테이크", "갈비", "육포")),
            Map.entry("장난감", List.of("피규어", "인형", "키링", "캐릭터", "굿즈", "토이", "완구")),
            Map.entry("거울", List.of("손거울", "미러", "화장거울", "메이크업", "뷰티")),
            Map.entry("핸드폰", List.of("아이폰", "갤럭시", "스마트폰", "휴대폰", "케이스", "보조배터리", "충전기")),
            Map.entry("휴대폰", List.of("아이폰", "갤럭시", "스마트폰", "핸드폰", "케이스", "보조배터리", "충전기")),
            Map.entry("스마트폰", List.of("아이폰", "갤럭시", "핸드폰", "휴대폰", "케이스", "보조배터리", "충전기")),
            Map.entry("쓰레기통", List.of("휴지통", "분리수거", "정리함", "수납", "리빙")),
            Map.entry("휴지통", List.of("쓰레기통", "분리수거", "정리함", "수납", "리빙")),
            Map.entry("전자제품", List.of("디지털", "전자", "가전", "보조배터리", "충전기", "스피커", "케이스")),
            Map.entry("가전", List.of("전자제품", "디지털", "커피머신", "스피커", "충전기", "테크")),
            Map.entry("맥북", List.of("macbook", "맥북에어", "맥북프로", "노트북", "laptop")),
            Map.entry("노트북", List.of("맥북", "macbook", "laptop", "그램", "갤럭시북", "맥북에어", "맥북프로")),
            Map.entry("태블릿", List.of("디지털", "아이패드", "갤럭시탭", "전자제품", "케이스", "가전")),
            Map.entry("이어폰", List.of("헤드폰", "블루투스", "오디오", "스피커", "디지털", "전자제품")),
            Map.entry("헤드폰", List.of("이어폰", "블루투스", "오디오", "스피커", "디지털", "전자제품")),
            Map.entry("키보드", List.of("마우스", "디지털", "전자제품", "가전", "테크")),
            Map.entry("마우스", List.of("키보드", "디지털", "전자제품", "가전", "테크")),
            Map.entry("생활용품", List.of("리빙", "정리함", "수납", "집들이", "욕실", "주방", "인테리어")),
            Map.entry("인테리어", List.of("리빙", "홈데코", "소품", "디퓨저", "캔들", "집들이", "생활용품")),
            Map.entry("주방용품", List.of("리빙", "주방", "키친", "머그", "텀블러", "컵", "접시")),
            Map.entry("문구", List.of("학용품", "다이어리", "노트", "펜", "캐릭터", "리빙")),
            Map.entry("학용품", List.of("문구", "다이어리", "노트", "펜", "캐릭터", "리빙")),
            Map.entry("반려용품", List.of("리빙", "생활용품", "장난감", "간식", "정리함")),
            Map.entry("유아용품", List.of("리빙", "생활용품", "장난감", "문구", "인테리어")),
            Map.entry("디저트", List.of("식품", "케이크", "쿠키", "초콜릿", "아이스크림", "간식")),
            Map.entry("음료", List.of("식품", "커피", "주스", "차", "에이드", "디저트")),
            Map.entry("빵", List.of("식품", "베이커리", "케이크", "쿠키", "디저트", "간식")),
            Map.entry("초콜릿", List.of("식품", "과자", "디저트", "쿠키", "사탕", "간식")),
            Map.entry("의류", List.of("옷", "패션", "티셔츠", "후드", "트레이닝복", "양말")),
            Map.entry("신발", List.of("패션", "운동화", "스니커즈", "샌들", "구두", "가방")),
            Map.entry("지갑", List.of("패션", "가죽지갑", "카드지갑", "가방", "잡화")),
            Map.entry("액세서리", List.of("패션", "주얼리", "반지", "목걸이", "팔찌", "귀걸이")),
            Map.entry("악세서리", List.of("패션", "주얼리", "반지", "목걸이", "팔찌", "귀걸이")),
            Map.entry("주얼리", List.of("패션", "반지", "목걸이", "팔찌", "귀걸이", "네크리스")),
            Map.entry("스킨케어", List.of("뷰티", "화장품", "핸드크림", "메이크업", "향수", "코스메틱")),
            Map.entry("메이크업", List.of("뷰티", "화장품", "립스틱", "쿠션", "파운데이션", "코스메틱")),
            Map.entry("향수", List.of("뷰티", "화장품", "코롱", "향기", "핸드크림")),
            Map.entry("핸드크림", List.of("뷰티", "화장품", "향수", "코롱", "스킨케어")),
            Map.entry("캠핑", List.of("스포츠", "아웃도어", "등산", "자전거", "운동용품", "리빙")),
            Map.entry("운동용품", List.of("스포츠", "헬스", "러닝", "트레이닝", "피트니스", "캠핑")),
            Map.entry("자전거", List.of("스포츠", "아웃도어", "러닝", "운동용품", "캠핑")),
            Map.entry("뷰티", List.of("립스틱", "립밤", "향수", "핸드크림", "쿠션", "파운데이션", "코롱")),
            Map.entry("디지털", List.of("보조배터리", "고속충전", "충전기", "커피머신", "네스프레소", "케이스", "카메라", "스피커")),
            Map.entry("식품", List.of("초콜릿", "쿠키", "젤리", "아이스크림", "케이크", "사탕", "고기")),
            Map.entry("패션", List.of("목걸이", "반지", "지갑", "가방", "신발", "의류", "티셔츠")),
            Map.entry("리빙", List.of("머그", "텀블러", "캔들", "디퓨저", "인테리어", "집들이")),
            Map.entry("스포츠", List.of("운동복", "트레이닝복", "러닝", "헬스", "양말", "반팔티"))
    );
    private static final Map<String, List<String>> SEARCH_INTENT_FALLBACK_KEYWORDS = Map.ofEntries(
            Map.entry("반지", List.of("주얼리", "쥬얼리", "목걸이", "네크리스", "ring")),
            Map.entry("목걸이", List.of("목걸이", "네크리스", "주얼리", "펜던트")),
            Map.entry("아이폰", List.of("아이폰", "iphone", "케이스")),
            Map.entry("케이스", List.of("케이스", "case", "아이폰", "갤럭시")),
            Map.entry("마이크", List.of("스피커", "블루투스", "마이크2개", "오디오")),
            Map.entry("옷", List.of("의류", "티셔츠", "후드", "트레이닝복")),
            Map.entry("커피", List.of("네스프레소", "캡슐", "에스프레소", "커피머신", "카페")),
            Map.entry("화장품", List.of("립스틱", "립밤", "파운데이션", "향수", "핸드크림", "코롱")),
            Map.entry("배터리", List.of("보조배터리", "고속충전", "충전", "충전기")),
            Map.entry("과자", List.of("초콜릿", "쿠키", "츄파춥스", "젤리", "아이스크림")),
            Map.entry("간식", List.of("초콜릿", "쿠키", "츄파춥스", "젤리", "아이스크림")),
            Map.entry("가방", List.of("가방", "핸드백", "토트백", "숄더백", "백팩", "크로스백", "파우치")),
            Map.entry("텀블러", List.of("텀블러", "머그", "보틀", "컵", "머그컵", "스텐컵")),
            Map.entry("고기", List.of("고기", "한우", "소고기", "스테이크", "갈비", "육포")),
            Map.entry("장난감", List.of("장난감", "완구", "토이", "피규어", "인형", "키링", "캐릭터", "굿즈")),
            Map.entry("거울", List.of("거울", "손거울", "미러", "화장거울", "메이크업")),
            Map.entry("핸드폰", List.of("핸드폰", "휴대폰", "스마트폰", "아이폰", "갤럭시", "케이스", "충전기", "보조배터리")),
            Map.entry("휴대폰", List.of("휴대폰", "핸드폰", "스마트폰", "아이폰", "갤럭시", "케이스", "충전기", "보조배터리")),
            Map.entry("스마트폰", List.of("스마트폰", "핸드폰", "휴대폰", "아이폰", "갤럭시", "케이스", "충전기", "보조배터리")),
            Map.entry("쓰레기통", List.of("쓰레기통", "휴지통", "분리수거", "정리함", "수납")),
            Map.entry("휴지통", List.of("휴지통", "쓰레기통", "분리수거", "정리함", "수납")),
            Map.entry("전자제품", List.of("전자제품", "디지털", "가전", "충전기", "보조배터리", "스피커")),
            Map.entry("가전", List.of("가전", "전자제품", "디지털", "커피머신", "스피커")),
            Map.entry("맥북", List.of("맥북", "macbook", "맥북에어", "맥북프로", "노트북", "laptop")),
            Map.entry("노트북", List.of("노트북", "맥북", "macbook", "laptop", "그램", "갤럭시북", "맥북에어", "맥북프로")),
            Map.entry("태블릿", List.of("태블릿", "아이패드", "갤럭시탭", "디지털", "케이스")),
            Map.entry("이어폰", List.of("이어폰", "헤드폰", "블루투스", "오디오", "스피커")),
            Map.entry("헤드폰", List.of("헤드폰", "이어폰", "블루투스", "오디오", "스피커")),
            Map.entry("키보드", List.of("키보드", "마우스", "디지털")),
            Map.entry("마우스", List.of("마우스", "키보드", "디지털")),
            Map.entry("생활용품", List.of("생활용품", "정리함", "수납", "집들이", "인테리어", "리빙")),
            Map.entry("인테리어", List.of("인테리어", "홈데코", "소품", "디퓨저", "캔들", "리빙")),
            Map.entry("주방용품", List.of("주방용품", "주방", "키친", "머그", "텀블러", "컵")),
            Map.entry("문구", List.of("문구", "학용품", "다이어리", "노트", "펜")),
            Map.entry("학용품", List.of("학용품", "문구", "다이어리", "노트", "펜")),
            Map.entry("반려용품", List.of("반려용품", "생활용품", "장난감", "간식")),
            Map.entry("유아용품", List.of("유아용품", "장난감", "문구", "생활용품")),
            Map.entry("디저트", List.of("디저트", "케이크", "쿠키", "초콜릿", "아이스크림", "간식")),
            Map.entry("음료", List.of("음료", "커피", "주스", "차", "에이드")),
            Map.entry("빵", List.of("빵", "베이커리", "케이크", "쿠키", "디저트")),
            Map.entry("초콜릿", List.of("초콜릿", "과자", "디저트", "쿠키", "사탕")),
            Map.entry("의류", List.of("의류", "옷", "티셔츠", "후드", "트레이닝복", "패션")),
            Map.entry("신발", List.of("신발", "운동화", "스니커즈", "샌들", "구두", "패션")),
            Map.entry("지갑", List.of("지갑", "카드지갑", "가죽지갑", "잡화", "패션")),
            Map.entry("액세서리", List.of("액세서리", "주얼리", "반지", "목걸이", "팔찌", "패션")),
            Map.entry("악세서리", List.of("악세서리", "주얼리", "반지", "목걸이", "팔찌", "패션")),
            Map.entry("주얼리", List.of("주얼리", "반지", "목걸이", "팔찌", "귀걸이", "패션")),
            Map.entry("스킨케어", List.of("스킨케어", "화장품", "핸드크림", "향수", "코스메틱", "뷰티")),
            Map.entry("메이크업", List.of("메이크업", "화장품", "립스틱", "쿠션", "파운데이션", "뷰티")),
            Map.entry("향수", List.of("향수", "코롱", "화장품", "핸드크림", "뷰티")),
            Map.entry("핸드크림", List.of("핸드크림", "화장품", "향수", "코롱", "뷰티")),
            Map.entry("캠핑", List.of("캠핑", "아웃도어", "등산", "운동용품", "스포츠")),
            Map.entry("운동용품", List.of("운동용품", "헬스", "러닝", "트레이닝", "스포츠")),
            Map.entry("자전거", List.of("자전거", "아웃도어", "운동용품", "러닝", "스포츠")),
            Map.entry("뷰티", List.of("립스틱", "립밤", "파운데이션", "향수", "핸드크림", "코롱")),
            Map.entry("디지털", List.of("보조배터리", "고속충전", "충전기", "커피머신", "케이스", "카메라", "스피커")),
            Map.entry("식품", List.of("초콜릿", "쿠키", "젤리", "아이스크림", "케이크", "사탕", "고기")),
            Map.entry("패션", List.of("목걸이", "반지", "지갑", "가방", "신발", "의류")),
            Map.entry("리빙", List.of("머그", "텀블러", "캔들", "디퓨저")),
            Map.entry("스포츠", List.of("운동복", "트레이닝복", "러닝", "헬스"))
    );
    private static final Map<String, String> SEARCH_INTENT_FALLBACK_CATEGORY = Map.ofEntries(
            Map.entry("반지", "패션"),
            Map.entry("목걸이", "패션"),
            Map.entry("아이폰", "디지털"),
            Map.entry("케이스", "디지털"),
            Map.entry("마이크", "디지털"),
            Map.entry("맥북", "디지털"),
            Map.entry("옷", "패션"),
            Map.entry("화장품", "뷰티"),
            Map.entry("배터리", "디지털"),
            Map.entry("과자", "식품"),
            Map.entry("간식", "식품"),
            Map.entry("가방", "패션"),
            Map.entry("텀블러", "리빙"),
            Map.entry("고기", "식품"),
            Map.entry("장난감", "리빙"),
            Map.entry("거울", "뷰티"),
            Map.entry("핸드폰", "디지털"),
            Map.entry("휴대폰", "디지털"),
            Map.entry("스마트폰", "디지털"),
            Map.entry("쓰레기통", "리빙"),
            Map.entry("휴지통", "리빙"),
            Map.entry("전자제품", "디지털"),
            Map.entry("가전", "디지털"),
            Map.entry("노트북", "디지털"),
            Map.entry("태블릿", "디지털"),
            Map.entry("이어폰", "디지털"),
            Map.entry("헤드폰", "디지털"),
            Map.entry("키보드", "디지털"),
            Map.entry("마우스", "디지털"),
            Map.entry("생활용품", "리빙"),
            Map.entry("인테리어", "리빙"),
            Map.entry("주방용품", "리빙"),
            Map.entry("문구", "리빙"),
            Map.entry("학용품", "리빙"),
            Map.entry("반려용품", "리빙"),
            Map.entry("유아용품", "리빙"),
            Map.entry("디저트", "식품"),
            Map.entry("음료", "식품"),
            Map.entry("빵", "식품"),
            Map.entry("초콜릿", "식품"),
            Map.entry("의류", "패션"),
            Map.entry("신발", "패션"),
            Map.entry("지갑", "패션"),
            Map.entry("액세서리", "패션"),
            Map.entry("악세서리", "패션"),
            Map.entry("주얼리", "패션"),
            Map.entry("스킨케어", "뷰티"),
            Map.entry("메이크업", "뷰티"),
            Map.entry("향수", "뷰티"),
            Map.entry("핸드크림", "뷰티"),
            Map.entry("캠핑", "스포츠"),
            Map.entry("운동용품", "스포츠"),
            Map.entry("자전거", "스포츠"),
            Map.entry("뷰티", "뷰티"),
            Map.entry("디지털", "디지털"),
            Map.entry("식품", "식품"),
            Map.entry("패션", "패션"),
            Map.entry("리빙", "리빙"),
            Map.entry("스포츠", "스포츠")
    );

    private final ItemIndexRepository itemIndexRepository;
    private final ItemRepository itemRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final JPAQueryFactory queryFactory;
    private final SearchKeywordLogRepository searchKeywordLogRepository;

    @Value("${app.search.keyword-dictionary.top-n:120}")
    private int autoDictionaryTopN;

    @Value("${app.search.keyword-dictionary.lookback-days:30}")
    private int autoDictionaryLookbackDays;

    @Value("${app.search.keyword-dictionary.synonyms-per-keyword:5}")
    private int autoDictionarySynonymsPerKeyword;

    @Value("${app.search.keyword-log.retention-days:90}")
    private int searchKeywordLogRetentionDays;

    private volatile long lastIndexedItemCount = -1L;
    private volatile LocalDateTime lastIndexedModifiedDate;
    private volatile LocalDateTime lastSyncAttemptAt;
    private volatile Map<String, List<String>> runtimeSearchSynonyms = Map.of();
    private volatile Map<String, List<String>> runtimeIntentFallbackKeywords = Map.of();
    private volatile Map<String, String> runtimeIntentFallbackCategory = Map.of();
    private volatile Set<String> runtimeCategoryFirstIntents = Set.of();
    private volatile Set<String> runtimeSynonymFirstKeywords = Set.of();

    public Slice<ShopDto> searchWithCategoryAndName(String keyword, Pageable pageable) {
        ensureIndexReady();
        ensureAutoDictionaryReady();
        String normalizedKeyword = normalizeSearchToken(keyword);
        if (normalizedKeyword.isBlank()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        List<String> expandedKeywords = expandSearchKeywords(normalizedKeyword);
        if (expandedKeywords.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        Slice<ItemIndex> items = searchByExpandedKeywords(expandedKeywords, pageable);
        if (!items.isEmpty()) {
            persistSearchKeywordLog(keyword, normalizedKeyword, items);
            return items.map(ShopDto::fromIndex);
        }

        if (shouldPreferIntentSearch(normalizedKeyword)) {
            Slice<ItemIndex> fallbackItems = searchByIntentFallback(normalizedKeyword, pageable);
            if (!fallbackItems.isEmpty()) {
                persistSearchKeywordLog(keyword, normalizedKeyword, fallbackItems);
                return fallbackItems.map(ShopDto::fromIndex);
            }
        }

        persistSearchKeywordLog(keyword, normalizedKeyword, items);
        return items.map(ShopDto::fromIndex);
    }

    @Counted("ItemIndexService.getItemsUsingElasticsearch")
    public Slice<ShopDto> searchWithCategory(String keyword, Pageable pageable) {
        ensureIndexReady();
        Slice<ItemIndex> items;

        if (keyword == null || keyword.isBlank()) {
            Pageable sortedPageable = pageable.getSort().isSorted()
                    ? pageable
                    : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Order.desc("itemId")));
            Page<ItemIndex> page = itemIndexRepository.findAll(sortedPageable);
            items = page;
        } else {
            items = itemIndexRepository.findByCategory(keyword, pageable);
        }

        return items.map(ShopDto::fromIndex);
    }

    public List<String> getCategories() {
        ensureIndexReady();
        return StreamSupport.stream(itemIndexRepository.findAll(Sort.by(Sort.Direction.ASC, "category")).spliterator(), false)
                .map(ItemIndex::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .toList();
    }

    public ItemDetailDto getItemDetail(Long memberId, Long itemId) {
        ensureIndexReady();

        ItemIndex itemIndex = itemIndexRepository.findById(itemId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_ITEM));

        boolean bookmarked = memberId != null
                && bookmarkRepository.findBookmarkByMemberAndItem(memberId, itemId).isPresent();

        return ItemDetailDto.fromIndex(itemIndex, bookmarked);
    }

    public HomeRankingResult getHomeRankings(String rankingType, String audience, String priceRange, int size) {
        ensureIndexReady();
        String normalizedRankingType = normalizeRankingType(rankingType);
        String normalizedAudience = normalizeAudience(audience);
        String normalizedPriceRange = normalizePriceRange(priceRange);
        boolean fallbackApplied = false;

        List<ScoredItem> scoredItems = switch (normalizedRankingType) {
            case "purchase" -> getPurchaseRanking(normalizedAudience, normalizedPriceRange, size);
            case "wish" -> getWishRanking(normalizedAudience, normalizedPriceRange, size);
            default -> getFundingRanking(normalizedAudience, normalizedPriceRange, size);
        };

        if (scoredItems.isEmpty() && shouldUseFallback(normalizedRankingType, normalizedAudience, normalizedPriceRange)) {
            scoredItems = getFallbackRanking(normalizedAudience, normalizedPriceRange, size);
            fallbackApplied = !scoredItems.isEmpty();
        }

        return new HomeRankingResult(toHomeRankingItems(scoredItems), fallbackApplied);
    }

    synchronized void ensureIndexReady() {
        try {
            long databaseItemCount = itemRepository.count();
            IndexOperations indexOperations = elasticsearchOperations.indexOps(ItemIndex.class);

            if (!indexOperations.exists()) {
                indexOperations.createWithMapping();
            }

            LocalDateTime latestModifiedDate = itemRepository.findFirstByOrderByModifiedDateDesc()
                    .map(Item::getModifiedDate)
                    .orElse(null);

            boolean alreadySynced = databaseItemCount == lastIndexedItemCount
                    && Objects.equals(latestModifiedDate, lastIndexedModifiedDate);

            if (alreadySynced) {
                return;
            }

            if (databaseItemCount == 0) {
                // 빈 상태는 읽기 경로에서 굳이 전체 삭제를 강제하지 않는다.
                lastIndexedItemCount = 0L;
                lastIndexedModifiedDate = null;
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            if (lastSyncAttemptAt != null && now.isBefore(lastSyncAttemptAt.plus(INDEX_SYNC_RETRY_INTERVAL))) {
                return;
            }
            lastSyncAttemptAt = now;

            List<ItemIndex> itemIndexes = itemRepository.findAll(Sort.by(Sort.Direction.ASC, "itemId")).stream()
                    .map(ItemIndex::fromEntity)
                    .toList();

            // document id(itemId) 기준 upsert. read API 경로에서 deleteAll()로 블로킹하지 않는다.
            itemIndexRepository.saveAll(itemIndexes);
            lastIndexedItemCount = databaseItemCount;
            lastIndexedModifiedDate = latestModifiedDate;
            log.info("elasticsearch item index synced: {} docs", itemIndexes.size());
        } catch (Exception e) {
            // 인덱스 동기화 실패가 즉시 사용자 조회 실패(500)로 이어지지 않게 격리한다.
            String reason = e.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = e.getClass().getSimpleName();
            } else if (reason.length() > 300) {
                reason = reason.substring(0, 300) + "...";
            }
            log.warn("elasticsearch item index sync skipped due to error: {}", reason);
        }
    }

    private void ensureAutoDictionaryReady() {
        if (!runtimeIntentFallbackKeywords.isEmpty()
                || !runtimeIntentFallbackCategory.isEmpty()
                || !runtimeSearchSynonyms.isEmpty()) {
            return;
        }
        refreshAutoSearchDictionary();
    }

    @Scheduled(cron = "${app.search.keyword-dictionary.refresh-cron:0 10 * * * *}")
    @Transactional
    public void refreshAutoSearchDictionary() {
        try {
            int topN = Math.min(Math.max(autoDictionaryTopN, 1), MAX_AUTO_DICTIONARY_TOP_N);
            int synonymsPerKeyword = Math.min(Math.max(autoDictionarySynonymsPerKeyword, 1), MAX_AUTO_SYNONYMS_PER_KEYWORD);
            int lookbackDays = Math.max(autoDictionaryLookbackDays, 1);
            LocalDateTime from = LocalDateTime.now().minusDays(lookbackDays);

            List<SearchKeywordLogRepository.KeywordFrequencyView> topKeywords = searchKeywordLogRepository.findTopKeywordsSince(
                    from,
                    PageRequest.of(0, topN)
            );
            if (topKeywords.isEmpty()) {
                runtimeSearchSynonyms = Map.of();
                runtimeIntentFallbackKeywords = Map.of();
                runtimeIntentFallbackCategory = Map.of();
                runtimeCategoryFirstIntents = Set.of();
                runtimeSynonymFirstKeywords = Set.of();
                return;
            }

            Map<String, List<String>> autoSynonyms = new LinkedHashMap<>();
            Map<String, List<String>> autoFallbackKeywords = new LinkedHashMap<>();
            Map<String, String> autoFallbackCategory = new LinkedHashMap<>();
            Set<String> autoCategoryFirstIntents = new LinkedHashSet<>();
            Set<String> autoSynonymFirstKeywords = new LinkedHashSet<>();

            for (SearchKeywordLogRepository.KeywordFrequencyView keywordRow : topKeywords) {
                String normalizedKeyword = normalizeSearchToken(keywordRow.getKeyword());
                if (normalizedKeyword.isBlank() || normalizedKeyword.length() < MIN_SEARCH_KEYWORD_LENGTH) {
                    continue;
                }

                List<SearchKeywordLogRepository.CategoryFrequencyView> topCategories =
                        searchKeywordLogRepository.findTopCategoriesByKeywordSince(
                                normalizedKeyword,
                                from,
                                PageRequest.of(0, synonymsPerKeyword)
                        );

                LinkedHashSet<String> relatedCategories = new LinkedHashSet<>();
                for (SearchKeywordLogRepository.CategoryFrequencyView categoryRow : topCategories) {
                    String normalizedCategory = normalizeSearchToken(categoryRow.getCategory());
                    if (!normalizedCategory.isBlank()) {
                        relatedCategories.add(normalizedCategory);
                    }
                }

                if (relatedCategories.isEmpty()) {
                    continue;
                }

                List<String> synonymTerms = relatedCategories.stream()
                        .map(this::normalizeSearchToken)
                        .filter(category -> !category.isBlank())
                        .filter(category -> CATEGORY_FALLBACK_INTENTS.contains(normalizedKeyword)
                                || HIGH_LEVEL_INTENTS.contains(normalizedKeyword)
                                || !HIGH_LEVEL_INTENTS.contains(category))
                        .distinct()
                        .toList();
                if (synonymTerms.isEmpty()) {
                    continue;
                }
                autoSynonyms.put(normalizedKeyword, synonymTerms);

                LinkedHashSet<String> fallbackTerms = new LinkedHashSet<>();
                fallbackTerms.add(normalizedKeyword);
                fallbackTerms.addAll(synonymTerms);
                autoFallbackKeywords.put(normalizedKeyword, List.copyOf(fallbackTerms));

                String primaryCategory = synonymTerms.get(0);
                autoFallbackCategory.put(normalizedKeyword, primaryCategory);
                if (CATEGORY_FIRST_INTENTS.contains(normalizedKeyword) || HIGH_LEVEL_INTENTS.contains(normalizedKeyword)) {
                    autoCategoryFirstIntents.add(normalizedKeyword);
                }
                if (SYNONYM_FIRST_KEYWORDS.contains(normalizedKeyword) || CATEGORY_FALLBACK_INTENTS.contains(normalizedKeyword)) {
                    autoSynonymFirstKeywords.add(normalizedKeyword);
                }
            }

            runtimeSearchSynonyms = toImmutableKeywordMap(autoSynonyms);
            runtimeIntentFallbackKeywords = toImmutableKeywordMap(autoFallbackKeywords);
            runtimeIntentFallbackCategory = Map.copyOf(autoFallbackCategory);
            runtimeCategoryFirstIntents = Set.copyOf(autoCategoryFirstIntents);
            runtimeSynonymFirstKeywords = Set.copyOf(autoSynonymFirstKeywords);

            log.info("search auto dictionary refreshed: topKeywords={}, mappedKeywords={}",
                    topKeywords.size(), runtimeIntentFallbackKeywords.size());
        } catch (Exception e) {
            String reason = e.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = e.getClass().getSimpleName();
            }
            log.warn("search auto dictionary refresh skipped due to error: {}", reason);
        }
    }

    @Scheduled(cron = "${app.search.keyword-log.cleanup-cron:0 35 3 * * *}")
    @Transactional
    public void cleanupSearchKeywordLogs() {
        try {
            int retentionDays = Math.max(searchKeywordLogRetentionDays, autoDictionaryLookbackDays + 1);
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            long deleted = searchKeywordLogRepository.deleteByCreatedDateBefore(cutoff);
            if (deleted > 0) {
                log.info("search keyword logs cleaned up: deleted={}", deleted);
            }
        } catch (Exception e) {
            String reason = e.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = e.getClass().getSimpleName();
            }
            log.warn("search keyword log cleanup skipped due to error: {}", reason);
        }
    }

    private void persistSearchKeywordLog(String rawKeyword, String normalizedKeyword, Slice<ItemIndex> searchResult) {
        if (normalizedKeyword == null
                || normalizedKeyword.isBlank()
                || normalizedKeyword.length() < MIN_SEARCH_KEYWORD_LENGTH) {
            return;
        }

        String safeRawKeyword = trimToLength(rawKeyword == null ? normalizedKeyword : rawKeyword.trim(), 200);
        String safeNormalizedKeyword = trimToLength(normalizedKeyword, 120);
        int resultCount = searchResult == null ? 0 : searchResult.getNumberOfElements();
        String topCategory = searchResult == null ? null : extractTopCategory(searchResult.getContent());
        String safeTopCategory = trimToLength(topCategory, 40);

        try {
            searchKeywordLogRepository.save(SearchKeywordLog.create(
                    safeRawKeyword,
                    safeNormalizedKeyword,
                    resultCount,
                    safeTopCategory
            ));
        } catch (Exception e) {
            log.debug("search keyword log save skipped: {}", e.getClass().getSimpleName());
        }
    }

    private String extractTopCategory(List<ItemIndex> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        Map<String, Integer> frequency = new LinkedHashMap<>();
        String topCategory = null;
        int topFrequency = 0;
        for (ItemIndex indexedItem : items) {
            if (indexedItem == null) {
                continue;
            }
            String category = normalizeSearchToken(indexedItem.getCategory());
            if (category.isBlank()) {
                continue;
            }
            int next = frequency.getOrDefault(category, 0) + 1;
            frequency.put(category, next);
            if (next > topFrequency) {
                topFrequency = next;
                topCategory = category;
            }
        }
        return topCategory;
    }

    private Map<String, List<String>> toImmutableKeywordMap(Map<String, List<String>> source) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copied.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copied);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private List<String> expandSearchKeywords(String rawKeyword) {
        String normalized = normalizeSearchToken(rawKeyword);
        if (normalized.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        addExpandedKeyword(expanded, normalized);

        String[] tokens = normalized.split("\\s+");
        for (String token : tokens) {
            addExpandedKeyword(expanded, token);
        }

        return expanded.stream()
                .filter(token -> token.length() >= 2)
                .limit(MAX_SEARCH_EXPANSION)
                .toList();
    }

    private void addExpandedKeyword(LinkedHashSet<String> expanded, String token) {
        String normalized = normalizeSearchToken(token);
        if (normalized.isBlank()) {
            return;
        }

        boolean synonymFirst = SYNONYM_FIRST_KEYWORDS.contains(normalized) || runtimeSynonymFirstKeywords.contains(normalized);
        if (!synonymFirst) {
            expanded.add(normalized);
        }

        List<String> synonyms = resolveSynonyms(normalized);
        if (synonyms != null) {
            for (String synonym : synonyms) {
                String normalizedSynonym = normalizeSearchToken(synonym);
                if (!normalizedSynonym.isBlank()) {
                    expanded.add(normalizedSynonym);
                }
            }
        }

        if (synonymFirst) {
            expanded.add(normalized);
        }
    }

    private String normalizeSearchToken(String token) {
        if (token == null) {
            return "";
        }
        return token.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private Slice<ItemIndex> searchByExpandedKeywords(List<String> expandedKeywords, Pageable pageable) {
        return itemIndexRepository.findByKeywords(expandedKeywords, pageable);
    }

    private Slice<ItemIndex> searchByIntentFallback(String rawKeyword, Pageable pageable) {
        String normalizedKeyword = normalizeSearchToken(rawKeyword);
        String intentKey = resolveIntentKey(normalizedKeyword);
        boolean preferCategoryFirst = (CATEGORY_FIRST_INTENTS.contains(intentKey) || runtimeCategoryFirstIntents.contains(intentKey))
                && shouldPreferIntentSearch(normalizedKeyword);
        boolean allowCategoryFallback = HIGH_LEVEL_INTENTS.contains(intentKey)
                || CATEGORY_FALLBACK_INTENTS.contains(intentKey)
                || runtimeCategoryFirstIntents.contains(intentKey);
        String mappedCategory = HIGH_LEVEL_INTENTS.contains(intentKey)
                ? intentKey
                : resolveFallbackCategory(intentKey);

        if (preferCategoryFirst) {
            Slice<ItemIndex> byHighLevelCategory = mappedCategory == null || mappedCategory.isBlank()
                    ? new SliceImpl<>(List.of(), pageable, false)
                    : itemIndexRepository.findByCategory(mappedCategory, pageable);
            if (!byHighLevelCategory.isEmpty()) {
                return byHighLevelCategory;
            }
        }

        List<String> fallbackKeywords = resolveFallbackKeywords(intentKey);
        if (fallbackKeywords != null && !fallbackKeywords.isEmpty()) {
            Slice<ItemIndex> byKeyword = searchByExpandedKeywords(fallbackKeywords, pageable);
            if (!byKeyword.isEmpty()) {
                if (!allowCategoryFallback || byKeyword.getNumberOfElements() >= MIN_INTENT_KEYWORD_HITS) {
                    return byKeyword;
                }

                String keywordFallbackCategory = resolveFallbackCategory(intentKey);
                if (keywordFallbackCategory != null && !keywordFallbackCategory.isBlank()) {
                    Slice<ItemIndex> byCategory = itemIndexRepository.findByCategory(keywordFallbackCategory, pageable);
                    if (!byCategory.isEmpty()) {
                        return mergePrimaryAndFallback(byKeyword, byCategory, pageable);
                    }
                }
                return byKeyword;
            }
        }

        String fallbackCategory = resolveFallbackCategory(intentKey);
        if (allowCategoryFallback && fallbackCategory != null && !fallbackCategory.isBlank()) {
            return itemIndexRepository.findByCategory(fallbackCategory, pageable);
        }
        return new SliceImpl<>(List.of(), pageable, false);
    }

    private Slice<ItemIndex> mergePrimaryAndFallback(Slice<ItemIndex> primary, Slice<ItemIndex> fallback, Pageable pageable) {
        LinkedHashMap<Long, ItemIndex> mergedById = new LinkedHashMap<>();
        for (ItemIndex item : primary.getContent()) {
            mergedById.put(item.getItemId(), item);
        }
        for (ItemIndex item : fallback.getContent()) {
            if (mergedById.size() >= pageable.getPageSize()) {
                break;
            }
            mergedById.putIfAbsent(item.getItemId(), item);
        }

        List<ItemIndex> merged = new ArrayList<>(mergedById.values());
        boolean hasNext = primary.hasNext() || fallback.hasNext() || merged.size() > pageable.getPageSize();
        if (merged.size() > pageable.getPageSize()) {
            merged = merged.subList(0, pageable.getPageSize());
        }
        return new SliceImpl<>(merged, pageable, hasNext);
    }

    private boolean shouldPreferIntentSearch(String normalizedKeyword) {
        if (runtimeCategoryFirstIntents.contains(normalizedKeyword)
                || runtimeSynonymFirstKeywords.contains(normalizedKeyword)
                || runtimeIntentFallbackCategory.containsKey(normalizedKeyword)
                || runtimeIntentFallbackKeywords.containsKey(normalizedKeyword)) {
            return true;
        }
        return containsAny(normalizedKeyword,
                "뷰티", "디지털", "식품", "패션", "리빙", "스포츠",
                "전자", "전자제품", "전자기기", "가전", "테크", "노트북", "태블릿", "이어폰", "헤드폰", "키보드", "마우스",
                "생활용품", "인테리어", "집들이", "주방용품", "문구", "학용품", "반려용품", "유아용품",
                "먹거리", "먹을거리", "음식", "디저트", "간편식", "빵", "베이커리", "음료", "초콜릿",
                "의류", "신발", "지갑", "액세서리", "악세서리", "주얼리",
                "운동용품", "캠핑", "자전거", "미용", "코스메틱", "스킨케어", "화장품", "메이크업", "향수", "핸드크림",
                "장난감", "완구", "토이", "거울", "미러",
                "쓰레기통", "휴지통", "분리수거");
    }

    private String resolveIntentKey(String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return "";
        }
        if (runtimeIntentFallbackKeywords.containsKey(normalizedKeyword)
                || runtimeIntentFallbackCategory.containsKey(normalizedKeyword)) {
            return normalizedKeyword;
        }
        if (SEARCH_INTENT_FALLBACK_KEYWORDS.containsKey(normalizedKeyword)
                || SEARCH_INTENT_FALLBACK_CATEGORY.containsKey(normalizedKeyword)) {
            return normalizedKeyword;
        }
        if (normalizedKeyword.contains("반지")) {
            return "반지";
        }
        if (normalizedKeyword.contains("목걸이") || normalizedKeyword.contains("네크리스")) {
            return "목걸이";
        }
        if (normalizedKeyword.contains("아이폰") || normalizedKeyword.contains("iphone")) {
            return "아이폰";
        }
        if (normalizedKeyword.contains("케이스") || normalizedKeyword.contains("case")) {
            return "케이스";
        }
        if (normalizedKeyword.contains("마이크") || normalizedKeyword.contains("mic")) {
            return "마이크";
        }
        if (containsAny(normalizedKeyword, "옷", "의류", "패션", "의상", "웨어")) {
            return "옷";
        }
        if (containsAny(normalizedKeyword, "커피", "cafe")) {
            return "커피";
        }
        if (containsAny(normalizedKeyword, "화장품", "코스메틱", "메이크업", "스킨케어", "미용", "향수", "핸드크림")) {
            return "화장품";
        }
        if (containsAny(normalizedKeyword, "배터리", "보조배터리", "충전기", "고속충전")) {
            return "배터리";
        }
        if (containsAny(normalizedKeyword, "과자", "간식", "스낵", "디저트", "빵", "초콜릿", "음료")) {
            return "과자";
        }
        if (containsAny(normalizedKeyword, "가방", "핸드백", "토트백", "숄더백", "백팩", "크로스백")) {
            return "가방";
        }
        if (containsAny(normalizedKeyword, "텀블러", "머그", "머그컵", "보틀", "스텐컵", "물병")) {
            return "텀블러";
        }
        if (containsAny(normalizedKeyword, "고기", "한우", "소고기", "돼지고기", "스테이크", "갈비", "육포")) {
            return "고기";
        }
        if (containsAny(normalizedKeyword, "장난감", "완구", "토이", "피규어", "인형", "키링", "캐릭터", "굿즈")) {
            return "장난감";
        }
        if (containsAny(normalizedKeyword, "거울", "손거울", "미러", "화장거울")) {
            return "거울";
        }
        if (containsAny(normalizedKeyword, "핸드폰", "휴대폰", "스마트폰", "모바일폰", "휴대전화")) {
            return "핸드폰";
        }
        if (containsAny(normalizedKeyword, "쓰레기통", "휴지통", "분리수거", "trash")) {
            return "쓰레기통";
        }
        if (containsAny(normalizedKeyword, "맥북", "macbook", "노트북", "laptop", "그램", "갤럭시북")) {
            return "노트북";
        }
        if (containsAny(normalizedKeyword, "뷰티", "화장", "화장품", "코스메틱", "메이크업", "미용", "스킨케어", "향수", "핸드크림", "립", "쿠션", "파운데이션")) {
            return "뷰티";
        }
        if (containsAny(normalizedKeyword, "디지털", "전자", "전자제품", "전자기기", "디바이스", "기기", "가전", "테크", "it", "모바일",
                "노트북", "태블릿", "이어폰", "헤드폰", "키보드", "마우스")) {
            return "디지털";
        }
        if (containsAny(normalizedKeyword, "식품", "음식", "먹거리", "먹을거리", "디저트", "군것질", "간편식", "음료", "빵", "베이커리", "초콜릿")) {
            return "식품";
        }
        if (containsAny(normalizedKeyword, "패션", "의류", "주얼리", "액세서리", "악세서리", "악세사리", "신발", "가방", "지갑", "잡화")) {
            return "패션";
        }
        if (containsAny(normalizedKeyword, "리빙", "생활", "생활용품", "인테리어", "집들이", "홈", "홈데코", "소품", "주방", "키친",
                "문구", "학용품", "반려용품", "유아용품", "머그", "텀블러")) {
            return "리빙";
        }
        if (containsAny(normalizedKeyword, "스포츠", "운동", "헬스", "러닝", "트레이닝", "피트니스", "gym", "짐", "캠핑", "자전거", "아웃도어")) {
            return "스포츠";
        }
        return normalizedKeyword;
    }

    private List<String> resolveSynonyms(String keyword) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        List<String> runtimeSynonyms = runtimeSearchSynonyms.get(keyword);
        if (runtimeSynonyms != null) {
            for (String synonym : runtimeSynonyms) {
                String normalizedSynonym = normalizeSearchToken(synonym);
                if (!normalizedSynonym.isBlank()) {
                    merged.add(normalizedSynonym);
                }
            }
        }
        List<String> staticSynonyms = SEARCH_SYNONYMS.get(keyword);
        if (staticSynonyms != null) {
            for (String synonym : staticSynonyms) {
                String normalizedSynonym = normalizeSearchToken(synonym);
                if (!normalizedSynonym.isBlank()) {
                    merged.add(normalizedSynonym);
                }
            }
        }
        if (merged.isEmpty()) {
            return null;
        }
        return List.copyOf(merged);
    }

    private List<String> resolveFallbackKeywords(String intentKey) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        List<String> runtimeKeywords = runtimeIntentFallbackKeywords.get(intentKey);
        if (runtimeKeywords != null) {
            for (String keyword : runtimeKeywords) {
                String normalizedKeyword = normalizeSearchToken(keyword);
                if (!normalizedKeyword.isBlank()) {
                    merged.add(normalizedKeyword);
                }
            }
        }
        List<String> staticKeywords = SEARCH_INTENT_FALLBACK_KEYWORDS.get(intentKey);
        if (staticKeywords != null) {
            for (String keyword : staticKeywords) {
                String normalizedKeyword = normalizeSearchToken(keyword);
                if (!normalizedKeyword.isBlank()) {
                    merged.add(normalizedKeyword);
                }
            }
        }
        if (merged.isEmpty()) {
            return null;
        }
        return List.copyOf(merged);
    }

    private String resolveFallbackCategory(String intentKey) {
        String runtimeCategory = runtimeIntentFallbackCategory.get(intentKey);
        if (runtimeCategory != null && !runtimeCategory.isBlank()) {
            return runtimeCategory;
        }
        return SEARCH_INTENT_FALLBACK_CATEGORY.get(intentKey);
    }

    private boolean containsAny(String normalizedKeyword, String... terms) {
        for (String term : terms) {
            if (normalizedKeyword.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private List<HomeRankingItemDto> toHomeRankingItems(List<ScoredItem> scoredItems) {
        List<Long> itemIds = scoredItems.stream()
                .map(ScoredItem::itemId)
                .toList();

        Map<Long, ItemIndex> itemIndexMap = new HashMap<>();
        itemIndexRepository.findAllById(itemIds).forEach(index -> itemIndexMap.put(index.getItemId(), index));

        List<HomeRankingItemDto> results = new ArrayList<>();

        for (int i = 0; i < scoredItems.size(); i++) {
            ScoredItem scoredItem = scoredItems.get(i);
            ItemIndex itemIndex = itemIndexMap.get(scoredItem.itemId());
            if (itemIndex == null) {
                continue;
            }
            results.add(HomeRankingItemDto.fromIndex(itemIndex, scoredItem.score(), i + 1));
        }

        return results;
    }

    private List<ScoredItem> getFundingRanking(String audience, String priceRange, int size) {
        List<Tuple> rows = queryFactory
                .select(fundingItem.item.itemId, fundingItem.count())
                .from(fundingItem)
                .join(fundingItem.item, item)
                .join(fundingItem.funding.member, member)
                .where(memberGenderFilter(audience), priceRangeFilter(priceRange))
                .groupBy(fundingItem.item.itemId)
                .orderBy(fundingItem.count().desc(), fundingItem.item.itemId.desc())
                .limit(size)
                .fetch();

        return rows.stream()
                .map(row -> new ScoredItem(
                        row.get(fundingItem.item.itemId),
                        row.get(fundingItem.count()) == null ? 0L : row.get(fundingItem.count())))
                .toList();
    }

    private List<ScoredItem> getPurchaseRanking(String audience, String priceRange, int size) {
        List<Tuple> rows = queryFactory
                .select(orderItem.item.itemId, orderItem.quantity.sum())
                .from(orderItem)
                .join(orderItem.item, item)
                .join(orderItem.order.member, member)
                .where(memberGenderFilter(audience), priceRangeFilter(priceRange))
                .groupBy(orderItem.item.itemId)
                .orderBy(orderItem.quantity.sum().desc(), orderItem.item.itemId.desc())
                .limit(size)
                .fetch();

        return rows.stream()
                .map(row -> {
                    Integer score = row.get(orderItem.quantity.sum());
                    return new ScoredItem(row.get(orderItem.item.itemId), score == null ? 0L : score.longValue());
                })
                .toList();
    }

    private List<ScoredItem> getWishRanking(String audience, String priceRange, int size) {
        List<Tuple> rows = queryFactory
                .select(bookmark.item.itemId, bookmark.count())
                .from(bookmark)
                .join(bookmark.item, item)
                .join(bookmark.member, member)
                .where(memberGenderFilter(audience), priceRangeFilter(priceRange))
                .groupBy(bookmark.item.itemId)
                .orderBy(bookmark.count().desc(), bookmark.item.itemId.desc())
                .limit(size)
                .fetch();

        return rows.stream()
                .map(row -> new ScoredItem(
                        row.get(bookmark.item.itemId),
                        row.get(bookmark.count()) == null ? 0L : row.get(bookmark.count())))
                .toList();
    }

    private List<ScoredItem> getFallbackRanking(String audience, String priceRange, int size) {
        List<Item> items = queryFactory
                .selectFrom(item)
                .where(priceRangeFilter(priceRange))
                .orderBy(item.itemId.desc())
                .limit(size)
                .fetch();

        List<ScoredItem> fallback = new ArrayList<>();
        for (Item fallbackItem : items) {
            fallback.add(new ScoredItem(fallbackItem.getItemId(), 0L));
        }
        return fallback;
    }

    private boolean shouldUseFallback(String rankingType, String audience, String priceRange) {
        if (!"all".equals(audience) || !"all".equals(priceRange)) {
            return false;
        }

        return !hasAnyRankingData(rankingType);
    }

    private boolean hasAnyRankingData(String rankingType) {
        return switch (rankingType) {
            case "purchase" -> queryFactory
                    .selectOne()
                    .from(orderItem)
                    .fetchFirst() != null;
            case "wish" -> queryFactory
                    .selectOne()
                    .from(bookmark)
                    .fetchFirst() != null;
            default -> queryFactory
                    .selectOne()
                    .from(fundingItem)
                    .fetchFirst() != null;
        };
    }

    private BooleanExpression memberGenderFilter(String audience) {
        if ("all".equals(audience)) {
            return null;
        }

        return member.gender.eq("woman".equals(audience) ? MemberGender.WOMAN : MemberGender.MAN);
    }

    private BooleanExpression priceRangeFilter(String priceRange) {
        return switch (priceRange) {
            case "under10k" -> item.itemPrice.lt(10_000);
            case "10kto30k" -> item.itemPrice.goe(10_000).and(item.itemPrice.lt(30_000));
            case "30kto50k" -> item.itemPrice.goe(30_000).and(item.itemPrice.lt(50_000));
            case "over50k" -> item.itemPrice.goe(50_000);
            default -> null;
        };
    }

    private String normalizeRankingType(String rankingType) {
        if (rankingType == null || rankingType.isBlank()) {
            return "funding";
        }
        return rankingType.toLowerCase();
    }

    private String normalizeAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            return "all";
        }
        return audience.toLowerCase();
    }

    private String normalizePriceRange(String priceRange) {
        if (priceRange == null || priceRange.isBlank()) {
            return "all";
        }
        return priceRange.toLowerCase();
    }

    private record ScoredItem(Long itemId, long score) {
    }

    public record HomeRankingResult(List<HomeRankingItemDto> items, boolean fallbackApplied) {
    }
}
