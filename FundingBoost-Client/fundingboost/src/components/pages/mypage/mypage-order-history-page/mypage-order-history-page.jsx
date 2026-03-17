import React from 'react';
import './mypage-order-history-page.scss';
import HeaderBar from "../../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer'
import OrderHistoryPane from "../../../organisms/contents/mypage-order-history/mypage-order-history";

function MypageOrderHistoryPage() {
    return (
        <div className="Mypage-Order-history-page">
            <HeaderBar/>
            <OrderHistoryPane/>
            <Footer/>
        </div>
    );
}

export default MypageOrderHistoryPage;