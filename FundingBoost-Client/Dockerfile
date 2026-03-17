FROM node:20-alpine AS build

WORKDIR /app/fundingboost
COPY fundingboost/package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY fundingboost/ ./

ARG REACT_APP_FUNDINGBOOST=/api/v1
ARG REACT_APP_FUNDINGBOOST_V3=/api/v3
ENV REACT_APP_FUNDINGBOOST=$REACT_APP_FUNDINGBOOST
ENV REACT_APP_FUNDINGBOOST_V3=$REACT_APP_FUNDINGBOOST_V3

RUN npm run build

FROM node:20-alpine

WORKDIR /app
RUN npm install -g serve
COPY --from=build /app/fundingboost/build ./build

EXPOSE 3000

CMD ["serve", "-s", "build", "-l", "3000"]
