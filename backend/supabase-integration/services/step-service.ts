import { supabase } from '../supabase-client'
import type { Database } from '../supabase-client'

type StepData = Database['public']['Tables']['step_data']['Row']
type StepDataInsert = Database['public']['Tables']['step_data']['Insert']
type StepDataUpdate = Database['public']['Tables']['step_data']['Update']

export interface StepStatistics {
  period: string
  startDate: string
  endDate: string
  totalSteps: number
  totalDistance: number
  totalCalories: number
  totalActiveMinutes: number
  averageSteps: number
  currentStreak: number
  longestStreak: number
  daysWithData: number
}

export interface StepTrends {
  dailyData: Array<{
    date: string
    steps: number
    distanceMeters: number | null
    calories: number | null
  }>
  movingAverages: Array<{
    date: string
    averageSteps: number
  }>
  period: string
}

export class StepService {
  // Get daily step data
  static async getDailySteps(date: string): Promise<StepData> {
    const { data, error } = await supabase
      .from('step_data')
      .select('*')
      .eq('date', date)
      .single()
    
    if (error && error.code !== 'PGRST116') throw error
    
    // Return default data if no record exists
    return data || {
      id: '',
      user_id: '',
      date,
      steps: 0,
      distance_meters: 0,
      calories: 0,
      active_minutes: 0,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    }
  }

  // Get step data for date range
  static async getStepRange(startDate: string, endDate: string): Promise<StepData[]> {
    const { data, error } = await supabase
      .from('step_data')
      .select('*')
      .gte('date', startDate)
      .lte('date', endDate)
      .order('date')
    
    if (error) throw error
    return data || []
  }

  // Sync step data (upsert multiple records)
  static async syncSteps(stepsData: StepDataInsert[]): Promise<StepData[]> {
    const { data, error } = await supabase
      .from('step_data')
      .upsert(stepsData, { 
        onConflict: 'user_id,date',
        ignoreDuplicates: false
      })
      .select()
    
    if (error) throw error
    return data || []
  }

  // Update single step record
  static async updateStepData(stepData: StepDataUpdate): Promise<StepData> {
    const { data, error } = await supabase
      .from('step_data')
      .update({
        ...stepData,
        updated_at: new Date().toISOString()
      })
      .eq('user_id', stepData.user_id!)
      .eq('date', stepData.date!)
      .select()
      .single()
    
    if (error) throw error
    return data
  }

  // Insert single step record
  static async insertStepData(stepData: StepDataInsert): Promise<StepData> {
    const { data, error } = await supabase
      .from('step_data')
      .insert(stepData)
      .select()
      .single()
    
    if (error) throw error
    return data
  }

  // Get step statistics using database function
  static async getStatistics(period: string, startDate?: string): Promise<StepStatistics> {
    let queryStartDate: string
    let queryEndDate: string
    
    if (startDate) {
      queryStartDate = startDate
      const endDate = new Date(startDate)
      endDate.setDate(endDate.getDate() + (period === 'week' ? 7 : period === 'month' ? 30 : 365))
      queryEndDate = endDate.toISOString().split('T')[0]
    } else {
      queryEndDate = new Date().toISOString().split('T')[0]
      const startDate = new Date()
      startDate.setDate(startDate.getDate() - (period === 'week' ? 7 : period === 'month' ? 30 : 365))
      queryStartDate = startDate.toISOString().split('T')[0]
    }

    // Get current user
    const { data: { user } } = await supabase.auth.getUser()
    if (!user) throw new Error('User not authenticated')

    // Call database function for statistics
    const { data, error } = await supabase
      .rpc('get_step_statistics', {
        p_user_id: user.id,
        p_start_date: queryStartDate,
        p_end_date: queryEndDate
      })
    
    if (error) throw error
    
    const stats = data[0] || {
      total_steps: 0,
      total_distance: 0,
      total_calories: 0,
      average_steps: 0,
      current_streak: 0,
      longest_streak: 0
    }

    // Get total days with data
    const { count } = await supabase
      .from('step_data')
      .select('*', { count: 'exact', head: true })
      .gte('date', queryStartDate)
      .lte('date', queryEndDate)

    return {
      period,
      startDate: queryStartDate,
      endDate: queryEndDate,
      totalSteps: stats.total_steps || 0,
      totalDistance: stats.total_distance || 0,
      totalCalories: stats.total_calories || 0,
      totalActiveMinutes: 0, // Calculate from step data if needed
      averageSteps: Math.round(stats.average_steps || 0),
      currentStreak: stats.current_streak || 0,
      longestStreak: stats.longest_streak || 0,
      daysWithData: count || 0
    }
  }

  // Get step trends with moving averages
  static async getTrends(days: number = 30): Promise<StepTrends> {
    const endDate = new Date().toISOString().split('T')[0]
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - days + 1)
    const startDateStr = startDate.toISOString().split('T')[0]

    // Get step data for the period
    const { data, error } = await supabase
      .from('step_data')
      .select('*')
      .gte('date', startDateStr)
      .lte('date', endDate)
      .order('date')
    
    if (error) throw error

    // Fill in missing dates with zero steps
    const filledData = this.fillMissingDates(data || [], startDateStr, endDate)
    
    // Calculate moving averages
    const movingAverages = this.calculateMovingAverages(filledData, 7)

    return {
      dailyData: filledData.map(item => ({
        date: item.date,
        steps: item.steps,
        distanceMeters: item.distance_meters,
        calories: item.calories
      })),
      movingAverages,
      period: `${days} days`
    }
  }

  // Get current streak
  static async getCurrentStreak(): Promise<number> {
    const { data, error } = await supabase
      .from('step_data')
      .select('date, steps')
      .order('date', { ascending: false })
    
    if (error) throw error

    let streak = 0
    const today = new Date()
    
    for (const record of data || []) {
      const recordDate = new Date(record.date)
      const daysDiff = Math.floor((today.getTime() - recordDate.getTime()) / (1000 * 60 * 60 * 24))
      
      if (daysDiff === streak && record.steps > 0) {
        streak++
      } else {
        break
      }
    }
    
    return streak
  }

  // Get best day (highest step count)
  static async getBestDay(): Promise<{ date: string; steps: number } | null> {
    const { data, error } = await supabase
      .from('step_data')
      .select('date, steps')
      .order('steps', { ascending: false })
      .limit(1)
      .single()
    
    if (error) throw error
    return data ? { date: data.date, steps: data.steps } : null
  }

  // Get weekly summary
  static async getWeeklySummary(): Promise<{
    totalSteps: number
    averageSteps: number
    daysWithData: number
    goalProgress: number
  }> {
    const endDate = new Date().toISOString().split('T')[0]
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - 6)
    const startDateStr = startDate.toISOString().split('T')[0]

    const { data, error } = await supabase
      .from('step_data')
      .select('steps')
      .gte('date', startDateStr)
      .lte('date', endDate)
    
    if (error) throw error

    const steps = data?.map(record => record.steps) || []
    const totalSteps = steps.reduce((sum, step) => sum + step, 0)
    const daysWithData = steps.filter(step => step > 0).length
    const averageSteps = daysWithData > 0 ? Math.round(totalSteps / daysWithData) : 0

    // Assume weekly goal of 70,000 steps (10,000 per day)
    const goalProgress = Math.min((totalSteps / 70000) * 100, 100)

    return {
      totalSteps,
      averageSteps,
      daysWithData,
      goalProgress
    }
  }

  // Get monthly summary
  static async getMonthlySummary(): Promise<{
    totalSteps: number
    averageSteps: number
    daysWithData: number
    goalProgress: number
  }> {
    const endDate = new Date().toISOString().split('T')[0]
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - 29)
    const startDateStr = startDate.toISOString().split('T')[0]

    const { data, error } = await supabase
      .from('step_data')
      .select('steps')
      .gte('date', startDateStr)
      .lte('date', endDate)
    
    if (error) throw error

    const steps = data?.map(record => record.steps) || []
    const totalSteps = steps.reduce((sum, step) => sum + step, 0)
    const daysWithData = steps.filter(step => step > 0).length
    const averageSteps = daysWithData > 0 ? Math.round(totalSteps / daysWithData) : 0

    // Assume monthly goal of 300,000 steps (10,000 per day)
    const goalProgress = Math.min((totalSteps / 300000) * 100, 100)

    return {
      totalSteps,
      averageSteps,
      daysWithData,
      goalProgress
    }
  }

  // Subscribe to real-time step updates
  static subscribeToStepUpdates(callback: (payload: any) => void) {
    return supabase
      .channel('step_updates')
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'step_data'
        },
        callback
      )
      .subscribe()
  }

  // Helper method to fill missing dates
  private static fillMissingDates(data: StepData[], startDate: string, endDate: string): StepData[] {
    const filledData: StepData[] = []
    const currentDate = new Date(startDate)
    const end = new Date(endDate)
    
    while (currentDate <= end) {
      const dateStr = currentDate.toISOString().split('T')[0]
      const existingData = data.find(item => item.date === dateStr)
      
      if (existingData) {
        filledData.push(existingData)
      } else {
        filledData.push({
          id: '',
          user_id: '',
          date: dateStr,
          steps: 0,
          distance_meters: 0,
          calories: 0,
          active_minutes: 0,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        })
      }
      
      currentDate.setDate(currentDate.getDate() + 1)
    }
    
    return filledData
  }

  // Helper method to calculate moving averages
  private static calculateMovingAverages(data: StepData[], windowSize: number): Array<{ date: string; averageSteps: number }> {
    const movingAverages: Array<{ date: string; averageSteps: number }> = []
    
    for (let i = windowSize - 1; i < data.length; i++) {
      const window = data.slice(i - windowSize + 1, i + 1)
      const average = Math.round(window.reduce((sum, item) => sum + item.steps, 0) / windowSize)
      
      movingAverages.push({
        date: data[i].date,
        averageSteps: average
      })
    }
    
    return movingAverages
  }

  // Calculate calories burned (rough estimation)
  static calculateCalories(steps: number, weightKg: number = 70): number {
    // Rough estimation: 1 step = 0.04 calories for average person
    return Math.round(steps * 0.04 * (weightKg / 70))
  }

  // Calculate distance in meters (rough estimation)
  static calculateDistance(steps: number, stepLengthMeters: number = 0.762): number {
    // Average step length is about 0.762 meters (30 inches)
    return Math.round(steps * stepLengthMeters * 100) / 100
  }

  // Get step data for habit tracker (last 365 days)
  static async getHabitTrackerData(): Promise<Array<{ date: string; steps: number; level: number }>> {
    const endDate = new Date().toISOString().split('T')[0]
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - 364)
    const startDateStr = startDate.toISOString().split('T')[0]

    const { data, error } = await supabase
      .from('step_data')
      .select('date, steps')
      .gte('date', startDateStr)
      .lte('date', endDate)
      .order('date')
    
    if (error) throw error

    const filledData = this.fillMissingDates(data || [], startDateStr, endDate)
    
    return filledData.map(item => ({
      date: item.date,
      steps: item.steps,
      level: this.getActivityLevel(item.steps)
    }))
  }

  // Get activity level based on steps (for habit tracker)
  private static getActivityLevel(steps: number): number {
    if (steps === 0) return 0
    if (steps < 5000) return 1
    if (steps < 7500) return 2
    if (steps < 10000) return 3
    return 4
  }
} 