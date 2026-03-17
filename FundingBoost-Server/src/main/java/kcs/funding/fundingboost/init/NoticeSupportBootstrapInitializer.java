package kcs.funding.fundingboost.init;

import java.util.List;
import kcs.funding.fundingboost.domain.entity.Notice;
import kcs.funding.fundingboost.domain.entity.SupportFaq;
import kcs.funding.fundingboost.domain.repository.NoticeRepository;
import kcs.funding.fundingboost.domain.repository.SupportFaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.testdata", name = "enabled", havingValue = "true")
public class NoticeSupportBootstrapInitializer {

    private final NoticeRepository noticeRepository;
    private final SupportFaqRepository supportFaqRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        if (noticeRepository.count() == 0) {
            noticeRepository.saveAll(List.of(
                    Notice.createNotice(
                            "업데이트",
                            "실시간 선물랭킹 가격대 필터가 추가되었습니다.",
                            "홈 화면 실시간 선물랭킹에서 1만원 이하, 1-3만원, 3-5만원, 5만원 이상 가격대별로 상품을 바로 확인할 수 있습니다."
                    ),
                    Notice.createNotice(
                            "안내",
                            "마이페이지 이력 화면이 10개 단위 페이지네이션으로 변경되었습니다.",
                            "지난 펀딩 이력, 친구 펀딩 기록, 구매 이력, 위시리스트, 배송지 관리, MY 리뷰가 모두 10개씩 페이지 단위로 분리되어 더 안정적으로 확인할 수 있습니다."
                    ),
                    Notice.createNotice(
                            "운영",
                            "이미지 URL 정규화와 크롤러 썸네일 보정이 적용되었습니다.",
                            "카카오 CDN 이미지 URL이 percent-encoding 된 상태로 저장되던 문제를 수정했고, 배지 이미지가 대표 이미지로 저장되지 않도록 크롤러 선택 로직을 개선했습니다."
                    )
            ));
            log.info("공지사항 기본 데이터 시드 완료");
        }

        if (supportFaqRepository.count() == 0) {
            supportFaqRepository.saveAll(List.of(
                    SupportFaq.createFaq(
                            "펀딩이 종료되면 결제는 어떻게 처리되나요?",
                            "펀딩이 종료되면 내가 직접 결제한 금액, 포인트 사용 금액, 친구들에게 받은 펀딩 금액이 합산되어 주문 이력에 반영됩니다. 주문 상세에서 결제 비중을 확인할 수 있습니다.",
                            1
                    ),
                    SupportFaq.createFaq(
                            "카카오 로그인 후 성별 입력이 보이는 이유는 무엇인가요?",
                            "카카오 동의항목 권한 제약으로 성별 정보를 직접 받기 어려운 경우가 있어, 서비스 내부에서 최초 1회 성별을 입력하도록 처리합니다.",
                            2
                    ),
                    SupportFaq.createFaq(
                            "문의는 어디로 보내면 되나요?",
                            "현재 토이 프로젝트 단계에서는 support@fundingboost.test 메일로 문의를 받고 있습니다.",
                            3
                    )
            ));
            log.info("고객센터 FAQ 기본 데이터 시드 완료");
        }
    }
}

