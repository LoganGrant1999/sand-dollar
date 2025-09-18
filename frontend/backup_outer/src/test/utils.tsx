import { ReactElement } from 'react'
import { render, RenderOptions } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, MemoryRouterProps } from 'react-router-dom'

export function renderWithProviders(
  ui: ReactElement,
  {
    route = '/',
    routerProps,
    queryClient,
    ...options
  }: RenderOptions & {
    route?: string
    routerProps?: Omit<MemoryRouterProps, 'children'>
    queryClient?: QueryClient
  } = {}
) {
  const client = queryClient ?? new QueryClient()

  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[route]} {...routerProps}>
          {children}
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return {
    client,
    ...render(ui, { wrapper: Wrapper, ...options }),
  }
}
