import React, { useEffect, useState } from 'react';
import { Button, Container, Form, Nav, Navbar, NavDropdown } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { useRecoilValue, useSetRecoilState } from 'recoil';
import axios from 'axios';
import 'bootstrap/dist/css/bootstrap.min.css';
import './header.scss';
import logoImg from '../../../assets/logo.svg';
import gifthub from '../../../assets/gifthub.svg';
import { CiSearch } from "react-icons/ci";
import { loginState } from '../../../state/auth';

function HeaderBar() {
    const navigate = useNavigate();
    const login = useRecoilValue(loginState);
    const setLoginState = useSetRecoilState(loginState);
    const [nickName, setNickName] = useState('');
    const [isAdmin, setIsAdmin] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            const user = JSON.parse(storedUser);
            setNickName(user.nickName);
        }
    }, [login.isAuthenticated]);

    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        if (!login.isAuthenticated || !token) {
            setIsAdmin(false);
            return;
        }

        let mounted = true;
        const checkAdminAccess = async () => {
            try {
                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/admin/dashboard/access`, {
                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${token}`
                    }
                });
                const hasAccess = response?.data?.data?.isSuccess === true;
                if (mounted) {
                    setIsAdmin(hasAccess);
                }
            } catch (error) {
                if (mounted) {
                    setIsAdmin(false);
                }
            }
        };

        checkAdminAccess();
        return () => {
            mounted = false;
        };
    }, [login.isAuthenticated]);

    const loginHandler = () => {
        setLoginState(prevState => ({
            ...prevState,
            isAuthenticated: true,
            user: { nickName: 'nick_name' }
        }));
        navigate('/login');
    };

    const logoutHandler = async () => {
        try {
            const accessToken = localStorage.getItem('accessToken');
            const refreshToken = localStorage.getItem('refreshToken');

            await axios.post(
                `${process.env.REACT_APP_FUNDINGBOOST}/logout`,
                {},
                {
                    headers: {
                        'Authorization': `Bearer ${accessToken}`,
                        'RefreshToken': refreshToken
                    }
                }
            );

            setLoginState(prevState => ({
                ...prevState,
                isAuthenticated: false,
                user: null,
                accessToken: null,
                refreshToken: null
            }));
            localStorage.removeItem('isAuthenticated');
            localStorage.removeItem('user');
            localStorage.removeItem('GrantType');
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('nickName');
            setIsAdmin(false);
            navigate('/home');
        } catch (error) {
            console.error('로그아웃 실패:', error);
            // 오류 처리 로직 추가
        }
    };

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
    };

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        const normalizedQuery = searchQuery.trim();
        if (!normalizedQuery) {
            navigate('/shopping');
            return;
        }

        const params = new URLSearchParams();
        params.set('search', normalizedQuery);
        navigate(`/shopping?${params.toString()}`);
    };

    return (
        <Navbar expand="lg" className="headerBar">
            <Container fluid>
                <Navbar.Brand href="/home" className="navbar-logo"><img src={logoImg} alt="Logo" /></Navbar.Brand>
                <Navbar.Toggle aria-controls="navbarScroll" />
                <Navbar.Collapse id="navbarScroll">
                    <Nav className="navbarMenu" navbarScroll>
                        <Nav.Link href="/home" className="home-btn">홈</Nav.Link>
                        <Nav.Link href="/friend-funding" className="friend-btn">친구펀딩</Nav.Link>
                        <Nav.Link href="/shopping" className="shopping-btn">쇼핑하기</Nav.Link>
                        <Nav.Link href="/mypage" className="my-btn">MY</Nav.Link>
                    </Nav>

                    <div className="header-search-login">
                        <Form className="searchBar" onSubmit={handleSearchSubmit}>
                            <Form.Control
                                type="search"
                                placeholder="상품을 검색해보세요"
                                className="me-2"
                                aria-label="Search"
                                value={searchQuery}
                                onChange={handleSearchChange}
                            />
                            <CiSearch style={{ fontSize: '40px', cursor: 'pointer' }} onClick={handleSearchSubmit} />
                            <Button className="gifthub-btn" href="/gifthub"><img src={gifthub} alt="GiftHub" /></Button>
                        </Form>

                            <div className="loginLogout">
                            {login.isAuthenticated && localStorage.getItem('accessToken') ? (
                                <NavDropdown title={localStorage.getItem('nickName')} id="logoutDropdown" align="end">
                                    {isAdmin && (
                                        <>
                                            <NavDropdown.Item onClick={() => navigate('/adm')} className="dropdownItem">관리자</NavDropdown.Item>
                                            <NavDropdown.Divider />
                                        </>
                                    )}
                                    <NavDropdown.Item onClick={() => navigate('/mypage')} className="dropdownItem">My Page</NavDropdown.Item>
                                    <NavDropdown.Divider />
                                    <NavDropdown.Item onClick={logoutHandler} className="dropdownItem">로그아웃</NavDropdown.Item>
                                </NavDropdown>
                            ) : (
                                <button onClick={loginHandler} className="login-btn">로그인</button>
                            )}
                        </div>
                    </div>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
}

export default HeaderBar;
