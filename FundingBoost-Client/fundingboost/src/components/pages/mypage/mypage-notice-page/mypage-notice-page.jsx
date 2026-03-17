import React from 'react';
import './mypage-notice-page.scss';
import HeaderBar from '../../../organisms/header/header';
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer';
import MypageNotice from '../../../organisms/contents/mypage-notice/mypage-notice';

function MypageNoticePage() {
    return (
        <div className="mypage-notice-page">
            <HeaderBar />
            <MypageNotice />
            <Footer />
        </div>
    );
}

export default MypageNoticePage;
