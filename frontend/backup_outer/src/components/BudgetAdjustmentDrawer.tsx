import { useState, useRef, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { adjustBudget, type BudgetAdjustmentRequest, type BudgetAdjustmentResponse } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { BudgetAdjustmentCard } from '@/components/BudgetAdjustmentCard'
import { X, Send, Bot, Loader2, Calculator } from 'lucide-react'
import toast from 'react-hot-toast'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  budgetAdjustment?: {
    instruction: string
    adjustmentData: BudgetAdjustmentResponse
  }
}

interface BudgetAdjustmentDrawerProps {
  isOpen: boolean
  onClose: () => void
}

export function BudgetAdjustmentDrawer({ isOpen, onClose }: BudgetAdjustmentDrawerProps) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [pendingBudgetAdjustment, setPendingBudgetAdjustment] = useState<{instruction: string, adjustmentData: BudgetAdjustmentResponse} | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const queryClient = useQueryClient()

  const budgetAdjustmentMutation = useMutation({
    mutationFn: async (request: BudgetAdjustmentRequest) => {
      return await adjustBudget(request)
    },
    onSuccess: (response: BudgetAdjustmentResponse, variables: BudgetAdjustmentRequest) => {
      if (response.status === 'success') {
        toast.success('Budget updated successfully!')
        queryClient.invalidateQueries({ queryKey: ['budgets'] })
        setPendingBudgetAdjustment(null)
        
        // Add success message to chat
        const assistantMessage: Message = {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: '✅ Budget adjustment completed successfully! Your budget has been updated.',
          timestamp: new Date()
        }
        setMessages(prev => [...prev, assistantMessage])
      } else if (response.status === 'needs_confirmation') {
        setPendingBudgetAdjustment({
          instruction: variables.instruction,
          adjustmentData: response
        })
        
        // Add confirmation request to chat
        const assistantMessage: Message = {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: 'I need your confirmation for this budget adjustment. Please review the changes below and confirm if you want to proceed.',
          timestamp: new Date(),
          budgetAdjustment: {
            instruction: variables.instruction,
            adjustmentData: response
          }
        }
        setMessages(prev => [...prev, assistantMessage])
      }
    },
    onError: (error: any) => {
      toast.error('Failed to adjust budget: ' + error.message)
      
      // Add error message to chat
      const assistantMessage: Message = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: `❌ Sorry, I couldn't process that budget adjustment. Error: ${error.message}`,
        timestamp: new Date()
      }
      setMessages(prev => [...prev, assistantMessage])
    }
  })

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // Add welcome message when drawer opens
  useEffect(() => {
    if (isOpen && messages.length === 0) {
      const welcomeMessage: Message = {
        id: `assistant-welcome`,
        role: 'assistant',
        content: `👋 Hi! I'm your budget adjustment assistant. I can help you make changes to your budget quickly and easily.

Try saying things like:
• "Increase my groceries budget by $100"
• "Move $50 from entertainment to savings"
• "Reduce dining out by 20%"
• "Set transportation to $300"

What would you like to adjust?`,
        timestamp: new Date()
      }
      setMessages([welcomeMessage])
    }
  }, [isOpen, messages.length])

  const handleSendMessage = async (message: string) => {
    if (!message.trim() || budgetAdjustmentMutation.isPending) return

    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: message.trim(),
      timestamp: new Date()
    }

    setMessages(prev => [...prev, userMessage])
    setInput('')

    // Process the budget adjustment
    try {
      await budgetAdjustmentMutation.mutateAsync({
        instruction: message.trim(),
        confirm: false
      })
    } catch (error) {
      // Error is handled in onError callback
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    handleSendMessage(input)
  }

  const handleConfirmBudgetAdjustment = async (sourceCategory?: string) => {
    if (!pendingBudgetAdjustment) return

    try {
      await budgetAdjustmentMutation.mutateAsync({
        instruction: pendingBudgetAdjustment.instruction,
        confirm: true,
        sourceCategory
      })
    } catch (error) {
      console.error('Failed to confirm budget adjustment:', error)
    }
  }

  const handleCancelBudgetAdjustment = () => {
    setPendingBudgetAdjustment(null)
    
    // Add cancellation message to chat
    const assistantMessage: Message = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: 'Budget adjustment cancelled. What else would you like to adjust?',
      timestamp: new Date()
    }
    setMessages(prev => [...prev, assistantMessage])
  }

  const handleClearChat = () => {
    setMessages([])
    setPendingBudgetAdjustment(null)
  }

  if (!isOpen) return null

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 bg-black/20 z-40" onClick={onClose} />
      
      {/* Drawer */}
      <div className="fixed top-0 right-0 h-full w-full sm:w-96 bg-background border-l border-border shadow-lg z-50 flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center">
              <Calculator className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h2 className="text-xl font-bold">Budget Assistant</h2>
              <p className="text-sm text-muted-foreground">Adjust your budget with AI</p>
            </div>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="h-5 w-5" />
          </Button>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex items-start space-x-3 ${
                message.role === 'user' ? 'justify-end' : 'justify-start'
              }`}
            >
              {message.role === 'assistant' && (
                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0 mt-1">
                  <Bot className="h-4 w-4 text-primary" />
                </div>
              )}
              
              <div className="flex flex-col space-y-2 max-w-[80%]">
                <div
                  className={`rounded-2xl px-4 py-3 ${
                    message.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted'
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                  <p className={`text-xs mt-2 opacity-70`}>
                    {message.timestamp.toLocaleTimeString()}
                  </p>
                </div>
                
                {/* Budget Adjustment Confirmation Card */}
                {message.budgetAdjustment && (
                  <Card className="mt-2">
                    <CardContent className="p-4">
                      <BudgetAdjustmentCard
                        adjustmentData={message.budgetAdjustment.adjustmentData}
                        instruction={message.budgetAdjustment.instruction}
                        onConfirm={handleConfirmBudgetAdjustment}
                        onCancel={handleCancelBudgetAdjustment}
                        loading={budgetAdjustmentMutation.isPending}
                      />
                    </CardContent>
                  </Card>
                )}
              </div>

              {message.role === 'user' && (
                <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center flex-shrink-0 mt-1">
                  <span className="text-xs font-medium">You</span>
                </div>
              )}
            </div>
          ))}

          {budgetAdjustmentMutation.isPending && (
            <div className="flex items-start space-x-3">
              <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                <Bot className="h-4 w-4 text-primary" />
              </div>
              <div className="bg-muted rounded-2xl px-4 py-3">
                <div className="flex items-center space-x-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span className="text-sm">Processing your budget adjustment...</span>
                </div>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input */}
        <div className="p-6 border-t border-border">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm text-muted-foreground">Quick budget adjustments</span>
            {messages.length > 1 && (
              <Button variant="ghost" size="sm" onClick={handleClearChat}>
                Clear chat
              </Button>
            )}
          </div>
          
          <form onSubmit={handleSubmit} className="flex space-x-2">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="e.g., 'Increase groceries by $50'"
              disabled={budgetAdjustmentMutation.isPending}
              className="flex-1"
            />
            <Button 
              type="submit"
              disabled={!input.trim() || budgetAdjustmentMutation.isPending}
              size="icon"
            >
              <Send className="h-4 w-4" />
            </Button>
          </form>
        </div>
      </div>
    </>
  )
}