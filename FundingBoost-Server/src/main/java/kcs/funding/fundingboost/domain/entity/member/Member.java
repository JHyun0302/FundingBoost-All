package kcs.funding.fundingboost.domain.entity.member;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import kcs.funding.fundingboost.domain.entity.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @NotNull
    @Column(name = "nick_name", length = 20)
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20, columnDefinition = "varchar(20)")
    private MemberRole memberRole;

    @Column(length = 100)
    private String password;

    @NotNull
    @Column(length = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MemberGender gender;

    @Column(name = "profile_img_url", length = 1000)
    private String profileImgUrl;

    @ColumnDefault("0")
    @NotNull
    private int point;

    @Column(length = 100)
    private String kakaoId;

    public static Member createSignUpMember(String nickName, String password, String email, MemberGender gender) {
        Member member = new Member();
        member.nickName = nickName;
        member.password = password;
        member.email = email;
        member.gender = gender == null ? MemberGender.UNKNOWN : gender;
        member.point = 0;
        member.memberRole = MemberRole.ROLE_USER;

        return member;
    }

    public static Member createMember(String nickName, String email, String password, String profileImgUrl,
                                      String kakaoId) {
        Member member = new Member();
        member.nickName = nickName;
        member.memberRole = MemberRole.ROLE_USER;
        member.email = email;
        member.gender = MemberGender.UNKNOWN;
        member.password = password;
        member.profileImgUrl = profileImgUrl;
        member.kakaoId = kakaoId;
        return member;
    }

    //init(ν¬μΈνΈ ν¬ν¨)
    public static Member createMemberWithPoint(String nickName, String email, String password, String profileImgUrl,
                                               int point, String kakaoId, MemberGender gender) {
        Member member = new Member();
        member.nickName = nickName;
        member.memberRole = MemberRole.ROLE_USER;
        member.email = email;
        member.gender = gender == null ? MemberGender.UNKNOWN : gender;
        member.password = password;
        member.profileImgUrl = profileImgUrl;
        member.point = point;
        member.kakaoId = kakaoId;
        return member;
    }

    public void minusPoint(int usingPoint) {
        point -= usingPoint;
    }

    public void plusPoint(int exchangePoint) {
        point += exchangePoint;
    }
	
    public void changeProfileImgUrl(String newProfileImgUrl) {
        this.profileImgUrl = newProfileImgUrl;
    }

    public void changeGender(MemberGender newGender) {
        this.gender = newGender;
    }

    public void changeMemberRole(MemberRole newRole) {
        this.memberRole = newRole;
    }
}
