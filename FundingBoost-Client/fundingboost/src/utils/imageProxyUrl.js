const IMAGE_PROXY_PATH = process.env.REACT_APP_IMAGE_PROXY_PATH || "/img-proxy";
const IMAGE_PROXY_THUMB_PATH = `${IMAGE_PROXY_PATH}-thumb`;
const IMAGE_PROXY_LARGE_PATH = `${IMAGE_PROXY_PATH}-large`;
const HTTP_URL_PATTERN = /^https?:\/\//i;
const PROTOCOL_RELATIVE_PATTERN = /^\/\//;
const DATA_OR_BLOB_PATTERN = /^(data:|blob:)/i;
const LARGE_PROXY_THRESHOLD = 400;
const KAKAO_THUMB_HOST = "img1.kakaocdn.net";

const encodeProxyParam = (targetUrl) =>
    encodeURI(targetUrl)
        .replace(/#/g, "%23")
        .replace(/&/g, "%26")
        .replace(/;/g, "%3B");

const toPositiveInt = (value, fallback) => {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
        return fallback;
    }
    return Math.floor(parsed);
};

const hasResizeHint = (options = {}) =>
    toPositiveInt(options.width, 0) > 0 || toPositiveInt(options.height, 0) > 0;

const resolveProxyPath = (options = {}) => {
    if (!hasResizeHint(options)) {
        return IMAGE_PROXY_PATH;
    }

    const width = toPositiveInt(options.width, 0);
    const height = toPositiveInt(options.height, 0);
    const longestEdge = Math.max(width, height);

    if (longestEdge > LARGE_PROXY_THRESHOLD) {
        return IMAGE_PROXY_LARGE_PATH;
    }
    return IMAGE_PROXY_THUMB_PATH;
};

const safeDecodeURIComponent = (value) => {
    if (typeof value !== "string") {
        return value;
    }

    let decoded = value;
    for (let i = 0; i < 3; i += 1) {
        try {
            const next = decodeURIComponent(decoded);
            if (next === decoded) {
                break;
            }
            decoded = next;
        } catch (error) {
            break;
        }
    }
    return decoded;
};

const toCanonicalSourceUrl = (sourceUrl, parsedSourceUrl) => {
    if (
        parsedSourceUrl.hostname.toLowerCase() !== KAKAO_THUMB_HOST ||
        !parsedSourceUrl.pathname.startsWith("/thumb/")
    ) {
        return sourceUrl;
    }

    const rawFname = parsedSourceUrl.searchParams.get("fname");
    if (!rawFname) {
        return sourceUrl;
    }

    const decodedFname = safeDecodeURIComponent(rawFname.trim());
    if (!HTTP_URL_PATTERN.test(decodedFname)) {
        return sourceUrl;
    }

    try {
        return new URL(decodedFname).toString();
    } catch (error) {
        return sourceUrl;
    }
};

const buildProxyUrl = (sourceUrl, options = {}) => {
    const parsed = new URL(sourceUrl);
    const targetUrl = toCanonicalSourceUrl(sourceUrl, parsed);
    const proxyPath = resolveProxyPath(options);

    return `${proxyPath}?url=${encodeProxyParam(targetUrl)}`;
};

export const toImageProxyUrl = (value, options = {}) => {
    if (typeof value !== "string") {
        return value;
    }

    const url = value.trim();
    if (!url) {
        return url;
    }

    if (DATA_OR_BLOB_PATTERN.test(url)) {
        return url;
    }

    if (url.startsWith(IMAGE_PROXY_PATH)) {
        return url;
    }

    if (HTTP_URL_PATTERN.test(url)) {
        try {
            return buildProxyUrl(url, options);
        } catch (error) {
            return `${IMAGE_PROXY_PATH}?url=${encodeProxyParam(url)}`;
        }
    }

    if (PROTOCOL_RELATIVE_PATTERN.test(url)) {
        const normalized = `https:${url}`;
        try {
            return buildProxyUrl(normalized, options);
        } catch (error) {
            return `${IMAGE_PROXY_PATH}?url=${encodeProxyParam(normalized)}`;
        }
    }

    if (url.startsWith("/")) {
        return url;
    }

    return `/${url}`;
};
