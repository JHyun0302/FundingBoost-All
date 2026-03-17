import React from 'react';
import './funding-success-page.scss';
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import FundingSuccessPane from '../../organisms/contents/fundingsuccess/fundingsuccess'

function FundingSuccessPage() {
    return (
        <div className="Funding-Success-pages">
            <HeaderBar />
            <FundingSuccessPane />
            <Footer />
        </div>
    );
}

export default FundingSuccessPage;