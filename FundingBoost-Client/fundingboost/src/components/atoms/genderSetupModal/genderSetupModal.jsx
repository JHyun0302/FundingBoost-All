import React, { useEffect } from 'react';
import './genderSetupModal.scss';

const genderOptions = [
    { value: 'MAN', label: '남자' },
    { value: 'WOMAN', label: '여자' },
];

const GenderSetupModal = ({ show, nickName, selectedGender, onSelect, onSubmit, isSaving, errorMessage }) => {
    useEffect(() => {
        if (!show) {
            document.body.style.overflow = '';
            return undefined;
        }

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        return () => {
            document.body.style.overflow = previousOverflow;
        };
    }, [show]);

    if (!show) {
        return null;
    }

    return (
        <div className="gender-setup-overlay" role="dialog" aria-modal="true" aria-labelledby="gender-setup-title">
            <div className="gender-setup-card">
                <div className="gender-setup-badge">첫 설정</div>
                <h2 id="gender-setup-title">
                    {nickName ? `${nickName}님,` : '회원님,'} 성별을 알려주세요
                </h2>
                <p className="gender-setup-description">
                    카카오 로그인은 성별 정보를 직접 받을 수 없어 한 번만 추가로 확인합니다.
                    <br />
                    홈 랭킹의 남자/여자 기준과 추천 정확도에만 사용됩니다.
                </p>

                <div className="gender-setup-options">
                    {genderOptions.map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            className={`gender-setup-option ${selectedGender === option.value ? 'active' : ''}`}
                            onClick={() => onSelect(option.value)}
                            disabled={isSaving}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>

                {errorMessage ? <div className="gender-setup-error">{errorMessage}</div> : null}

                <button
                    type="button"
                    className="gender-setup-submit"
                    onClick={onSubmit}
                    disabled={!selectedGender || isSaving}
                >
                    {isSaving ? '저장 중...' : '저장하고 계속 보기'}
                </button>
            </div>
        </div>
    );
};

export default GenderSetupModal;
