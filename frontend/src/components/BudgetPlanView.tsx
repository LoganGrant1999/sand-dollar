import React from 'react';
import { themePrimitives, Card } from './ui/ThemePrimitives';

interface BudgetCategory {
  category: string;
  currentAvg: number;
  suggestedMax: number;
  trim: number;
  trimPercent: number;
}

interface BudgetPlan {
  monthlyTarget: number;
  categories: BudgetCategory[];
  rationale: string;
}

interface BudgetPlanViewProps {
  budgetPlan: BudgetPlan;
  variant?: 'default' | 'compact';
  showRationale?: boolean;
  maxCategories?: number;
}

export default function BudgetPlanView({
  budgetPlan,
  variant = 'default',
  showRationale = true,
  maxCategories = 10
}: BudgetPlanViewProps) {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatPercentage = (value: number) => {
    return `${Math.round(value)}%`;
  };

  const getCategoryDisplayName = (category: string) => {
    return category.toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  const getTrimColor = (trimPercent: number) => {
    if (trimPercent >= 20) return themePrimitives.colors.red[500];
    if (trimPercent >= 10) return themePrimitives.colors.orange[500];
    if (trimPercent > 0) return themePrimitives.colors.yellow[500];
    return themePrimitives.colors.green[500];
  };

  const categoriesToShow = budgetPlan.categories.slice(0, maxCategories);

  if (variant === 'compact') {
    return (
      <Card className="p-4">
        <div className="mb-3">
          <div className="flex justify-between items-center">
            <span className="text-sm font-medium" style={{ color: themePrimitives.colors.slate[700] }}>
              Monthly Target
            </span>
            <span className="font-semibold" style={{ color: themePrimitives.colors.green[600] }}>
              {formatCurrency(budgetPlan.monthlyTarget)}
            </span>
          </div>
        </div>

        <div className="space-y-2">
          {categoriesToShow.slice(0, 5).map((category, index) => (
            <div key={index} className="flex justify-between items-center text-sm">
              <span style={{ color: themePrimitives.colors.slate[600] }}>
                {getCategoryDisplayName(category.category)}
              </span>
              <div className="text-right">
                <div className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {formatCurrency(category.suggestedMax)}
                </div>
                {category.trim > 0 && (
                  <div className="text-xs" style={{ color: getTrimColor(category.trimPercent) }}>
                    -{formatCurrency(category.trim)}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {budgetPlan.categories.length > 5 && (
          <div className="mt-2 text-center text-xs" style={{ color: themePrimitives.colors.slate[500] }}>
            +{budgetPlan.categories.length - 5} more categories
          </div>
        )}
      </Card>
    );
  }

  return (
    <Card className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
          Budget Plan
        </h3>
        <div className="text-right">
          <div className="text-sm" style={{ color: themePrimitives.colors.slate[600] }}>
            Monthly Target
          </div>
          <div className="text-xl font-bold" style={{ color: themePrimitives.colors.green[600] }}>
            {formatCurrency(budgetPlan.monthlyTarget)}
          </div>
        </div>
      </div>

      <div className="space-y-4">
        {categoriesToShow.map((category, index) => (
          <div key={index} className="border-b pb-3" style={{ borderColor: themePrimitives.colors.slate[200] }}>
            <div className="flex justify-between items-start mb-2">
              <div className="flex-1">
                <div className="font-medium" style={{ color: themePrimitives.colors.slate[900] }}>
                  {getCategoryDisplayName(category.category)}
                </div>
                <div className="text-xs mt-1" style={{ color: themePrimitives.colors.slate[500] }}>
                  Current avg: {formatCurrency(category.currentAvg)}
                </div>
              </div>
              <div className="text-right">
                <div className="font-semibold" style={{ color: themePrimitives.colors.slate[900] }}>
                  {formatCurrency(category.suggestedMax)}
                </div>
                {category.trim > 0 && (
                  <div className="text-sm" style={{ color: getTrimColor(category.trimPercent) }}>
                    -{formatCurrency(category.trim)} ({formatPercentage(category.trimPercent)})
                  </div>
                )}
              </div>
            </div>

            {/* Visual bar showing current vs suggested */}
            <div className="flex items-center space-x-2">
              <div className="flex-1 h-2 rounded-full relative" style={{ backgroundColor: themePrimitives.colors.slate[200] }}>
                {/* Current spending bar */}
                <div
                  className="h-2 rounded-full absolute"
                  style={{
                    width: '100%',
                    backgroundColor: themePrimitives.colors.slate[400]
                  }}
                />
                {/* Suggested spending bar */}
                <div
                  className="h-2 rounded-full absolute"
                  style={{
                    width: `${Math.min((category.suggestedMax / category.currentAvg) * 100, 100)}%`,
                    backgroundColor: category.trim > 0 ? getTrimColor(category.trimPercent) : themePrimitives.colors.green[500]
                  }}
                />
              </div>
              {category.trim > 0 && (
                <span className="text-xs font-medium" style={{ color: getTrimColor(category.trimPercent) }}>
                  Trim {formatPercentage(category.trimPercent)}
                </span>
              )}
            </div>
          </div>
        ))}
      </div>

      {showRationale && budgetPlan.rationale && (
        <div
          className="mt-6 p-4 rounded-lg"
          style={{
            backgroundColor: themePrimitives.colors.blue[50],
            borderLeft: `4px solid ${themePrimitives.colors.blue[500]}`
          }}
        >
          <div className="text-sm font-medium mb-2" style={{ color: themePrimitives.colors.blue[800] }}>
            💡 Budget Insights
          </div>
          <div className="text-sm leading-relaxed" style={{ color: themePrimitives.colors.blue[700] }}>
            {budgetPlan.rationale}
          </div>
        </div>
      )}

      {budgetPlan.categories.length > maxCategories && (
        <div className="mt-4 text-center">
          <div className="text-sm" style={{ color: themePrimitives.colors.slate[500] }}>
            Showing {maxCategories} of {budgetPlan.categories.length} categories
          </div>
        </div>
      )}
    </Card>
  );
}