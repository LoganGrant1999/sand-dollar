// Feature flag utility for MVP focus
export const getFeatureFlag = (flagName: string): boolean => {
  // Check environment variable first
  const envValue = import.meta.env[`VITE_${flagName}`];
  if (envValue !== undefined) {
    return envValue === 'true';
  }

  // Check localStorage for development/testing
  const localValue = localStorage.getItem(`FEATURE_${flagName}`);
  if (localValue !== null) {
    return localValue === 'true';
  }

  // Default values
  switch (flagName) {
    case 'MVP_FOCUS':
      return true; // Default to MVP focus mode
    default:
      return false;
  }
};

export const setFeatureFlag = (flagName: string, value: boolean): void => {
  localStorage.setItem(`FEATURE_${flagName}`, value.toString());
};

// Specific feature flags
export const isMvpFocus = () => getFeatureFlag('MVP_FOCUS');
export const showAdvancedFeatures = () => !isMvpFocus();

// MVP features
export const MVP_FEATURES = {
  GOALS: true,
  PLAN: true,
  BUDGET_SNAPSHOT: true,
} as const;

// Non-MVP features (hidden when MVP_FOCUS=true)
export const ADVANCED_FEATURES = {
  NET_WORTH: false,
  DEEP_ANALYTICS: false,
  AI_BUDGET_WIZARD: false,
  SPENDING_ANALYSIS: false,
  DETAILED_REPORTS: false,
} as const;