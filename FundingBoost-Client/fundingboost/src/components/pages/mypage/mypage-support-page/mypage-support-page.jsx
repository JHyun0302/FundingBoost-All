import React from 'react';
import './mypage-support-page.scss';
import HeaderBar from '../../../organisms/header/header';
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer';
import MypageSupport from '../../../organisms/contents/mypage-support/mypage-support';

function MypageSupportPage() {
    return (
        <div className="mypage-support-page">
            <HeaderBar />
            <MypageSupport />
            <Footer />
        </div>
    );
}

export default MypageSupportPage;
