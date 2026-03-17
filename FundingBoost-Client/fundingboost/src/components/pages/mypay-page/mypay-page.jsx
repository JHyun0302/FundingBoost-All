import React from 'react';
import './mypay-page.scss'
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import MypayPane from '../../organisms/contents/mypay/mypay';


function MypayPage() {
    return (
        <div className="mypay-pages">
            <HeaderBar />
            <MypayPane/>
            <Footer />
        </div>
    );
}

export default MypayPage;