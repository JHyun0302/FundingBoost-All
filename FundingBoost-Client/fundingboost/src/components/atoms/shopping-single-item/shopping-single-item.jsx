import React from 'react';
import style from './shopping-single-item.scss';
import { Link } from 'react-router-dom';
import { toImageProxyUrl } from '../../../utils/imageProxyUrl';

export default function ShoppingSingleItem({ product }) {
    if (!product) {
        return null;
    }

    const { itemId, itemName, price, itemImageUrl, brandName } = product;


    // console.log(product);

    const truncatedTitle = itemName.length > 25 ? itemName.slice(0, 25) + '...' : itemName;

    return (
        // "/shopping/detail/" + itemId
        <Link to={"/shopping/detail/" + itemId} className={style.product} style={{textDecoration: 'none'}}>
            <div className="shopring-item-img-title">
                <div className={style.imgwrap}>
                    {/* 상품 품절 여부에 따라 표시 */}
                    <div className={style.soldout}>
                        {/*<p className={style.text}>{price > 0 ? "판매중" : "Sold Out"}</p>*/}
                        <div className={style.bg}></div>
                    </div>
                    <div className="img-wrapper">
                        <img
                            src={toImageProxyUrl(itemImageUrl, {
                                width: 320,
                                height: 320,
                                quality: 80
                            })}
                            width="100%"
                            style={{borderRadius: '3px'}}
                            alt={itemName}
                            loading="lazy"
                            decoding="async"
                        />
                    </div>
                </div>
                <div className="brand-wrapper">
                    <p className={style.brand}>{brandName}</p>
                </div>
                <div className="title-wrapper">
                    <div className="title-style">{truncatedTitle}</div>
                </div>
            </div>
            <div className="shopring-item-price">
                <div className="price-wrapper">
                    <p className={style.price}>{price.toLocaleString()}원</p>
                </div>
            </div>

        </Link>
    );
}
