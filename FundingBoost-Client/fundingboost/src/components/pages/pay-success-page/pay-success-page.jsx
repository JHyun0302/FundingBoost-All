import React from 'react';
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import PaySuccessPane from '../../organisms/contents/paysuccess/paysuccess'

function PaySuccessPage() {
    return (
        <div className="mypay-pages">
            <HeaderBar />
            <PaySuccessPane />
            <Footer />
        </div>
    );
}

export default PaySuccessPage;