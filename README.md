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
cp env.template .env
```

2. Update `.env`

3. Start the services:
```bash
docker compose up
```

Access the application at `http://localhost:8080`