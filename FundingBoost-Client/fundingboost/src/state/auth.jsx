import { atom } from 'recoil';

// localStorage에서 로그인 상태 불러오기
const getInitialLoginState = () => {
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    const user = JSON.parse(localStorage.getItem('user'));
    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');

    return {
        isAuthenticated,
        user,
        accessToken,
        refreshToken
    };
};

export const nickNameState = atom({
    key: 'nickNameState',
    default: '',
});

export const passwordState = atom({
    key: 'passwordState',
    default: '',
});

export const loginState = atom({
    key: 'loginState',
    default: getInitialLoginState(),
});
