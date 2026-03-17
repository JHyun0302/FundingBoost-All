import React from 'react';
import './error-handle-page.scss';
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import ErrorHandle from '../../organisms/contents/error-handle/error-handle'

function FundingSuccessPage() {
    return (
        <div className="Funding-Success-pages">
            <HeaderBar />
            <ErrorHandle/>
            <Footer />
        </div>
    );
}

export default FundingSuccessPage;