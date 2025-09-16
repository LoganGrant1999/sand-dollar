import { useCallback } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import {
  getFinancialSnapshot,
  generateAiBudget,
  acceptAiBudget,
} from '@/lib/api'
import type {
  FinancialSnapshotResponse,
  GenerateBudgetRequest,
  GenerateBudgetResponse,
  AcceptBudgetRequest,
  AcceptBudgetResponse,
} from '@/types'

export const AI_BUDGET_QUERY_KEY = ['ai-budget', 'snapshot'] as const

export function useBudgetSnapshot() {
  return useQuery<FinancialSnapshotResponse>({
    queryKey: AI_BUDGET_QUERY_KEY,
    queryFn: getFinancialSnapshot,
    staleTime: 1000 * 60, // cache snapshot for a minute
  })
}

export function useGenerateAiBudget() {
  const queryClient = useQueryClient()

  return useMutation<GenerateBudgetResponse, Error, GenerateBudgetRequest>({
    mutationFn: generateAiBudget,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: AI_BUDGET_QUERY_KEY })
    },
    onError: (error) => {
      toast.error(error.message || 'Unable to generate AI budget right now.')
    },
  })
}

export function useAcceptAiBudget() {
  const queryClient = useQueryClient()

  return useMutation<AcceptBudgetResponse, Error, AcceptBudgetRequest>({
    mutationFn: acceptAiBudget,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: AI_BUDGET_QUERY_KEY })
      toast.success('AI budget accepted! Your targets are live for this month.')
    },
    onError: () => {
      toast.error('Failed to accept AI budget. Please try again.')
    },
  })
}

export function useRefetchBudgetSnapshot() {
  const queryClient = useQueryClient()
  return useCallback(() => {
    queryClient.invalidateQueries({ queryKey: AI_BUDGET_QUERY_KEY })
  }, [queryClient])
}
