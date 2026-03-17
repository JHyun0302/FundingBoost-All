// import axios from 'axios';
// import { useSetRecoilState } from 'recoil';
// import { loginState } from '../state/auth';
//
// const axiosInstance = axios.create({
//     baseURL: process.env.REACT_APP_FUNDINGBOOST,
// });
//
// const setupInterceptors = (setLogin) => {
//     axiosInstance.interceptors.response.use(
//         response => response,
//         async error => {
//             const originalRequest = error.config;
//             if (error.response && error.response.status === 401 && !originalRequest._retry) {
//                 originalRequest._retry = true;
//                 try {
//                     const refreshToken = localStorage.getItem('refreshToken');
//                     if (!refreshToken) throw new Error('No refresh token available');
//
//                     const { data } = await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/access-reissue`, {}, {
//                         headers: {
//                             Authorization: `Bearer ${refreshToken}`,
//                         },
//                     });
//
//                     setLogin(prev => ({ ...prev, accessToken: data.accessToken }));
//                     localStorage.setItem('accessToken', data.accessToken);
//                     axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${data.accessToken}`;
//
//                     // 재발급 받은 토큰으로 원래 요청 재시도
//                     originalRequest.headers['Authorization'] = `Bearer ${data.accessToken}`;
//                     return axiosInstance(originalRequest);
//                 } catch (err) {
//                     setLogin({ accessToken: null, refreshToken: null });
//                     localStorage.removeItem('accessToken');
//                     localStorage.removeItem('refreshToken');
//                     return Promise.reject(err);
//                 }
//             }
//             return Promise.reject(error);
//         }
//     );
// };
//
// export default axiosInstance;
// export { setupInterceptors };
