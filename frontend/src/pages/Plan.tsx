import { useState } from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select } from '@/components/ui/select'
import {
  MapPin,
  Target,
  Calendar,
  DollarSign,
  Plane,
  Home,
  GraduationCap,
  Car,
  Heart,
  PiggyBank
} from 'lucide-react'

export default function Plan() {
  const [step, setStep] = useState(1)
  const [goalData, setGoalData] = useState({
    name: '',
    category: '',
    targetAmount: '',
    targetDate: '',
    description: '',
    currentAmount: '0'
  })

  const goalCategories = [
    { value: 'vacation', label: 'Vacation & Travel', icon: Plane },
    { value: 'emergency', label: 'Emergency Fund', icon: PiggyBank },
    { value: 'home', label: 'Home & Property', icon: Home },
    { value: 'education', label: 'Education', icon: GraduationCap },
    { value: 'vehicle', label: 'Vehicle', icon: Car },
    { value: 'wedding', label: 'Wedding & Events', icon: Heart },
    { value: 'other', label: 'Other', icon: Target }
  ]

  const handleNext = () => {
    if (step < 3) setStep(step + 1)
  }

  const handlePrevious = () => {
    if (step > 1) setStep(step - 1)
  }

  const handleSubmit = () => {
    // TODO: Submit goal to backend
    console.log('Creating goal:', goalData)
    // Redirect to goals page
    window.location.href = '/goals'
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-foreground">Plan a Goal</h1>
        <p className="text-muted-foreground mt-2">
          Set up your financial goal and create a plan to achieve it
        </p>
      </div>

      {/* Progress indicator */}
      <div className="flex items-center justify-center space-x-4">
        {[1, 2, 3].map((stepNumber) => (
          <div key={stepNumber} className="flex items-center">
            <div
              className={`flex items-center justify-center w-8 h-8 rounded-full border-2 ${
                stepNumber <= step
                  ? 'bg-primary border-primary text-primary-foreground'
                  : 'border-muted-foreground text-muted-foreground'
              }`}
            >
              {stepNumber}
            </div>
            {stepNumber < 3 && (
              <div
                className={`w-12 h-0.5 ${
                  stepNumber < step ? 'bg-primary' : 'bg-muted'
                }`}
              />
            )}
          </div>
        ))}
      </div>

      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MapPin className="h-5 w-5 text-primary" />
            {step === 1 && 'Choose Your Goal'}
            {step === 2 && 'Set Your Target'}
            {step === 3 && 'Plan Details'}
          </CardTitle>
          <CardDescription>
            {step === 1 && 'What would you like to save for?'}
            {step === 2 && 'How much do you need and when?'}
            {step === 3 && 'Add details to personalize your goal'}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {step === 1 && (
            <div className="space-y-4">
              <div>
                <Label htmlFor="goal-name">Goal Name</Label>
                <Input
                  id="goal-name"
                  placeholder="e.g., Japan vacation, Emergency fund, Down payment"
                  value={goalData.name}
                  onChange={(e) => setGoalData({ ...goalData, name: e.target.value })}
                />
              </div>

              <div>
                <Label htmlFor="goal-category">Category</Label>
                <Select
                  id="goal-category"
                  value={goalData.category}
                  onChange={(e) => setGoalData({ ...goalData, category: e.target.value })}
                >
                  <option value="">Select a category</option>
                  {goalCategories.map((category) => (
                    <option key={category.value} value={category.value}>
                      {category.label}
                    </option>
                  ))}
                </Select>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-4">
              <div>
                <Label htmlFor="target-amount">Target Amount</Label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="target-amount"
                    type="number"
                    placeholder="5000"
                    className="pl-9"
                    value={goalData.targetAmount}
                    onChange={(e) => setGoalData({ ...goalData, targetAmount: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="target-date">Target Date</Label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="target-date"
                    type="date"
                    className="pl-9"
                    value={goalData.targetDate}
                    onChange={(e) => setGoalData({ ...goalData, targetDate: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="current-amount">Current Amount (optional)</Label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="current-amount"
                    type="number"
                    placeholder="0"
                    className="pl-9"
                    value={goalData.currentAmount}
                    onChange={(e) => setGoalData({ ...goalData, currentAmount: e.target.value })}
                  />
                </div>
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-4">
              <div>
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea
                  id="description"
                  placeholder="Add any details about your goal..."
                  value={goalData.description}
                  onChange={(e) => setGoalData({ ...goalData, description: e.target.value })}
                />
              </div>

              <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                <h4 className="font-semibold text-foreground">Goal Summary</h4>
                <div className="space-y-1 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Goal:</span>
                    <span className="font-medium">{goalData.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Target:</span>
                    <span className="font-medium">${goalData.targetAmount}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">By:</span>
                    <span className="font-medium">{goalData.targetDate}</span>
                  </div>
                  {goalData.currentAmount !== '0' && (
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Starting with:</span>
                      <span className="font-medium">${goalData.currentAmount}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          <div className="flex justify-between pt-4">
            <Button
              variant="outline"
              onClick={handlePrevious}
              disabled={step === 1}
            >
              Previous
            </Button>

            {step < 3 ? (
              <Button
                onClick={handleNext}
                disabled={
                  (step === 1 && (!goalData.name || !goalData.category)) ||
                  (step === 2 && (!goalData.targetAmount || !goalData.targetDate))
                }
              >
                Next
              </Button>
            ) : (
              <Button onClick={handleSubmit}>
                Create Goal
              </Button>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}