import React from 'react';
import './mypage-delivery-management-page.scss';
import HeaderBar from "../../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer'
import MypageDeliveryPane from "../../../organisms/contents/mypage-delivery-manage/mypage-delivery-mange";

function MypageDeliveryManagementPage() {
    return (
        <div className="Mypage-delivery-mangement-page">
            <HeaderBar/>
            <MypageDeliveryPane/>
            <Footer/>
        </div>
    );
}

export default MypageDeliveryManagementPage;