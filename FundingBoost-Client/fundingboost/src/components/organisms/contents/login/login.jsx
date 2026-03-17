import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useRecoilState } from 'recoil';
import { nickNameState, passwordState, loginState } from '../../../../state/auth';
import '../login/login.scss';
import kakaologin from "../../../../assets/sociallogin/kakaologin.png";
import naverlogin from "../../../../assets/sociallogin/naverlogin.png";
import googlelogin from "../../../../assets/sociallogin/googlelogin.png";
import applelogin from "../../../../assets/sociallogin/applelogin.png";
import loginmoji from "../../../../assets/loginmoji.svg";

const LoginPane = () => {
    const navigate = useNavigate();
    const [nickName, setNickName] = useRecoilState(nickNameState);
    const [email, setemail] = useRecoilState(nickNameState);
    const [password, setPassword] = useRecoilState(passwordState);
    const [login, setLoginState] = useRecoilState(loginState);
    const [loginError, setLoginError] = useState(false);

    const handleLogin = async () => {
        try {
            const data = {
                email,
                password
            };

            const response = await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/login`, data, {
                responseType: 'json',
                headers: {
                    'Content-Type': 'application/json',
                },
                withCredentials: true,
            });

            console.log('Response:', response.data);

            if (response.data.success) {
                const { access_token, refresh_token } = response.data.data;

                if (access_token && refresh_token) {
                    const [grantType, token] = access_token.split(' ');

                    axios.defaults.headers.common['Authorization'] = token;
                    axios.defaults.headers.common['RefreshToken'] = refresh_token;

                    // 로그인 상태와 토큰을 localStorage에 저장
                    localStorage.setItem('isAuthenticated', 'true');
                    // localStorage.setItem('user', JSON.stringify({ nickName }));
                    localStorage.setItem('GrantType', grantType);
                    localStorage.setItem('accessToken', token);
                    localStorage.setItem('refreshToken', refresh_token);

                    setLoginState({
                        isAuthenticated: true,
                        user: { nickName },
                        GrantType: grantType,
                        accessToken: token,
                        refreshToken: refresh_token,
                    });

                    setLoginError(false);
                    navigate('/home');
                } else {
                    console.error('Missing token data:', response.data.data);
                    setLoginError(true);
                }
            } else {
                setLoginError(true);
            }
        } catch (error) {
            console.error('POST 에러:', error);
            setLoginError(true);
        }
    };

    const handleKakaoLogin = async () => {
        try{
           await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/login/oauth`, {
                responseType: 'json',
                headers: {
                    'Content-Type': 'application/json',
                },
                withCredentials: true,
            })
               .then((response) => {
                   console.log('응답 데이터:', response.data); // 응답 데이터 출력
                   console.log('로그인 접속!');
                   window.location.href = response.data; // 백엔드에서 받은 URL로 리디렉션
               })
               .catch((error) => {
                   console.error('로그인 실패', error);
               });
        } catch (error) {
            console.error('POST 에러:', error);
            setLoginError(true);
        }
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            handleLogin();
        }
    };

    const goSignUp = () => {
        navigate('/signup');
    };

    return (
        <div className="social-login-pane">
            <div className="social-login-box">
                <h1 className="centerAlign">LOGIN</h1>
                <div className="loginmoji">
                    <img src={loginmoji} alt="loginmoji" />
                </div>
                <div className="login-box">
                    <br />
                    <div className="input-box">
                        <input
                            type="text"
                            className="input-field"
                            placeholder="아이디"
                            value={email}
                            onChange={(e) => setemail(e.target.value)}
                        />
                        <input
                            type="password"
                            className="input-field"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            onKeyDown={handleKeyDown} // 엔터 키 이벤트 핸들러 추가
                        />
                        {loginError && ( // 로그인 에러 발생 시 메시지 띄우기
                            <p className="error-message">아이디 또는 비밀번호를 다시 확인해주세요.</p>
                        )}
                    </div>
                    <br />
                    <button className="login-btn-grey" onClick={handleLogin}>로그인</button>
                </div>
                <button type="button" className="social-login-link social-login-link-button" onClick={handleKakaoLogin}>
                    <img src={kakaologin} alt="Kakao Login" className="social-login-btn" />
                </button>
                <a href="https://nid.naver.com/oauth2.0/authorize" className="social-login-link">
                    <img src={naverlogin} alt="Naver Login" className="social-login-btn" />
                </a>
                <a href="https://accounts.google.com/o/oauth2/auth" className="social-login-link">
                    <img src={googlelogin} alt="Google Login" className="social-login-btn" />
                </a>
                <a href="https://appleid.apple.com/auth/authorize" className="social-login-link">
                    <img src={applelogin} alt="Apple Login" className="social-login-btn" />
                </a>
                <h5 className="signup-text">아직 회원이 아니신가요?&nbsp;&nbsp;&nbsp;
                    <button className="signup-btn" onClick={goSignUp}>회원가입</button>
                </h5>
            </div>
        </div>
    );
};

export default LoginPane;
