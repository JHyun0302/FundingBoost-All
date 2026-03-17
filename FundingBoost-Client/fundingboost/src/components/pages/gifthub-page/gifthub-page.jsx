import React from 'react';
import { useEffect, useState } from 'react';
import axios from 'axios';
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer'
import GifthubPane from '../../organisms/contents/gifthub/gifthub'

export function GifthubPage() {

    return (
        <div className="gifthub-pages">
            <HeaderBar />
            <GifthubPane />
            <Footer />
        </div>
    );
}

export default GifthubPage;