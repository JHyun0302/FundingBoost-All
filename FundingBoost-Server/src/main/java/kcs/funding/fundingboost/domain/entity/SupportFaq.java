package kcs.funding.fundingboost.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kcs.funding.fundingboost.domain.entity.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "support_faq")
public class SupportFaq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long faqId;

    @Column(name = "question", nullable = false, length = 300)
    private String question;

    @Column(name = "answer", nullable = false, length = 4000)
    private String answer;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static SupportFaq createFaq(String question, String answer, int sortOrder) {
        SupportFaq faq = new SupportFaq();
        faq.question = question;
        faq.answer = answer;
        faq.sortOrder = sortOrder;
        return faq;
    }

    public void update(String question, String answer, int sortOrder) {
        this.question = question;
        this.answer = answer;
        this.sortOrder = sortOrder;
    }
}

