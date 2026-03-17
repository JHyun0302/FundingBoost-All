import React from 'react';
import './shopping-detail-page.scss'
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer';
import ShoppingDetailPane from '../../organisms/contents/shopping-detail/shopping-detail';


function MypayPage() {
    return (
        <div className="shopping-detail-pages">
            <HeaderBar />
            <ShoppingDetailPane/>
            <Footer />
        </div>
    );
}

export default MypayPage;