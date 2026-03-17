import React from 'react';
import './mypage-myfunding-history-page.scss';
import HeaderBar from "../../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer'
import MypageFundingHistoryPane from '../../../organisms/contents/mypage-myhistory/mypage-myhistory'

function MypageFundingHistoryPage() {
    return (
        <div className="Mypage-funding-history-page">
            <HeaderBar/>
            <MypageFundingHistoryPane/>
            <Footer/>
        </div>
    );
}

export default MypageFundingHistoryPage;