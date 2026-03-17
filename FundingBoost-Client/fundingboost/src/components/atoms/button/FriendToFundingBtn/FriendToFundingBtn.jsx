import React from 'react';
import Button from 'react-bootstrap/Button';
import { useNavigate, useParams } from 'react-router-dom';
import '../yellowBtn.scss';


function FriendToFundingBtn({ fundingAmount }) {
    const navigate = useNavigate();
    const { fundingId } = useParams()
    console.log(fundingId);
    const handleClick = () => {
        navigate(`/friend-funding/pay/${fundingId}`, { state: fundingAmount });
    };

    return (

        <div className="friendToFundingBtn-btn">
            {fundingAmount !== 0 ? (
                <Button className="yellowBtn" onClick={handleClick}>펀딩하기</Button>
            ) : (
                <Button className="yellowBtn"  style={{ backgroundColor: "#D6D6D6", color : "black" }} disabled>펀딩하기</Button>
            )}
        </div>
    );
}

export default FriendToFundingBtn;
