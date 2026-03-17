import React from 'react';
import './shopping-page.scss'
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import ShoppingPane from '../../organisms/contents/shopping/shopping'

function ShoppingPage() {
    return (
        <div className="shopping-pages">
            <HeaderBar />
            <ShoppingPane/>
            <Footer />
        </div>
    );
}

export default ShoppingPage;