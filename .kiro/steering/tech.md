# Technology Stack & Build System

## Backend Stack
- **Java 21** with **Spring Boot 3.2.0**
- **Spring Security** for authentication and authorization
- **Spring Data JPA** with **Hibernate**
- **PostgreSQL** database with **Flyway** migrations
- **JWT** for token-based authentication with httpOnly cookies
- **Plaid Java SDK** for banking integrations
- **OpenAI Java Client** for AI features
- **Maven** for dependency management and builds

## Frontend Stack
- **React 18** with **TypeScript**
- **Vite** for fast development and building
- **Tailwind CSS** for styling with custom design system
- **shadcn/ui** component library
- **Recharts** for data visualization
- **React Router** for navigation
- **Axios** for API communication
- **React Query** for state management
- **React Plaid Link** for bank connections

## Development Tools
- **Docker Compose** for local development environment
- **Spring Boot DevTools** for hot reload
- **Vitest** for frontend testing
- **JUnit 5** for backend testing
- **Testcontainers** for integration testing

## Common Commands

### Backend Development
```bash
cd backend

# Run application
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run tests
./mvnw test

# Database migrations
./mvnw flyway:migrate

# Skip tests during build
./mvnw spring-boot:run -Dmaven.test.skip=true
```

### Frontend Development
```bash
cd frontend

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm run test

# Lint code
npm run lint
```

### Docker Development
```bash
# Start database only
docker-compose up postgres -d

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend

# Rebuild services
docker-compose up --build
```

## Environment Configuration
- Backend uses `.env` files with Spring profiles (local, dev, plaid, mock)
- Frontend uses `.env.local` for Vite environment variables
- Docker Compose uses root `.env` file for container configuration