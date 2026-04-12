# Routine Impulse

A routine management application with a Quarkus backend and React frontend.

## Quick Start

### Prerequisites
- Docker & Docker Compose

### Build Docker Image

```bash
docker build -t routine-impulse .
```

### Run with Docker Compose

1. Copy the environment template:
```bash
cp .env.template .env
```

2. Generate JWT keys:
```bash
mkdir -p .secrets/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out .secrets/jwt/privateKey.pem
openssl rsa -pubout -in .secrets/jwt/privateKey.pem -out .secrets/jwt/publicKey.pem
```

3. Update `.env` if needed (DB/JWT issuer and key file paths).

4. Start the services:
```bash
docker compose up
```

Access the application at `http://localhost:8080`