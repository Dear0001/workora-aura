FROM golang:1.24.9-alpine AS builder

ARG MINIO_VERSION=RELEASE.2024-10-13T13-34-11Z
ARG MC_VERSION=RELEASE.2025-08-13T08-35-41Z

RUN CGO_ENABLED=0 go install "github.com/minio/minio@${MINIO_VERSION}" \
    && CGO_ENABLED=0 go install "github.com/minio/mc@${MC_VERSION}"

FROM alpine:3.22

RUN apk add --no-cache ca-certificates curl

COPY --from=builder /go/bin/minio /usr/local/bin/minio
COPY --from=builder /go/bin/mc /usr/local/bin/mc

EXPOSE 9000 9001
VOLUME ["/data"]

ENTRYPOINT ["/usr/local/bin/minio"]
CMD ["server", "/data", "--console-address", ":9001"]
