import React from 'react';
import style from './FundingItem.scss';
import ItemImg from "../../../atoms/itemImg/itemImg";
import {Link} from "react-router-dom";
import { formatPrice } from '../../../../utils/formats';
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";


export default function Mainrankingitem({ products }) {

    if (!products) {
        return null;
    }

    function FundingItem(products) {


        const price = products.price || 10000; // 기본값으로 10000 설정

        const truncatedTitle = products.title.length > 25 ? products.title.slice(0, 25) + '...' : products.title;
        return (
            <div className="FundingItem">
                <Link to={"/products/" + products.id} className={style.products}>
                    <img src={toImageProxyUrl(products.thumbnail)} width="100%" style={{borderRadius: '3px'}} alt={products.title}/>
                    <div className="title-wrapper">
                        <h4 className={style.itemName}>{truncatedTitle}</h4>
                    </div>
                    <div className="price-wrapper">
                        <p className={style.itemPrice}>{formatPrice(price)}원</p>
                    </div>

                </Link>


            </div>
        );
    };
}
