# Project Structure & Organization

## Root Structure
```
sand-dollar/
├── backend/           # Spring Boot application
├── frontend/          # React application
├── docs/             # Documentation and API collections
├── Images/           # Static assets and images
├── docker-compose.yml # Local development environment
└── .env.example      # Environment template
```

## Backend Structure (`backend/`)
```
src/main/java/com/sanddollar/
├── SandDollarApplication.java    # Main Spring Boot application
├── budgeting/                    # Budget calculation logic
├── config/                       # Spring configuration classes
├── controller/                   # REST API controllers
├── dto/                          # Data Transfer Objects
├── entity/                       # JPA entities
├── repository/                   # Data access layer
├── security/                     # Security components (JWT, filters)
├── service/                      # Business logic layer
└── web/                          # Web-specific components

src/main/resources/
├── application.yml               # Main configuration
├── application-{profile}.yml     # Profile-specific configs
├── db/migration/                 # Flyway database migrations
└── static/                       # Static web assets (built frontend)

src/test/java/                    # Test classes mirroring main structure
```

## Frontend Structure (`frontend/src/`)
```
src/
├── components/                   # Reusable React components
│   ├── ui/                      # Base UI components (shadcn/ui)
│   └── __tests__/               # Component tests
├── contexts/                     # React contexts (AuthProvider)
├── hooks/                        # Custom React hooks
├── lib/                          # Utility libraries (api, auth, utils)
├── pages/                        # Route components
│   ├── budget/                  # Budget-specific pages
│   ├── goals/                   # Goal-specific pages
│   ├── plan/                    # Planning pages
│   └── __tests__/               # Page tests
├── styles/                       # CSS and theme files
├── types/                        # TypeScript type definitions
├── utils/                        # Utility functions
├── App.tsx                       # Main application component
└── main.tsx                      # Application entry point
```

## Architecture Patterns

### Backend Patterns
- **Layered Architecture**: Controller → Service → Repository → Entity
- **DTO Pattern**: Separate DTOs for API requests/responses
- **Repository Pattern**: Spring Data JPA repositories
- **Service Layer**: Business logic encapsulation
- **Configuration Classes**: Centralized Spring configuration

### Frontend Patterns
- **Component-Based**: Reusable React components
- **Custom Hooks**: Shared stateful logic
- **Context API**: Global state management (auth)
- **React Query**: Server state management
- **Route-Based Code Splitting**: Pages as route components

## Naming Conventions

### Backend (Java)
- **Classes**: PascalCase (`UserService`, `AuthController`)
- **Methods**: camelCase (`getUserById`, `createBudget`)
- **Constants**: UPPER_SNAKE_CASE (`JWT_SECRET`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase (`com.sanddollar.service`)

### Frontend (TypeScript/React)
- **Components**: PascalCase (`BudgetCard`, `AuthProvider`)
- **Files**: PascalCase for components, camelCase for utilities
- **Variables**: camelCase (`userName`, `isLoading`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`)

## Key Conventions
- **API Endpoints**: RESTful with `/api` prefix
- **Database**: Snake_case table/column names
- **Environment Variables**: UPPER_SNAKE_CASE
- **Git Branches**: kebab-case (`feature/budget-wizard`)
- **CSS Classes**: Tailwind utility classes with custom CSS variables