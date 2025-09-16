import { useState, useRef, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { aiChat, aiChatStream, adjustBudget, type ChatMessage as APIChatMessage, type BudgetAdjustmentRequest, type BudgetAdjustmentResponse } from '@/lib/api'
import { BudgetAdjustmentCard } from '@/components/BudgetAdjustmentCard'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { 
  Bot,
  Send,
  User,
  Loader2,
  TrendingUp,
  DollarSign,
  Calculator,
  Target,
  Trash2
} from 'lucide-react'
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


const SUGGESTED_QUESTIONS = [
  "How much did I spend on food this month?",
  "Show me my account balances",
  "What's my spending trend for the last 30 days?",
  "How am I doing with my current budget?",
  "What are my top spending categories?",
  "Help me create a savings goal",
  "Analyze my recent transactions"
]

export default function Assistant() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [isStreaming, setIsStreaming] = useState(false)
  const [useStreaming, setUseStreaming] = useState(true)
  const [pendingBudgetAdjustment, setPendingBudgetAdjustment] = useState<{instruction: string, adjustmentData: BudgetAdjustmentResponse} | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const queryClient = useQueryClient()

  const convertMessagesToAPI = (messages: Message[]): APIChatMessage[] => {
    return messages.map(msg => ({
      role: msg.role,
      content: msg.content
    }))
  }

  const chatMutation = useMutation({
    mutationFn: async (currentMessages: Message[]) => {
      const apiMessages = convertMessagesToAPI(currentMessages)
      return await aiChat(apiMessages)
    },
    onSuccess: (response: string) => {
      const assistantMessage: Message = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response,
        timestamp: new Date()
      }
      setMessages(prev => [...prev, assistantMessage])
      setIsTyping(false)
    },
    onError: () => {
      const errorMessage: Message = {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: 'Sorry, I encountered an error. Please try again.',
        timestamp: new Date()
      }
      setMessages(prev => [...prev, errorMessage])
      setIsTyping(false)
    }
  })

  const budgetAdjustmentMutation = useMutation({
    mutationFn: async (request: BudgetAdjustmentRequest) => {
      return await adjustBudget(request)
    },
    onSuccess: (response: BudgetAdjustmentResponse, variables: BudgetAdjustmentRequest) => {
      if (response.status === 'success') {
        toast.success('Budget updated successfully!')
        queryClient.invalidateQueries({ queryKey: ['budgets'] })
        setPendingBudgetAdjustment(null)
      } else if (response.status === 'needs_confirmation') {
        setPendingBudgetAdjustment({
          instruction: variables.instruction,
          adjustmentData: response
        })
      }
    },
    onError: (error: any) => {
      toast.error('Failed to adjust budget: ' + error.message)
    }
  })

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages, isTyping, isStreaming])

  const handleSendMessage = async (message: string) => {
    if (!message.trim() || chatMutation.isPending || isStreaming) return

    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: message.trim(),
      timestamp: new Date()
    }

    const updatedMessages = [...messages, userMessage]
    setMessages(updatedMessages)
    setInput('')
    
    // Check if this looks like a budget adjustment instruction
    if (isBudgetAdjustmentInstruction(message.trim())) {
      // Try budget adjustment first
      try {
        await budgetAdjustmentMutation.mutateAsync({
          instruction: message.trim(),
          confirm: false
        })
      } catch (error) {
        // If budget adjustment fails, fall back to regular chat
        console.warn('Budget adjustment failed, falling back to chat:', error)
        proceedWithChat(updatedMessages)
      }
    } else {
      proceedWithChat(updatedMessages)
    }
  }

  const proceedWithChat = async (updatedMessages: Message[]) => {
    if (useStreaming) {
      await handleStreamingMessage(updatedMessages)
    } else {
      setIsTyping(true)
      await chatMutation.mutateAsync(updatedMessages)
    }
  }

  const isBudgetAdjustmentInstruction = (message: string): boolean => {
    const budgetKeywords = ['budget', 'increase', 'decrease', 'reduce', 'allocate', 'move', 'transfer', 'adjust', 'change']
    const amountKeywords = ['$', '%', 'dollar', 'percent', 'by']
    
    const lowerMessage = message.toLowerCase()
    const hasBudgetKeyword = budgetKeywords.some(keyword => lowerMessage.includes(keyword))
    const hasAmountKeyword = amountKeywords.some(keyword => lowerMessage.includes(keyword))
    
    return hasBudgetKeyword && (hasAmountKeyword || lowerMessage.includes('to'))
  }

  const handleStreamingMessage = async (currentMessages: Message[]) => {
    setIsStreaming(true)
    
    const assistantMessageId = `assistant-${Date.now()}`
    
    // Create initial empty assistant message
    const assistantMessage: Message = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date()
    }
    
    setMessages(prev => [...prev, assistantMessage])
    
    try {
      const apiMessages = convertMessagesToAPI(currentMessages)
      await aiChatStream(
        apiMessages, 
        0.7, 
        (token) => {
          setMessages(prev => 
            prev.map(msg => 
              msg.id === assistantMessageId 
                ? { ...msg, content: msg.content + token }
                : msg
            )
          )
        },
        (attempt) => {
          // Show reconnection toast
          toast(`Reconnecting... (attempt ${attempt}/3)`, {
            icon: '🔄',
            duration: 2000
          })
        },
        (_error) => {
          // Show error toast on final failure
          toast.error('Connection failed after 3 attempts. Please try again.')
          setMessages(prev => 
            prev.map(msg => 
              msg.id === assistantMessageId 
                ? { ...msg, content: 'Sorry, the connection failed. Please try sending your message again.' }
                : msg
            )
          )
        }
      )
    } catch (error) {
      // Fallback error handling
      setMessages(prev => 
        prev.map(msg => 
          msg.id === assistantMessageId 
            ? { ...msg, content: 'Sorry, I encountered an error. Please try again.' }
            : msg
        )
      )
    } finally {
      setIsStreaming(false)
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    handleSendMessage(input)
  }

  const handleSuggestedQuestion = (question: string) => {
    handleSendMessage(question)
  }

  const handleClearChat = () => {
    setMessages([])
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
  }

  return (
    <div className="space-y-6 h-full">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">AI Assistant</h1>
          <p className="text-[var(--color-text-secondary)] mt-1">Get insights about your finances and spending habits</p>
        </div>
        {messages.length > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={handleClearChat}
            disabled={chatMutation.isPending || isStreaming}
            className="flex items-center space-x-2"
          >
            <Trash2 className="h-4 w-4" />
            <span>Clear Chat</span>
          </Button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 h-[calc(100vh-200px)]">
        {/* Suggested Questions Sidebar */}
        <div className="lg:col-span-1">
          <Card className="h-full">
            <CardHeader>
              <CardTitle className="text-lg flex items-center space-x-2">
                <Calculator className="h-5 w-5" />
                <span>Quick Questions</span>
              </CardTitle>
              <CardDescription>Click any question to get started</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {SUGGESTED_QUESTIONS.map((question, index) => (
                  <Button
                    key={index}
                    variant="ghost"
                    size="sm"
                    className="w-full text-left justify-start h-auto p-3 whitespace-normal text-sm"
                    onClick={() => handleSuggestedQuestion(question)}
                    disabled={chatMutation.isPending || isStreaming}
                  >
                    <div className="flex items-start space-x-2">
                      {index < 4 ? (
                        <TrendingUp className="h-4 w-4 mt-0.5 flex-shrink-0 text-[var(--color-accent-blue)]" />
                      ) : index < 6 ? (
                        <Target className="mt-0.5 h-4 w-4 flex-shrink-0 text-[var(--color-success)]" />
                      ) : (
                        <DollarSign className="mt-0.5 h-4 w-4 flex-shrink-0 text-[var(--color-accent-blue)]" />
                      )}
                      <span className="text-left">{question}</span>
                    </div>
                  </Button>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Chat Interface */}
        <div className="lg:col-span-3">
          <Card className="h-full flex flex-col">
            <CardHeader className="flex-shrink-0">
              <CardTitle className="flex items-center space-x-2">
                <Bot className="h-5 w-5 text-[var(--color-accent-blue)]" />
                <span>Chat with your Financial Assistant</span>
              </CardTitle>
            </CardHeader>

            {/* Messages */}
            <CardContent className="flex-1 overflow-hidden flex flex-col">
              <div className="flex-1 overflow-y-auto space-y-4 pr-4">
                {messages.length === 0 && (
                  <div className="flex flex-col items-center justify-center h-full text-center space-y-4">
                    <Bot className="h-16 w-16 text-[var(--color-accent-blue)] opacity-50" />
                    <div>
                      <h3 className="text-lg font-semibold text-[var(--color-text-primary)]">Welcome to your AI Financial Assistant!</h3>
                      <p className="mt-2 text-[var(--color-text-secondary)]">
                        Ask me anything about your finances, spending habits, or budgeting goals.
                      </p>
                    </div>
                  </div>
                )}

                {messages.map((message) => (
                  <div
                    key={message.id}
                    className={`flex items-start space-x-3 ${
                      message.role === 'user' ? 'justify-end' : 'justify-start'
                    }`}
                  >
                    {message.role === 'assistant' && (
                      <div className="w-8 h-8 rounded-full bg-[var(--color-accent-blue)]/15 flex items-center justify-center flex-shrink-0">
                        <Bot className="h-4 w-4 text-[var(--color-accent-blue)]" />
                      </div>
                    )}
                    
                    <div
                      className={`max-w-[80%] rounded-lg px-4 py-2 ${
                        message.role === 'user'
                          ? 'bg-[var(--color-accent-teal)] text-[var(--color-bg-dark)] shadow-[0px_2px_10px_rgba(0,0,0,0.4)]'
                          : 'bg-[rgba(255,255,255,0.06)] text-[var(--color-text-primary)]'
                      }`}
                    >
                      <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                      <p className={`mt-1 text-xs ${
                        message.role === 'user' ? 'text-[var(--color-bg-dark)]/70' : 'text-[var(--color-text-secondary)]'
                      }`}>
                        {message.timestamp.toLocaleTimeString()}
                      </p>
                    </div>

                    {message.role === 'user' && (
                      <div className="w-8 h-8 rounded-full bg-[rgba(255,255,255,0.06)] flex items-center justify-center flex-shrink-0">
                        <User className="h-4 w-4 text-[var(--color-text-secondary)]" />
                      </div>
                    )}
                  </div>
                ))}

                {(isTyping || isStreaming) && (
                  <div className="flex items-start space-x-3">
                    <div className="w-8 h-8 rounded-full bg-[var(--color-accent-blue)]/15 flex items-center justify-center">
                      <Bot className="h-4 w-4 text-[var(--color-accent-blue)]" />
                    </div>
                    <div className="bg-[rgba(255,255,255,0.06)] rounded-lg px-4 py-2">
                      <div className="flex items-center space-x-1">
                        <div className="h-2 w-2 animate-bounce rounded-full bg-[var(--color-text-secondary)]" />
                        <div className="h-2 w-2 animate-bounce rounded-full bg-[var(--color-text-secondary)]" style={{ animationDelay: '0.1s' }} />
                        <div className="h-2 w-2 animate-bounce rounded-full bg-[var(--color-text-secondary)]" style={{ animationDelay: '0.2s' }} />
                      </div>
                      <div className="text-xs text-[var(--color-text-secondary)] mt-1">
                        {isStreaming ? (
                          <span className="animate-pulse">Streaming...</span>
                        ) : (
                          'Thinking...'
                        )}
                      </div>
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* Budget Adjustment Confirmation Card */}
              {pendingBudgetAdjustment && (
                <div className="pt-4 border-t">
                  <BudgetAdjustmentCard
                    adjustmentData={pendingBudgetAdjustment.adjustmentData}
                    instruction={pendingBudgetAdjustment.instruction}
                    onConfirm={handleConfirmBudgetAdjustment}
                    onCancel={handleCancelBudgetAdjustment}
                    loading={budgetAdjustmentMutation.isPending}
                  />
                </div>
              )}

              {/* Input Form */}
              <div className="mt-4 pt-4 border-t space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id="streaming"
                      checked={useStreaming}
                      onChange={(e) => setUseStreaming(e.target.checked)}
                      className="rounded"
                    />
                    <label htmlFor="streaming" className="text-sm text-[var(--color-text-secondary)]">
                      Use streaming (real-time responses)
                    </label>
                  </div>
                </div>
                
                <form onSubmit={handleSubmit} className="flex space-x-2">
                  <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="Ask me about your finances..."
                    className="flex-1 rounded-lg border border-[rgba(255,255,255,0.08)] px-4 py-2 focus:outline-none focus:ring-2 focus:ring-[var(--color-accent-teal)] focus:border-transparent"
                    disabled={chatMutation.isPending || isStreaming}
                  />
                  <Button 
                    type="submit"
                    disabled={!input.trim() || chatMutation.isPending || isStreaming}
                    className="px-4 py-2"
                  >
                    {(chatMutation.isPending || isStreaming) ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Send className="h-4 w-4" />
                    )}
                  </Button>
                </form>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
