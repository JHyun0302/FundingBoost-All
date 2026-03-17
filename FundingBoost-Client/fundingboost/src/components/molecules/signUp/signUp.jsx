import React, {useState} from 'react';
import './signUp.scss';
import logo from "../../../assets/logo.svg";
import SignUpBtn from '../../atoms/button/signUpBtn/signUpBtn';
import { FaEye } from "react-icons/fa";
import { FaEyeSlash } from "react-icons/fa";

const SignUp = () => {

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');
    const [passwordConfirm, setPasswordConfirm] = useState('');
    const [emailValid, setEmailValid] = useState(true);
    const [passwordsMatch, setPasswordsMatch] = useState(true);
    const [seePassword, setSeePassword] = useState(false);
    const [seePasswordConfirm, setSeePasswordConfirm] = useState(false);
    const [gender, setGender] = useState('');

    //이메일 형식
    const fromEmail = (email) => {
        const em = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return em.test(String(email).toLowerCase());
    }

    const inputEmail = (e) => {
        const newEmail = e.target.value;
        setEmail(newEmail);
        setEmailValid(fromEmail(newEmail));
    }

    // 비밀번호 동일한지 체크
    const inputPassword = (e) => {
        const newPassword = e.target.value;
        setPassword(newPassword);
        setPasswordsMatch(newPassword === passwordConfirm);
    }
    const inputPasswordConfirm = (e) => {
        const newPasswordConfirm= e.target.value;
        setPasswordConfirm(newPasswordConfirm);
        setPasswordsMatch(password === newPasswordConfirm);
    }

    //비밀번호 숨기기
    const toggleSeePassword = (e) => {
        setSeePassword(!seePassword);
    }
    const toggleSeePasswordConfirm = (e) => {
        setSeePasswordConfirm(!seePasswordConfirm);
    }

    return (
        <div className="signUp">
            <div className="social-signUp-box">
                <div className="signUp-logoImg">
                    <img alt="logoImg" src={logo}/>
                </div>
                <div className="signUp-text">친구들과 함께 펀딩을 즐겨보세요.</div>
                <hr style={{border: '1px solid', color:'black', width: '80%'}}/>
                <div className="signUp-box">
                    <br/>
                    <div className="input-box">
                        <div className="input-box-info">
                            <div className="input-box-detail">
                                <a>이름</a>
                                <input
                                    type="text"
                                    className="input-field"
                                    placeholder="이름을 입력해주세요"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                />
                            </div>
                        </div>
                        <div className="input-box-info">
                            <div className="input-box-detail">
                                <a>이메일</a>
                                <input
                                    type="text"
                                    className="input-field"
                                    placeholder="이메일을 입력해주세요"
                                    value={email}
                                    onChange={inputEmail}
                                />
                            </div>
                            {!emailValid && <div className="singUp-error-message">이메일 형식은 funding12@boost.com 입니다.</div>}
                        </div>
                        <div className="input-box-info">
                            <div className="input-box-detail">
                                <a>비밀번호</a>
                                <div className="input-box-password">
                                    <input
                                        type={seePassword ? "text" : "password"}
                                        className="input-field"
                                        placeholder="비밀번호를 입력해주세요"
                                        value={password}
                                        onChange={inputPassword}
                                    />
                                    <button type="button" className="password-toggle-btn" onClick={toggleSeePassword}>
                                        {seePassword ? <FaEyeSlash/> : <FaEye/>}
                                    </button>
                                </div>
                            </div>

                        </div>
                        <div className="input-box-info">
                            <div className="input-box-detail">
                                <a>성별</a>
                                <div className="gender-select-group">
                                    <button
                                        type="button"
                                        className={`gender-select-btn ${gender === 'MAN' ? 'active' : ''}`}
                                        onClick={() => setGender('MAN')}
                                    >
                                        남자
                                    </button>
                                    <button
                                        type="button"
                                        className={`gender-select-btn ${gender === 'WOMAN' ? 'active' : ''}`}
                                        onClick={() => setGender('WOMAN')}
                                    >
                                        여자
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div className="input-box-info">
                            <div className="input-box-detail">
                                <a>비밀번호 확인</a>
                                <div className="input-box-password">
                                    <input
                                        type={seePasswordConfirm ? "text" : "password"}
                                        className="input-field"
                                        placeholder="비밀번호를 입력해주세요"
                                        value={passwordConfirm}
                                        onChange={inputPasswordConfirm}
                                    />
                                    <button type="button" className="password-toggle-btn"
                                            onClick={toggleSeePasswordConfirm}>
                                        {seePasswordConfirm ? <FaEyeSlash/> : <FaEye/>}
                                    </button>
                                </div>

                            </div>
                            {!passwordsMatch && <div className="singUp-error-message">비밀번호가 일치하지 않습니다.</div>}
                        </div>

                    </div>

                    <br/>

                </div>
                <div className="signUp-btn">
                    <SignUpBtn
                        username={username}
                        email={email}
                        emailValid={emailValid}
                        password={password}
                        passwordConfirm={passwordConfirm}
                        gender={gender}
                    />
                </div>

            </div>
        </div>
    );
};

export default SignUp;
