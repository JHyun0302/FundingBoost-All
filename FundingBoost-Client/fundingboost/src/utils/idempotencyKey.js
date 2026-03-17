const fallbackKey = () => {
    const randomPart = Math.random().toString(36).slice(2, 12);
    return `idem_${Date.now().toString(36)}_${randomPart}`;
};

export const createIdempotencyKey = () => {
    if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
        return crypto.randomUUID();
    }
    return fallbackKey();
};
