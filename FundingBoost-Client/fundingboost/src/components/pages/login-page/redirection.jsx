import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useRecoilState } from 'recoil';
import axios from 'axios';
import { nickNameState, loginState } from "../../../state/auth";

const Redirection = () => {
    const code = new URL(window.location.href).searchParams.get('code');
    const navigate = useNavigate();
    const [nickName, setNickName] = useRecoilState(nickNameState);
    const [login, setLoginState] = useRecoilState(loginState);
    const [loginError, setLoginError] = useState(false);
    console.log("Code:", code);

    useEffect(() => {
        const login = async () => {
            if (!code) {
                console.log("No code found");
                return;
            }
            try {
                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/login/oauth2/code/kakao?code=${code}`, {
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

                        localStorage.setItem('isAuthenticated', 'true');
                        localStorage.setItem('user', JSON.stringify({ nickName }));
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
                    console.error('Login failed:', response.data.message);
                    setLoginError(true);
                }
            } catch (error) {
                console.error('Error during login:', error);
                setLoginError(true);
            }
        };

        login();
    }, [code, navigate, nickName, setLoginState]);

    return <div></div>;
};

export default Redirection;