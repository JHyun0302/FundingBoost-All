import React from 'react';
import './footer.scss';
import logo from "../../../assets/logo.png";
import {
    MDBBtn,
    MDBIcon
} from 'mdb-react-ui-kit';

const Footer = () => {
    const repositories = [
        {
            name: "FundingBoost-Server",
            url: "https://github.com/JHyun0302/FundingBoost-Server"
        },
        {
            name: "FundingBoost-Client",
            url: "https://github.com/JHyun0302/FundingBoost-Client"
        },
        {
            name: "FundingBoost-DataCrawler",
            url: "https://github.com/JHyun0302/FundingBoost-DataCrawler"
        }
    ];

    return (
        <footer className="footer">
            <div className="container">
                <div className="footer-content">
                    <div className="footer-logo">
                        <img src={logo} alt="Logo" />
                    </div>
                    <div className="footer-text">
                        <h5>KCS TEAM1 FundingBoost</h5>
                        <ul className="footer-repo-list list-unstyled">
                            {repositories.map((repo) => (
                                <li key={repo.url} className="footer-repo-item">
                                    <span className="footer-repo-label">GitHub</span>
                                    <a
                                        href={repo.url}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="footer-repo-link"
                                    >
                                        {repo.name}
                                    </a>
                                    <span className="footer-repo-url">{repo.url}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                </div>
                <div>
                    <div className="icons-row">
                    <MDBBtn
                    rippleColor="dark"
                    color='link'
                    floating
                    size="lg"
                    className='text-dark m-1 '
                    href='https://www.facebook.com/'
                    role='button'
                >
                    <MDBIcon fab icon='facebook-f'  />
                </MDBBtn>

                <MDBBtn
                    rippleColor="dark"
                    color='link'
                    floating
                    size="lg"
                    className='text-dark m-1'
                    href='https://twitter.com/'
                    role='button'
                >
                    <MDBIcon fab icon='twitter'  />
                </MDBBtn>

                <MDBBtn
                    rippleColor="dark"
                    color='link'
                    floating
                    size="lg"
                    className='text-dark m-1'
                    href='https://www.google.com/'
                    role='button'
                >
                    <MDBIcon fab icon='google'  />
                </MDBBtn>
                <MDBBtn
                    rippleColor="dark"
                    color='link'
                    floating
                    size="lg"
                    className='text-dark m-1'
                    href='https://www.instagram.com/'
                    role='button'
                >
                    <MDBIcon fab icon='instagram' />
                </MDBBtn>

                <MDBBtn
                    rippleColor="dark"
                    color='link'
                    floating
                    size="lg"
                    className='text-dark m-1'
                    href='https://github.com/'
                    role='button'
                >
                    <MDBIcon fab icon='github' />
                </MDBBtn>
                    </div>
                </div>
                <div className="col-md-12 text-center">
                    <p>&copy; 2024 KAKAO CLOUD SCHOOL DEV. All rights reserved.</p>
                </div>
            </div>
        </footer>
    );
};

export default Footer;
