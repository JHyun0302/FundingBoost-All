import React from 'react';
import './mypage-friend-funding-history-page.scss';
import HeaderBar from "../../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer'
import MypageFriendHistory from '../../../organisms/contents/mypage-friend-history/mypage-friend-history';

function MypageFriendFundingHistoryPage() {
    return (
        <div className="Mypage-friend-funding-history-page">
            <HeaderBar/>
            <MypageFriendHistory/>
            <Footer/>
        </div>
    );
}

export default MypageFriendFundingHistoryPage;