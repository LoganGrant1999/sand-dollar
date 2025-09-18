import axios from 'axios'
import { authClient } from './auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE || '/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

// keep if you use interceptors to attach auth headers
try {
  authClient(api)
} catch {}
