import React from 'react';
import { themePrimitives } from './ui/ThemePrimitives';

interface ProgressPillProps {
  label: string;
  current: number;
  target?: number;
  percentage?: number;
  format?: 'currency' | 'number' | 'percentage';
  size?: 'sm' | 'md' | 'lg';
  color?: 'green' | 'blue' | 'orange' | 'red' | 'purple' | 'auto';
  showProgress?: boolean;
  className?: string;
}

export default function ProgressPill({
  label,
  current,
  target,
  percentage,
  format = 'currency',
  size = 'md',
  color = 'auto',
  showProgress = true,
  className = ''
}: ProgressPillProps) {
  const formatValue = (value: number) => {
    switch (format) {
      case 'currency':
        return new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: 'USD',
          minimumFractionDigits: 0,
          maximumFractionDigits: 0,
        }).format(value);
      case 'percentage':
        return `${Math.round(value)}%`;
      case 'number':
      default:
        return value.toLocaleString();
    }
  };

  const calculatePercentage = () => {
    if (percentage !== undefined) return percentage;
    if (target && target > 0) return (current / target) * 100;
    return 0;
  };

  const getProgressColor = () => {
    if (color !== 'auto') {
      const colorMap = {
        green: '#10B981', // bg-green-500 equivalent
        blue: '#3B82F6',  // bg-blue-500 equivalent
        orange: '#F59E0B', // bg-orange-500 equivalent
        red: '#EF4444',   // bg-red-500 equivalent
        purple: '#8B5CF6', // bg-purple-500 equivalent
      };
      return colorMap[color];
    }

    const percent = calculatePercentage();
    if (percent >= 100) return '#10B981'; // green
    if (percent >= 75) return '#3B82F6';  // blue
    if (percent >= 50) return '#F59E0B';  // orange
    if (percent >= 25) return '#EAB308';  // yellow
    return '#EF4444'; // red
  };

  const getSizeClasses = () => {
    switch (size) {
      case 'sm':
        return {
          container: 'px-3 py-2',
          label: 'text-xs',
          value: 'text-sm font-semibold',
          progress: 'h-1',
        };
      case 'lg':
        return {
          container: 'px-6 py-4',
          label: 'text-sm',
          value: 'text-xl font-bold',
          progress: 'h-2',
        };
      case 'md':
      default:
        return {
          container: 'px-4 py-3',
          label: 'text-xs',
          value: 'text-lg font-semibold',
          progress: 'h-1.5',
        };
    }
  };

  const sizeClasses = getSizeClasses();
  const progressColor = getProgressColor();
  const progressPercentage = calculatePercentage();

  return (
    <div
      className={`rounded-full border inline-flex flex-col items-center text-center min-w-0 ${sizeClasses.container} ${className}`}
      style={{
        backgroundColor: `${progressColor}10`,
        borderColor: `${progressColor}30`,
      }}
    >
      <div
        className={`${sizeClasses.label} font-medium mb-1`}
        style={{ color: '#475569' }}
      >
        {label}
      </div>

      <div
        className={sizeClasses.value}
        style={{ color: progressColor }}
      >
        {formatValue(current)}
        {target && format === 'currency' && (
          <span
            className="text-xs font-normal ml-1"
            style={{ color: '#64748B' }}
          >
            / {formatValue(target)}
          </span>
        )}
      </div>

      {showProgress && (target || percentage !== undefined) && (
        <div className="w-full mt-2">
          <div
            className={`w-full ${sizeClasses.progress} rounded-full`}
            style={{ backgroundColor: '#E2E8F0' }}
          >
            <div
              className={`${sizeClasses.progress} rounded-full transition-all duration-300`}
              style={{
                width: `${Math.min(progressPercentage, 100)}%`,
                backgroundColor: progressColor,
              }}
            />
          </div>
          <div
            className="text-xs mt-1 font-medium"
            style={{ color: '#64748B' }}
          >
            {Math.round(progressPercentage)}%
          </div>
        </div>
      )}
    </div>
  );
}