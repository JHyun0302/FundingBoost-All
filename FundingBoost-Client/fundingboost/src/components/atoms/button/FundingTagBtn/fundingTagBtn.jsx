import React, { useEffect, useMemo, useState } from "react";
import "./fundingTagBtn.scss";
import Button from "react-bootstrap/Button";

const DEFAULT_TAGS = ["생일", "졸업", "기타"];
const STORAGE_KEY = "funding.custom.tags";
const MAX_CUSTOM_TAGS = 8;
const MAX_TAG_LENGTH = 20;

export default function FundingTagBtn ({onTagSelect}) {
    const [selectedTag, setselectedTag] = useState(null);
    const [customTagInput, setCustomTagInput] = useState("");
    const [customTags, setCustomTags] = useState([]);

    const allTags = useMemo(() => {
        return [...DEFAULT_TAGS, ...customTags];
    }, [customTags]);

    const normalizeTag = (tag) => {
        if (typeof tag !== "string") {
            return "";
        }
        const normalizedTag = tag.replaceAll("#", "").trim().replace(/\s+/g, " ");
        if (!normalizedTag) {
            return "";
        }
        return normalizedTag.length > MAX_TAG_LENGTH
            ? normalizedTag.substring(0, MAX_TAG_LENGTH)
            : normalizedTag;
    };

    useEffect(() => {
        try {
            const storedTags = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
            if (!Array.isArray(storedTags)) {
                return;
            }

            const normalizedStoredTags = storedTags
                .map((tag) => normalizeTag(tag))
                .filter((tag) => !!tag && !DEFAULT_TAGS.includes(tag))
                .slice(0, MAX_CUSTOM_TAGS);

            setCustomTags(Array.from(new Set(normalizedStoredTags)));
        } catch (error) {
            console.error("커스텀 태그 로드 실패:", error);
        }
    }, []);

    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(customTags));
    }, [customTags]);

    const handleTagClick = (tagText) => {
        onTagSelect(tagText);
        setselectedTag(tagText);
    };

    const handleAddCustomTag = () => {
        const normalizedTag = normalizeTag(customTagInput);
        if (!normalizedTag) {
            return;
        }

        if (allTags.includes(normalizedTag)) {
            setCustomTagInput("");
            handleTagClick(normalizedTag);
            return;
        }

        if (customTags.length >= MAX_CUSTOM_TAGS) {
            alert(`사용자 태그는 최대 ${MAX_CUSTOM_TAGS}개까지 추가할 수 있습니다.`);
            return;
        }

        const updatedTags = [...customTags, normalizedTag];
        setCustomTags(updatedTags);
        setCustomTagInput("");
        handleTagClick(normalizedTag);
    };

    const handleCustomTagRemove = (targetTag) => {
        setCustomTags((prevTags) => prevTags.filter((tag) => tag !== targetTag));
        if (selectedTag === targetTag) {
            setselectedTag(null);
            onTagSelect("");
        }
    };

    const handleInputKeyDown = (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            handleAddCustomTag();
        }
    };

    return (
      <div className="fundingOpenBtnBox">
          <div className="fundingOpenBtnView">
              <div className="fundingOpenBtnTitle" >🔗 펀딩 태그</div>
              <div className="fundingOpenTagBtn">
                  {allTags.map((tag) => {
                      const isSelected = selectedTag === tag;
                      const isCustomTag = !DEFAULT_TAGS.includes(tag);
                      return (
                          <div key={tag} className={`fundingTagChip ${isSelected ? "selected" : ""}`}>
                              <button
                                  type="button"
                                  className="fundingTagChipButton"
                                  onClick={() => handleTagClick(tag)}
                              >
                                  #{tag}
                              </button>
                              {isCustomTag && (
                                  <button
                                      type="button"
                                      className="fundingTagChipRemoveButton"
                                      onClick={() => handleCustomTagRemove(tag)}
                                      aria-label={`${tag} 태그 삭제`}
                                  >
                                      ×
                                  </button>
                              )}
                          </div>
                      );
                  })}
              </div>
              <div className="fundingCustomTagManage">
                  <input
                      className="fundingCustomTagInput"
                      type="text"
                      value={customTagInput}
                      maxLength={MAX_TAG_LENGTH}
                      placeholder="태그 추가 (예: 입학, 결혼)"
                      onChange={(event) => setCustomTagInput(event.target.value)}
                      onKeyDown={handleInputKeyDown}
                  />
                  <Button className="fundingCustomTagAddBtn" onClick={handleAddCustomTag}>
                      추가
                  </Button>
              </div>
              <p className="fundingCustomTagHint">추가한 태그는 x 버튼으로 삭제할 수 있습니다.</p>
          </div>
      </div>
    );
};
