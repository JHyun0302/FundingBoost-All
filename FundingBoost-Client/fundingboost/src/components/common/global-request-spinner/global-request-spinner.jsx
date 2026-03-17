import React, { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { HashLoader } from 'react-spinners';
import './global-request-spinner.scss';

const SHOW_DELAY_MS = 180;
const MIN_VISIBLE_MS = 260;

const GlobalRequestSpinner = () => {
    const [visible, setVisible] = useState(false);
    const visibleRef = useRef(false);
    const inFlightRef = useRef(0);
    const visibleSinceRef = useRef(0);
    const showTimerRef = useRef(null);
    const hideTimerRef = useRef(null);

    useEffect(() => {
        visibleRef.current = visible;
    }, [visible]);

    const clearShowTimer = useCallback(() => {
        if (!showTimerRef.current) {
            return;
        }
        clearTimeout(showTimerRef.current);
        showTimerRef.current = null;
    }, []);

    const clearHideTimer = useCallback(() => {
        if (!hideTimerRef.current) {
            return;
        }
        clearTimeout(hideTimerRef.current);
        hideTimerRef.current = null;
    }, []);

    const openSpinner = useCallback(() => {
        clearHideTimer();
        if (visibleRef.current || showTimerRef.current) {
            return;
        }

        showTimerRef.current = setTimeout(() => {
            visibleSinceRef.current = Date.now();
            setVisible(true);
            showTimerRef.current = null;
        }, SHOW_DELAY_MS);
    }, [clearHideTimer]);

    const closeSpinner = useCallback(() => {
        clearShowTimer();
        if (!visibleRef.current) {
            return;
        }

        const elapsed = Date.now() - visibleSinceRef.current;
        const remaining = Math.max(0, MIN_VISIBLE_MS - elapsed);
        clearHideTimer();
        hideTimerRef.current = setTimeout(() => {
            setVisible(false);
            hideTimerRef.current = null;
        }, remaining);
    }, [clearHideTimer, clearShowTimer]);

    const handleRequestStart = useCallback(() => {
        inFlightRef.current += 1;
        if (inFlightRef.current === 1) {
            openSpinner();
        }
    }, [openSpinner]);

    const handleRequestEnd = useCallback(() => {
        inFlightRef.current = Math.max(0, inFlightRef.current - 1);
        if (inFlightRef.current === 0) {
            closeSpinner();
        }
    }, [closeSpinner]);

    useEffect(() => {
        const requestInterceptor = axios.interceptors.request.use(
            (config) => {
                if (!config?.skipGlobalLoading) {
                    handleRequestStart();
                }
                return config;
            },
            (error) => {
                if (!error?.config?.skipGlobalLoading) {
                    handleRequestEnd();
                }
                return Promise.reject(error);
            }
        );

        const responseInterceptor = axios.interceptors.response.use(
            (response) => {
                if (!response?.config?.skipGlobalLoading) {
                    handleRequestEnd();
                }
                return response;
            },
            (error) => {
                if (!error?.config?.skipGlobalLoading) {
                    handleRequestEnd();
                }
                return Promise.reject(error);
            }
        );

        return () => {
            axios.interceptors.request.eject(requestInterceptor);
            axios.interceptors.response.eject(responseInterceptor);
            clearShowTimer();
            clearHideTimer();
        };
    }, [clearHideTimer, clearShowTimer, handleRequestEnd, handleRequestStart]);

    useEffect(() => {
        if (typeof window === 'undefined' || typeof window.fetch !== 'function') {
            return undefined;
        }

        const nativeFetch = window.fetch.bind(window);
        const wrappedFetch = async (...args) => {
            const requestInit = args?.[1];
            const skipGlobalLoading = requestInit?.skipGlobalLoading === true;

            if (!skipGlobalLoading) {
                handleRequestStart();
            }

            try {
                return await nativeFetch(...args);
            } finally {
                if (!skipGlobalLoading) {
                    handleRequestEnd();
                }
            }
        };

        window.fetch = wrappedFetch;

        return () => {
            window.fetch = nativeFetch;
        };
    }, [handleRequestEnd, handleRequestStart]);

    return (
        <div className={`global-request-spinner ${visible ? 'visible' : ''}`} aria-hidden={!visible}>
            <div className="global-request-spinner__backdrop" />
            <div className="global-request-spinner__content" role="status" aria-live="polite">
                <HashLoader size={80} color="#ffd95d" />
                <p>데이터를 불러오는 중입니다...</p>
            </div>
        </div>
    );
};

export default GlobalRequestSpinner;
