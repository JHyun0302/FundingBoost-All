package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_RESOURCE;

import java.util.List;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.admin.AdminNoticeUpsertRequestDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.notice.MyPageNoticeDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.notice.MyPageNoticeListDto;
import kcs.funding.fundingboost.domain.entity.Notice;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public MyPageNoticeListDto getNotices() {
        List<MyPageNoticeDto> notices = noticeRepository.findAllByOrderByCreatedDateDesc().stream()
                .map(MyPageNoticeDto::fromEntity)
                .toList();
        return MyPageNoticeListDto.fromEntity(notices);
    }

    @Transactional
    public MyPageNoticeDto createNotice(AdminNoticeUpsertRequestDto request) {
        Notice notice = Notice.createNotice(
                request.category().trim(),
                request.title().trim(),
                request.body().trim()
        );
        Notice savedNotice = noticeRepository.save(notice);
        return MyPageNoticeDto.fromEntity(savedNotice);
    }

    @Transactional
    public MyPageNoticeDto updateNotice(Long noticeId, AdminNoticeUpsertRequestDto request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_RESOURCE));

        notice.update(
                request.category().trim(),
                request.title().trim(),
                request.body().trim()
        );
        return MyPageNoticeDto.fromEntity(notice);
    }

    @Transactional
    public CommonSuccessDto deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_RESOURCE));
        noticeRepository.delete(notice);
        return CommonSuccessDto.fromEntity(true);
    }
}

