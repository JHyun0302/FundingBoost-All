package kcs.funding.fundingboost.domain.dto.response.common;

import java.util.List;
import org.springframework.data.domain.Slice;

public record SliceResponseDto<T>(List<T> content, boolean hasNext) {

    public static <T> SliceResponseDto<T> fromSlice(Slice<T> slice) {
        return new SliceResponseDto<>(slice.getContent(), slice.hasNext());
    }
}
