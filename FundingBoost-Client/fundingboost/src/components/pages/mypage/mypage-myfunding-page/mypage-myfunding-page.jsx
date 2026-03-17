import React from 'react';
import HeaderBar from "../../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../../organisms/footer/footer'
import MypageMyfundingPane from '../../../organisms/contents/mypage-myfunding/mypage-myfunding'


function MypageMyfundingPage() {
    return (
        <div className="Mypage-pages">
            <HeaderBar />
            <MypageMyfundingPane />
            <Footer />
        </div>
    );
}

export default MypageMyfundingPage;