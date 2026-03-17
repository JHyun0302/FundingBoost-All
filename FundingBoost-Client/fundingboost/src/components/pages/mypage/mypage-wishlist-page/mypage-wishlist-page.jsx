import React from 'react';
import './mypage-wishlist-page.scss';
import HeaderBar from "../../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer';
import MypageWishlist from "../../../organisms/contents/mypage-wishlist/mypage-wishlist";

function MypageWishPage() {
    return (
        <div className="Mypage-Order-history-page">
            <HeaderBar/>
            <MypageWishlist/>
            <Footer/>
        </div>
    );
}

export default MypageWishPage;