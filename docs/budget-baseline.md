# Budget Baseline System

## Overview

The Budget Baseline System is a comprehensive solution for calculating personalized monthly budget recommendations based on historical transaction data. It analyzes spending patterns, income cadence, and financial behavior to provide intelligent baseline budgets that serve as starting points for budget planning.

## Features

- **Smart Category Normalization**: Resolves "Misc" categorization issues by falling back to subcategories
- **Transfer & Refund Detection**: Automatically filters out transfers and refunds from budget calculations
- **Denver Timezone Support**: All date calculations respect America/Denver timezone
- **Income Cadence Detection**: Identifies paycheck frequency (weekly, bi-weekly, semi-monthly, monthly, irregular)
- **Statistical Analysis**: Uses winsorized means, exponential moving averages, and confidence scoring
- **Robust Filtering**: Excludes pending transactions, transfers, and refunds for accurate analysis

## Architecture

### Backend Components

#### Core Services

- **`BudgetBaselineService`**: Main service orchestrating the baseline calculation
- **`CategoryNormalizer`**: Handles category normalization with fallback logic
- **`TransferHeuristics`**: Detects transfers using multiple heuristic patterns
- **`RefundHeuristics`**: Identifies refunds and returns
- **`DateWindows`**: Manages Denver timezone date calculations
- **`Stats`**: Provides statistical functions for data analysis

#### API Endpoint

```
GET /api/budget/baseline
```

**Response Format:**
```json
{
  "monthlyIncome": 5000.00,
  "totalMonthlyExpenses": 3500.00,
  "monthlyExpensesByCategory": {
    "Food And Drink": 800.00,
    "Transportation": 450.00,
    "Shopping": 300.00,
    "Entertainment": 200.00,
    "Utilities": 150.00
  },
  "paycheckCadence": "BIWEEKLY",
  "categoryConfidenceScores": {
    "Food And Drink": 0.85,
    "Transportation": 0.92,
    "Shopping": 0.67,
    "Entertainment": 0.45,
    "Utilities": 0.98
  }
}
```

### Frontend Components

#### BudgetBaselineCard

A React component that displays the budget baseline information with:

- Monthly income and expenses summary
- Net income and savings rate calculation
- Paycheck cadence indicator
- Top spending categories with confidence scores
- Expandable detail view

## Data Processing Pipeline

### 1. Data Collection

The system analyzes the last 3 months of transaction data to ensure sufficient sample size for meaningful patterns.

### 2. Filtering

Transactions are filtered to exclude:
- Pending transactions
- Transfers (detected via keywords, categories, account patterns, confirmation patterns)
- Refunds (detected via keywords and positive amounts)

### 3. Category Normalization

Categories are normalized using a fallback hierarchy:
1. Use `categoryTop` if present and not "Misc"
2. Fall back to `categorySub` if available
3. Default to "Misc" as last resort
4. Apply proper capitalization and whitespace normalization

### 4. Income Analysis

Income transactions are:
- Identified by positive amounts and income-related keywords/categories
- Grouped by month to calculate monthly averages
- Analyzed using winsorized mean (10th-90th percentile) to handle outliers

### 5. Expense Analysis

Expenses are:
- Grouped by normalized category
- Calculated using monthly averages
- Smoothed using winsorized mean to handle irregular spending

### 6. Paycheck Cadence Detection

The system analyzes income transaction patterns to detect:
- **Weekly**: ~7 days between paychecks (±20% tolerance)
- **Bi-weekly**: ~14 days between paychecks (±20% tolerance)
- **Semi-monthly**: ~15 days between paychecks (±20% tolerance)
- **Monthly**: ~30 days between paychecks (±20% tolerance)
- **Irregular**: Patterns that don't match standard cadences

### 7. Confidence Scoring

Each category receives a confidence score (0-1) based on:
- Consistency of spending amounts
- Frequency of transactions
- Standard deviation relative to mean (coefficient of variation)

## Technical Implementation

### Category Normalization Logic

```java
public String normalizeCategory(String categoryTop, String categorySub) {
    // Use categoryTop if valid and not "Misc"
    if (categoryTop != null && !categoryTop.trim().isEmpty() &&
        !categoryTop.trim().equalsIgnoreCase("Misc")) {
        return normalizeAndCapitalize(categoryTop);
    }

    // Fall back to categorySub
    if (categorySub != null && !categorySub.trim().isEmpty()) {
        return normalizeAndCapitalize(categorySub);
    }

    return "Misc";
}
```

### Transfer Detection Patterns

- **Keywords**: transfer, xfer, deposit, withdrawal, etc.
- **Categories**: transfer, bank fees, deposit
- **Account patterns**: `account #1234`, `acct *5678`
- **Confirmation patterns**: `ref#ABC123`, `confirmation 987654`

### Statistical Functions

- **Winsorized Mean**: Clips outliers at specified percentiles before averaging
- **Exponential Moving Average**: Weights recent data more heavily
- **Confidence Calculation**: `1.0 - min(1.0, coefficient_of_variation)`

## Usage Examples

### Basic Integration

```typescript
// Frontend usage
import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'

const baselineQuery = useQuery({
  queryKey: ['budget', 'baseline'],
  queryFn: async () => {
    const response = await api.get('/budget/baseline')
    return response.data
  }
})
```

### Backend Service Usage

```java
@Autowired
private BudgetBaselineService budgetBaselineService;

public void generateBaseline(User user) {
    BudgetBaseline baseline = budgetBaselineService.calculateBaseline(user);

    // Access calculated values
    long monthlyIncome = baseline.getMonthlyIncomeCents();
    Map<String, Long> expenses = baseline.getMonthlyExpensesByCategory();
    PaycheckCadence cadence = baseline.getPaycheckCadence();
    Map<String, Double> confidence = baseline.getCategoryConfidenceScores();
}
```

## Configuration

### Time Zone Settings

The system uses `America/Denver` timezone for all date calculations. This can be configured in the `DateWindows` component:

```java
private static final ZoneId DENVER_TIMEZONE = ZoneId.of("America/Denver");
```

### Statistical Parameters

- **Winsorization Percentiles**: 10th and 90th percentiles (configurable)
- **Paycheck Cadence Tolerance**: ±20% (configurable)
- **Analysis Window**: Last 3 months (configurable)

## Error Handling

### Common Scenarios

1. **Insufficient Data**: Returns error if user has fewer than 3 transactions
2. **No Income**: Sets monthly income to $0 and continues analysis
3. **Authentication Issues**: Returns 401 if user not authenticated
4. **Database Errors**: Returns 500 with appropriate error message

### Frontend Error Handling

The `BudgetBaselineCard` component gracefully handles:
- Loading states with spinner
- Error states with retry option
- Empty data states with helpful messaging

## Testing

### Unit Tests

Comprehensive test coverage includes:

- **CategoryNormalizerTest**: Category normalization logic
- **TransferHeuristicsTest**: Transfer detection patterns
- **RefundHeuristicsTest**: Refund identification
- **StatsTest**: Statistical function accuracy
- **DateWindowsTest**: Timezone and date calculations
- **BudgetBaselineServiceTest**: End-to-end service integration

### Test Data Requirements

Tests use mock data representing:
- Various transaction types and categories
- Different income patterns and cadences
- Edge cases (outliers, missing data, etc.)
- Multiple time zones and date ranges

## Performance Considerations

### Database Optimization

- Queries are optimized with proper indexes on `user_id`, `date`, and `pending` columns
- Date range filtering reduces dataset size
- Category grouping is performed in application layer

### Caching Strategy

Consider implementing caching for:
- Baseline calculations (24-hour TTL)
- Category normalization results
- Statistical computations

### Scalability

The system is designed to handle:
- Users with thousands of transactions
- Concurrent baseline calculations
- Real-time updates as new transactions arrive

## Future Enhancements

### Planned Features

1. **Machine Learning Integration**: Use ML models for more accurate categorization
2. **Seasonal Adjustments**: Account for seasonal spending variations
3. **Goal Integration**: Incorporate user financial goals into baseline calculations
4. **Multi-Currency Support**: Handle transactions in different currencies
5. **Custom Time Windows**: Allow users to specify analysis periods

### API Extensions

1. **Historical Baselines**: `/api/budget/baseline/history` for trend analysis
2. **Category Insights**: `/api/budget/baseline/categories/{category}` for deep dives
3. **Comparison Endpoints**: Compare baselines across time periods

## Troubleshooting

### Common Issues

1. **"Misc" Categories**: Ensure transaction data includes proper subcategories
2. **Missing Income**: Verify income transactions have positive amounts and proper categorization
3. **Irregular Cadence**: Normal for users with variable income sources
4. **Low Confidence Scores**: Indicates inconsistent spending patterns

### Debugging Tools

1. **Logging**: Enable DEBUG level for `com.sanddollar.budgeting` package
2. **Database Queries**: Review raw transaction data for anomalies
3. **Statistical Analysis**: Examine intermediate calculations for outliers

## Security Considerations

- All endpoints require authenticated users
- No sensitive financial data is logged
- Database queries use parameterized statements
- Frontend components handle authentication errors gracefully

## Dependencies

### Backend

- Spring Boot 3.x
- JPA/Hibernate for database access
- PostgreSQL for data storage
- JUnit 5 for testing

### Frontend

- React 18+
- TypeScript for type safety
- TanStack Query for data fetching
- Tailwind CSS for styling

## Support

For questions or issues related to the Budget Baseline System:

1. Check the troubleshooting section above
2. Review unit tests for usage examples
3. Examine the source code in `com.sanddollar.budgeting` package
4. Contact the development team for advanced support