package kcs.funding.fundingboost.domain.entity;

import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum Tag {
    BIRTHDAY("생일"),
    GRADUATE("졸업"),
    ETC("기타"),
    CUSTOM("커스텀");

    private final String tag;

    Tag(String tag) {
        this.tag = tag;
    }

    public String getDisplayName() {
        return "#" + this.tag;
    }

    public static String normalizeRawTag(String targetTag) {
        if (targetTag == null) {
            return "";
        }
        String normalizedTag = targetTag.trim();
        if (normalizedTag.startsWith("#")) {
            normalizedTag = normalizedTag.substring(1).trim();
        }
        return normalizedTag;
    }

    public static Tag resolveOrCustom(String targetTag) {
        String normalizedTag = normalizeRawTag(targetTag);
        if ("생일이에요🎉 축하해주세요".equals(normalizedTag)) {
            normalizedTag = BIRTHDAY.getTag();
        } else if ("졸업했어요🧑‍🎓 축하해주세요".equals(normalizedTag)) {
            normalizedTag = GRADUATE.getTag();
        } else if ("펀딩 해주세요🎁".equals(normalizedTag)) {
            normalizedTag = ETC.getTag();
        }

        for (Tag tag : Tag.values()) {
            if (tag == CUSTOM) {
                continue;
            }
            if (tag.getTag().equals(normalizedTag)) {
                return tag;
            }
        }
        return normalizedTag.isBlank() ? ETC : CUSTOM;
    }

    public static Tag getTag(String targetTag) {
        Tag resolvedTag = resolveOrCustom(targetTag);
        if (resolvedTag != CUSTOM) {
            return resolvedTag;
        }
        throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
