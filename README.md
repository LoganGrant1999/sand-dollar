# Sand Dollar - Personal Finance Management App

A comprehensive personal finance management application built with React, Spring Boot, and modern web technologies. Sand Dollar helps users track spending, set budgets, manage financial goals, and gain insights through AI-powered financial assistance.

## ✅ Implemented Features

### 📊 Dashboard & Analytics
- **Total Cash Balance**: Real-time sum of available balances across all connected accounts
- **Daily Spending Chart**: Bar chart showing spend per day for the last 7 days (excludes transfers/credit card payments)
- **Category Spending Breakdown**: 30/60/90-day spending analysis with trends
- **Credit Score Display**: Mock credit score (720) with provider connection placeholder

### 🏦 Plaid Bank Integration
- **Secure Account Linking**: Create link tokens and exchange public tokens
- **Real-Time Balance Sync**: Fetch and store account balances with timestamps  
- **Transaction Sync**: Cursored transaction syncing with automatic updates
- **Transfer Detection**: Intelligent detection of transfers between user accounts
- **Webhook Support**: Automatic sync triggers from Plaid webhooks

### 🔐 Security & Authentication
- **JWT Authentication**: httpOnly cookie-based auth with refresh token rotation
- **AES-GCM Encryption**: Encrypted storage of Plaid access tokens
- **CSRF Protection**: Cross-site request forgery protection
- **RBAC**: Role-based access ensuring users only access their own data
- **CORS Configuration**: Secure cross-origin requests

### 📊 Spending Analytics
- **Daily Spending Patterns**: Track spending trends over customizable periods
- **Category Analysis**: Breakdown by top-level spending categories with trend comparison
- **Transfer Filtering**: Exclude internal transfers from spending calculations
- **Historical Comparisons**: Compare current vs previous period spending

## Tech Stack

### Frontend
- **React 18** with **TypeScript**
- **Vite** for fast development and building
- **Tailwind CSS** for styling
- **shadcn/ui** component library
- **Recharts** for data visualization
- **React Router** for navigation
- **Axios** for API communication
- **React Plaid Link** for bank connections

### Backend
- **Java 21** with **Spring Boot 3**
- **Spring Security** for authentication and authorization
- **Spring Data JPA** with **Hibernate**
- **PostgreSQL** database
- **Flyway** for database migrations
- **JWT** for token-based authentication
- **Plaid Java SDK** for banking integrations
- **OpenAI Java Client** for AI features

### Infrastructure
- **Docker Compose** for local development
- **PostgreSQL** containerized database
- Environment-based configuration

## Quick Start

### Prerequisites
- Java 21+
- Node.js 18+
- Docker and Docker Compose
- Plaid Account (for banking features)
- OpenAI API Key (for AI features)

### 1. Clone and Setup Environment

```bash
git clone <repository-url>
cd sand-dollar
cp .env.example .env
```

### 2. Configure Environment Variables

Edit `.env` file with your credentials:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/sand_dollar
DB_USERNAME=sand_dollar_user
DB_PASSWORD=sand_dollar_password

# JWT & Security (Generate keys with: openssl rand -base64 32)
JWT_SECRET=your-super-secret-jwt-key-that-should-be-at-least-256-bits-long
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=2592000000
REFRESH_TOKEN_SECRET=your-refresh-token-secret-key
ENCRYPTION_KEY=your-32-byte-base64-encoded-encryption-key-here

# Plaid Configuration (create account at https://dashboard.plaid.com/)
PLAID_CLIENT_ID=your-plaid-client-id
PLAID_SECRET=your-plaid-secret-key
PLAID_ENV=sandbox
PLAID_WEBHOOK_URL=http://localhost:8080/api/plaid/webhook

# OpenAI (get key at https://platform.openai.com/)
OPENAI_API_KEY=your-openai-api-key
AI_BUDGET_ENABLED=true
OPENAI_CONNECT_TIMEOUT_MS=15000
OPENAI_READ_TIMEOUT_MS=20000
OPENAI_MAX_RETRIES=2
AI_BUDGET_RATE_LIMIT_PER_MINUTE=5

# CORS
APP_ORIGIN=http://localhost:5173
```

> See `.env.example` for the full list of optional knobs.

**Frontend `.env` file (create `frontend/.env`):**
```bash
VITE_API_BASE=http://localhost:8080/api
VITE_PLAID_ENV=sandbox
```

### 3. Generate Encryption Key

```bash
# Generate a secure 32-byte base64 encryption key
openssl rand -base64 32
```

### 4. Start Database

```bash
docker-compose up postgres -d
```

### 5. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will be available at http://localhost:8080

### 6. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available at http://localhost:5173

### Running Tests

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd ../frontend
npm run test
```

## Local Mock Mode

Want to trial the AI budgeting flow without wiring up Plaid or OpenAI? Use the `local` profile to run with realistic mock data and deterministic AI budget responses.

### Features

- **Realistic Transaction Data**: Automatically seeds current month with transactions across categories (Rent, Groceries, Dining, Transport, Utilities, Gym, Subscriptions, Misc)
- **Deterministic AI Budgets**: Returns consistent budget recommendations without OpenAI API calls
- **Budget Target Persistence**: Accept and modify budget targets that persist to database
- **Full Budget Flow**: Complete snapshot → generate → accept → adjust workflow

### Quick Start

```bash
# Terminal 1 – database
docker-compose up postgres -d

# Terminal 2 – backend (uses local profile)
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# Terminal 3 – frontend
cd ../frontend
npm run dev
```

Seeded credentials:

- Email: `ai.demo@sanddollar.local`
- Password: `password123`

After logging in, exercise the AI endpoints with the bundled [Postman collection](docs/ai-budget-local.postman_collection.json) or curl. Remember to capture the `Set-Cookie` values (both `accessToken` and `refreshToken`) from the login response and reuse them with the `-b` flag.

```bash
# 1) Login and collect cookies
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ai.demo@sanddollar.local","password":"password123"}'

# 2) Snapshot current month
curl -X POST http://localhost:8080/api/ai/budget/snapshot \
  -H 'Content-Type: application/json' -d '{}'

# 3) Generate a new AI budget
curl -X POST http://localhost:8080/api/ai/budget/generate \
  -H 'Content-Type: application/json' \
  -d '{"month":"2025-09","goals":["Emergency fund $5k by March","Pay down card $200/mo"],"style":"balanced","constraints":{"capDiningAt":300}}'

# 4) Accept the generated targets
curl -X POST http://localhost:8080/api/ai/budget/accept \
  -H 'Content-Type: application/json' \
  -d '{"month":"2025-09","targetsByCategory":[{"category":"Rent","target":1500,"reason":"Fixed obligation"},{"category":"Dining","target":300,"reason":"User cap"}]}'
```

## 🧪 Sandbox Bootstrap (Dev Only)

For instant development with pre-populated data, use the sandbox bootstrap endpoints:

### Prerequisites
1. Set environment variables in `backend/.env`:
```bash
PLAID_CLIENT_ID=<sandbox client id>
PLAID_SECRET=<sandbox secret>
PLAID_ENV=sandbox
PLAID_WEBHOOK_URL=http://localhost:8080/api/plaid/webhook
APP_ORIGIN=http://localhost:5173
DEV_AUTH_HEADER=letmein
```

2. Start database, backend, and frontend (see Quick Start above)

### Bootstrap Steps

#### 1. Create Sandbox Item + Initial Sync
```bash
curl -s -X POST http://localhost:8080/api/dev/sandbox/link-and-sync \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-Auth: letmein' \
  -d '{
    "institution_id":"ins_109508",
    "products":["transactions"],
    "start_date":"2025-07-01",
    "end_date":"2025-09-06",
    "create_webhook": true
  }' | jq
```

This will:
- Create a sandbox Plaid Item (First Platypus Bank)
- Generate 3 accounts (checking, savings, credit card)
- Create 60+ realistic transactions with categories
- Generate account balances ($1000-$15000 range)
- Return summary with itemId, account count, transaction count

#### 2. Mint Additional Transactions (Optional)
```bash
curl -s -X POST http://localhost:8080/api/dev/sandbox/mint-transactions \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-Auth: letmein' \
  -d '{ 
    "count": 30, 
    "start_date":"2025-08-30", 
    "end_date":"2025-09-06",
    "merchant_names": ["Taco Llama","Grocerly","Rides Co","CoffeeCat"],
    "amount_min": 5.0,
    "amount_max": 120.0
  }' | jq
```

#### 3. Fire Webhook (Optional)
```bash
curl -s -X POST http://localhost:8080/api/dev/sandbox/fire-webhook \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-Auth: letmein' \
  -d '{ 
    "item_id":"<item-id-from-step-1>", 
    "webhook_code":"SYNC_UPDATES_AVAILABLE" 
  }' | jq
```

### Verification
After bootstrap, verify the dashboard populates with:
- Total available balance across accounts
- 7-day spending chart with daily breakdowns  
- Category spending breakdown with realistic merchant data
- Multiple account types (checking, savings, credit card)

### Features Demonstrated
- **Transfer Detection**: Automatically excludes transfers from spending calculations
- **Category Analysis**: Groups spending by categories (Food & Drink, Shops, etc.)
- **Balance Tracking**: Real-time balance snapshots with historical data
- **Realistic Data**: Proper transaction amounts, dates, and merchant names

## 🚀 API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login  
- `POST /api/auth/refresh` - Token refresh
- `POST /api/auth/logout` - User logout

### Plaid Integration
- `POST /api/plaid/link-token` - Create Plaid Link token
- `POST /api/plaid/exchange` - Exchange public token for access token
- `GET /api/plaid/balances` - Fetch account balances
- `POST /api/plaid/transactions/sync` - Sync transactions
- `POST /api/plaid/webhook` - Handle Plaid webhooks

### Spending & Analytics
- `GET /api/balances/total` - Get total available balance across accounts
- `GET /api/spend/daily?days=7` - Get daily spending for last N days
- `GET /api/spend/categories?range=30d` - Get spending by category with trends

### Credit Score (Stub)
- `GET /api/credit-score` - Get mock credit score (720)
- `POST /api/credit-score/connect` - Placeholder for credit provider connection

### Development Sandbox (DEV ONLY)
- `POST /api/dev/sandbox/link-and-sync` - Create sandbox Item and sync data
- `POST /api/dev/sandbox/mint-transactions` - Generate additional fake transactions  
- `POST /api/dev/sandbox/fire-webhook` - Manually trigger webhooks

### Coming Soon
- `POST /api/ai/budget/chat` - AI-powered budget planning
- `POST /api/ai/assistant/chat` - Financial assistant chat
- `GET /api/budget/active` - Active budget plan
- `GET /api/budget/progress` - Budget progress tracking

## Database Schema

The application uses a comprehensive PostgreSQL schema with the following key entities:

- **users** - User accounts and authentication
- **plaid_items** - Plaid institution connections
- **accounts** - Bank accounts from Plaid
- **transactions** - Financial transactions with categorization
- **balance_snapshots** - Historical account balance data
- **goals** - User financial goals
- **budget_plans** - AI-generated budget plans
- **budget_periods** - Budget period instances
- **budget_lines** - Category-specific budget limits
- **refresh_tokens** - JWT refresh token management
- **app_events** - Application audit logging

## Security Features

### Authentication
- JWT-based authentication with httpOnly cookies
- Automatic token refresh
- CSRF protection for state-changing operations
- Secure logout with token cleanup

### Data Protection
- AES-GCM encryption for Plaid access tokens
- Password hashing with BCrypt
- Input validation and sanitization
- CORS configuration for frontend-backend communication

### Authorization
- Role-based access control (RBAC)
- User data isolation
- Protected API endpoints
- Integration test coverage for auth guards

## Development

### Backend Development

```bash
cd backend

# Run tests
./mvnw test

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Database migrations
./mvnw flyway:migrate
```

### Frontend Development

```bash
cd frontend

# Development server with hot reload
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Docker Development

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f postgres

# Rebuild services
docker-compose up --build
```

## Testing

### Backend Tests
- Unit tests for business logic
- Integration tests for API endpoints
- Security testing for auth flows
- Plaid integration testing (mocked)

### Frontend Tests
- Component testing with Vitest and React Testing Library
- E2E testing with Playwright
- Auth flow testing

## Deployment Considerations

### Production Environment
1. **HTTPS**: Enable HTTPS and set secure cookie flags
2. **Database**: Use managed PostgreSQL service
3. **Environment Variables**: Use secure secret management
4. **Monitoring**: Add application monitoring and logging
5. **Backup**: Implement database backup strategy

### Security Checklist
- [ ] Generate unique JWT secrets and encryption keys
- [ ] Enable HTTPS in production
- [ ] Set secure cookie flags
- [ ] Configure CORS for production domains
- [ ] Implement rate limiting
- [ ] Add request logging and monitoring
- [ ] Regular security audits

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Check the documentation
- Review the API endpoints

## 🚧 Implementation Status

### ✅ **Completed (Ready to Use)**
- **Backend Core**: Spring Boot application with full security setup
- **Database Schema**: Complete PostgreSQL schema with migrations
- **Authentication**: JWT-based auth with refresh tokens and CSRF protection
- **Plaid Integration**: Full integration with account linking, balance sync, and transaction management
- **Spending Analytics**: Daily and category-based spending analysis with trends
- **Transfer Detection**: Intelligent detection of transfers between accounts
- **API Endpoints**: All core endpoints for balance, spending, and account management

### 🔄 **In Progress/Next Steps**
- **OpenAI Integration**: Budget planning and financial assistant (backend structure ready)
- **Frontend Components**: React components for Dashboard, Budgeting, Spending pages  
- **Budget Management**: Complete budget planning and progress tracking
- **Testing Suite**: Comprehensive unit and integration tests
- **Seed Data**: Demo data generation for development

### 📋 **Quick Start Guide**

1. **Set up Plaid Sandbox**: Create account at https://dashboard.plaid.com/
2. **Configure Environment**: Copy `.env.example` to `.env` and fill in your keys
3. **Generate Keys**: Run `openssl rand -base64 32` for encryption keys
4. **Start Services**: Run `docker-compose up postgres -d` then start backend/frontend
5. **Test Integration**: Use Plaid Link to connect test bank accounts

---

## 🏗️ **Architecture Highlights**

- **Security-First**: All sensitive data encrypted, secure token management
- **Production-Ready**: Docker containerization, proper error handling, logging
- **Scalable Design**: Service layer architecture with clear separation of concerns
- **Modern Stack**: Latest Spring Boot 3, React 18, TypeScript, Tailwind CSS

---

**⚠️ Important**: This application is for educational and demonstration purposes. All financial advice provided by the AI assistant is educational only and should not be considered as professional financial advice. Always consult with qualified financial advisors for important financial decisions.
