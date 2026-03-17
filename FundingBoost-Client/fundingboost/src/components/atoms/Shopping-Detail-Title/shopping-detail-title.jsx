import React from "react";
import "./shopping-detail-title.scss";
import ShoppingDetailMenu from "../Shopping-Detail-Menu/shopping-detail-menu";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function ShoppingDetailItem({ itemId, itemName, itemThumbnailImageUrl, itemPrice, options, bookmark }) {

    return (
        <div className="ShoppingDetailItemBox">
            <div className="ShoppingDetailItemBoxView">
                <div className="shopping-detail-title-wrapper">
                    <img className="image" alt="Image" src={toImageProxyUrl(itemThumbnailImageUrl)} />
                    <div className="shopping-detail-column">
                        {itemName && (
                            <div className="itemName">
                                {itemName.length > 40 ? itemName.slice(0, 40) : itemName}
                                {itemName.length > 40 && <br />}
                                {itemName.length > 40 && itemName.slice(40)}
                            </div>
                        )}

                        <ShoppingDetailMenu
                            itemId={itemId}
                            itemThumbnailImageUrl={itemThumbnailImageUrl}
                            itemName={itemName}
                            itemPrice={itemPrice}
                            options={options}
                            bookmark={bookmark}
                        />

                    </div>
                </div>
            </div>
        </div>
    );
}
