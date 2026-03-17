import React from 'react';
import './order-pay-page.scss'
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import OrderPayPane from '../../organisms/contents/order-pay/order-pay'


function OrderPayPage() {
    return (
        <div className="mypay-pages">
            <HeaderBar />
            <OrderPayPane/>
            <Footer />
        </div>
    );
}

export default OrderPayPage;