import { createClient } from '@supabase/supabase-js'

// Environment variables
const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!

// Create Supabase client
export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true
  },
  realtime: {
    params: {
      eventsPerSecond: 10
    }
  }
})

// Database types
export interface Database {
  public: {
    Tables: {
      profiles: {
        Row: {
          id: string
          email: string
          name: string
          date_of_birth: string | null
          height_cm: number | null
          weight_kg: number | null
          units_system: 'metric' | 'imperial'
          timezone: string
          avatar_url: string | null
          created_at: string
          updated_at: string
          last_login: string | null
        }
        Insert: {
          id: string
          email: string
          name: string
          date_of_birth?: string | null
          height_cm?: number | null
          weight_kg?: number | null
          units_system?: 'metric' | 'imperial'
          timezone?: string
          avatar_url?: string | null
          created_at?: string
          updated_at?: string
          last_login?: string | null
        }
        Update: {
          id?: string
          email?: string
          name?: string
          date_of_birth?: string | null
          height_cm?: number | null
          weight_kg?: number | null
          units_system?: 'metric' | 'imperial'
          timezone?: string
          avatar_url?: string | null
          created_at?: string
          updated_at?: string
          last_login?: string | null
        }
      }
      step_data: {
        Row: {
          id: string
          user_id: string
          date: string
          steps: number
          distance_meters: number | null
          calories: number | null
          active_minutes: number | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          user_id: string
          date: string
          steps: number
          distance_meters?: number | null
          calories?: number | null
          active_minutes?: number | null
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          date?: string
          steps?: number
          distance_meters?: number | null
          calories?: number | null
          active_minutes?: number | null
          created_at?: string
          updated_at?: string
        }
      }
      walk_sessions: {
        Row: {
          id: string
          user_id: string
          name: string | null
          start_time: string
          end_time: string | null
          duration_seconds: number | null
          distance_meters: number | null
          average_pace_minutes_per_km: number | null
          max_elevation_meters: number | null
          elevation_gain_meters: number | null
          weather_conditions: any | null
          notes: string | null
          is_public: boolean
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          user_id: string
          name?: string | null
          start_time: string
          end_time?: string | null
          duration_seconds?: number | null
          distance_meters?: number | null
          average_pace_minutes_per_km?: number | null
          max_elevation_meters?: number | null
          elevation_gain_meters?: number | null
          weather_conditions?: any | null
          notes?: string | null
          is_public?: boolean
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          name?: string | null
          start_time?: string
          end_time?: string | null
          duration_seconds?: number | null
          distance_meters?: number | null
          average_pace_minutes_per_km?: number | null
          max_elevation_meters?: number | null
          elevation_gain_meters?: number | null
          weather_conditions?: any | null
          notes?: string | null
          is_public?: boolean
          created_at?: string
          updated_at?: string
        }
      }
      route_coordinates: {
        Row: {
          id: string
          walk_session_id: string
          latitude: number
          longitude: number
          elevation_meters: number | null
          timestamp: string
          accuracy_meters: number | null
          created_at: string
        }
        Insert: {
          id?: string
          walk_session_id: string
          latitude: number
          longitude: number
          elevation_meters?: number | null
          timestamp: string
          accuracy_meters?: number | null
          created_at?: string
        }
        Update: {
          id?: string
          walk_session_id?: string
          latitude?: number
          longitude?: number
          elevation_meters?: number | null
          timestamp?: string
          accuracy_meters?: number | null
          created_at?: string
        }
      }
      saved_routes: {
        Row: {
          id: string
          user_id: string
          name: string
          description: string | null
          distance_meters: number | null
          estimated_duration_minutes: number | null
          difficulty_level: string | null
          route_data: any | null
          thumbnail_url: string | null
          is_public: boolean
          is_favorite: boolean
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          user_id: string
          name: string
          description?: string | null
          distance_meters?: number | null
          estimated_duration_minutes?: number | null
          difficulty_level?: string | null
          route_data?: any | null
          thumbnail_url?: string | null
          is_public?: boolean
          is_favorite?: boolean
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          name?: string
          description?: string | null
          distance_meters?: number | null
          estimated_duration_minutes?: number | null
          difficulty_level?: string | null
          route_data?: any | null
          thumbnail_url?: string | null
          is_public?: boolean
          is_favorite?: boolean
          created_at?: string
          updated_at?: string
        }
      }
      goals: {
        Row: {
          id: string
          user_id: string
          type: 'daily' | 'weekly' | 'monthly' | 'custom'
          target_steps: number
          target_distance_meters: number | null
          start_date: string
          end_date: string | null
          is_active: boolean
          created_at: string
        }
        Insert: {
          id?: string
          user_id: string
          type: 'daily' | 'weekly' | 'monthly' | 'custom'
          target_steps: number
          target_distance_meters?: number | null
          start_date: string
          end_date?: string | null
          is_active?: boolean
          created_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          type?: 'daily' | 'weekly' | 'monthly' | 'custom'
          target_steps?: number
          target_distance_meters?: number | null
          start_date?: string
          end_date?: string | null
          is_active?: boolean
          created_at?: string
        }
      }
      friendships: {
        Row: {
          id: string
          user_id: string
          friend_id: string
          status: 'pending' | 'accepted' | 'blocked'
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          user_id: string
          friend_id: string
          status?: 'pending' | 'accepted' | 'blocked'
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          friend_id?: string
          status?: 'pending' | 'accepted' | 'blocked'
          created_at?: string
          updated_at?: string
        }
      }
      challenges: {
        Row: {
          id: string
          creator_id: string
          name: string
          description: string | null
          challenge_type: 'steps' | 'distance' | 'duration'
          target_value: number
          start_date: string
          end_date: string
          is_public: boolean
          max_participants: number | null
          created_at: string
        }
        Insert: {
          id?: string
          creator_id: string
          name: string
          description?: string | null
          challenge_type: 'steps' | 'distance' | 'duration'
          target_value: number
          start_date: string
          end_date: string
          is_public?: boolean
          max_participants?: number | null
          created_at?: string
        }
        Update: {
          id?: string
          creator_id?: string
          name?: string
          description?: string | null
          challenge_type?: 'steps' | 'distance' | 'duration'
          target_value?: number
          start_date?: string
          end_date?: string
          is_public?: boolean
          max_participants?: number | null
          created_at?: string
        }
      }
      challenge_participants: {
        Row: {
          id: string
          challenge_id: string
          user_id: string
          current_progress: number
          joined_at: string
        }
        Insert: {
          id?: string
          challenge_id: string
          user_id: string
          current_progress?: number
          joined_at?: string
        }
        Update: {
          id?: string
          challenge_id?: string
          user_id?: string
          current_progress?: number
          joined_at?: string
        }
      }
      notifications: {
        Row: {
          id: string
          user_id: string
          type: string
          title: string
          message: string
          data: any | null
          is_read: boolean
          created_at: string
        }
        Insert: {
          id?: string
          user_id: string
          type: string
          title: string
          message: string
          data?: any | null
          is_read?: boolean
          created_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          type?: string
          title?: string
          message?: string
          data?: any | null
          is_read?: boolean
          created_at?: string
        }
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      get_step_statistics: {
        Args: {
          p_user_id: string
          p_start_date: string
          p_end_date: string
        }
        Returns: {
          total_steps: number
          total_distance: number
          total_calories: number
          average_steps: number
          current_streak: number
          longest_streak: number
        }[]
      }
      check_achievements: {
        Args: {
          p_user_id: string
        }
        Returns: {
          achievement_type: string
          achievement_name: string
          description: string
        }[]
      }
    }
    Enums: {
      [_ in never]: never
    }
  }
}

// Type-safe Supabase client
export type TypedSupabaseClient = ReturnType<typeof createClient<Database>> 