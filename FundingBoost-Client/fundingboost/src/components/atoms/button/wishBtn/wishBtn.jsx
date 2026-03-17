import React, {useEffect} from 'react';
import wish from "../../../../assets/emptyheart.svg";
import wishFillHeart from "../../../../assets/fillheart.svg";
import './wishBtn.scss'
import axios from "axios";

const WishBtn = ({itemId, bookmark, onNonMemberModalOpen}) => {
    const [isWish, setIsWish] = React.useState(bookmark);

    //bookmark 상태
    useEffect(() => {
        setIsWish(bookmark);
    }, [bookmark]);


    const clickWishBtn = async () => {
        const wishState = !isWish;
        let accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            onNonMemberModalOpen();
            return;
        }

        if (localStorage.getItem('accessToken') != null) {
            accessToken = localStorage.getItem('accessToken');
        }

        try{
            const response = await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/bookmark/like/${itemId}`,{},{
                responseType: 'json',
                headers: ({
                    "Content-Type" : "application/json",
                    "ngrok-skip-browser-warning": true,
                    "Authorization": `Bearer ${accessToken}`
                }),
                withCredentials: true,
            });
            console.log("post:",response.data);
            // setIsWish(!isWish);
            if (response.data.success && response.data.data.isSuccess) {
                setIsWish(wishState);
            } else {
                setIsWish(isWish);
            }

        } catch (error) {
            setIsWish(isWish);
            console.error("wishBtn Post Error :", error);
        }
    };

    return (
        <div className="wishBtn">
            <button className="heartIconWrapper" onClick={clickWishBtn}>
                <img className="heartIcon" alt="heartIcon" src={!isWish ? wish: wishFillHeart} />
            </button>
        </div>
    );
};

export default WishBtn;