import React from 'react';
import Button from 'react-bootstrap/Button';
import { useNavigate, useParams } from 'react-router-dom';
import '../yellowBtn.scss';


function ShoppingNowBtn() {
    const navigate = useNavigate();

    return (

        <div className="shoppingNowBtn">
                <Button className="yellowBtn" href='/shopping'>쇼핑하러가기</Button>

        </div>
    );
}

export default ShoppingNowBtn;
