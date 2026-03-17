import React from 'react';
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import LoginPane from '../../organisms/contents/login/login'

function LoginPage() {
    return (
        <div className="Login-pages">
            <HeaderBar />
            <LoginPane />
            <Footer />
        </div>
    );
}

export default LoginPage;