import React from 'react';
import './signUp-page.scss'
import Header from '../../organisms/header/header';
import Footer from '../../organisms/footer/footer';
import SignUp from '../../molecules/signUp/signUp';

const SignUpPage = () => {
    return (
        <div>
            <Header/>
            <SignUp/>
            <Footer/>
        </div>
    );
};

export default SignUpPage;