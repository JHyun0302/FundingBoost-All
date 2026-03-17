import React from 'react';
import './App.css';
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Router from '../src/router';
import GlobalRequestSpinner from './components/common/global-request-spinner/global-request-spinner';

function App() {

    return (
        <>
            <GlobalRequestSpinner />
            <Router />
        </>
    );
}

export default App;
