const isProduction = process.env.NODE_ENV === "production";
const keepDebugLogs = process.env.REACT_APP_ENABLE_DEBUG_LOGS === "true";

export const applyConsoleControl = () => {
    if (!isProduction || keepDebugLogs) {
        return;
    }

    const noop = () => {};

    // 운영 번들에서는 콘솔 출력 전체를 숨긴다.
    console.log = noop;
    console.info = noop;
    console.debug = noop;
    console.warn = noop;
    console.error = noop;
};
