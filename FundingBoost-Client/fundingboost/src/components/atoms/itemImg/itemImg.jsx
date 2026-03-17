//item 이미지
import React from 'react';
import './itemImg.scss';
import NonItemImg from '../../../assets/nonItemImg.svg';
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";


function ItemImg({imageUrl}){

        return (
            <div className="item-img-container">
                {imageUrl ? <img src={toImageProxyUrl(imageUrl)} alt="Item" className="item-img"/> :
                    <img src={NonItemImg} alt="No Item" className="non-item-img"/>}
        </div>
        );


}

export default ItemImg;
