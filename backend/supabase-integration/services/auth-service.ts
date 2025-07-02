import { supabase } from '../supabase-client'
import type { Database } from '../supabase-client'

type Profile = Database['public']['Tables']['profiles']['Row']
type ProfileInsert = Database['public']['Tables']['profiles']['Insert']
type ProfileUpdate = Database['public']['Tables']['profiles']['Update']

export class AuthService {
  // Sign up with email/password
  static async signUp(email: string, password: string, name: string): Promise<{ user: any; session: any }> {
    const { data, error } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: {
          name
        }
      }
    })
    
    if (error) throw error
    
    // Create profile if user was created
    if (data.user) {
      const profileData: ProfileInsert = {
        id: data.user.id,
        email,
        name,
        units_system: 'metric',
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
      }
      
      const { error: profileError } = await supabase
        .from('profiles')
        .insert(profileData)
      
      if (profileError) {
        console.error('Error creating profile:', profileError)
        // Optionally delete the user if profile creation fails
        await supabase.auth.admin.deleteUser(data.user.id)
        throw profileError
      }
    }
    
    return data
  }

  // Sign in with email/password
  static async signIn(email: string, password: string): Promise<{ user: any; session: any }> {
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password
    })
    
    if (error) throw error
    
    // Update last login
    if (data.user) {
      await this.updateLastLogin(data.user.id)
    }
    
    return data
  }

  // Sign in with Google OAuth
  static async signInWithGoogle(): Promise<{ data: any; error: any }> {
    const { data, error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
        queryParams: {
          access_type: 'offline',
          prompt: 'consent'
        }
      }
    })
    
    return { data, error }
  }

  // Handle OAuth callback
  static async handleOAuthCallback(): Promise<{ user: any; session: any }> {
    const { data, error } = await supabase.auth.getSession()
    
    if (error) throw error
    
    if (data.session?.user) {
      // Check if profile exists, create if not
      const { data: profile } = await supabase
        .from('profiles')
        .select('*')
        .eq('id', data.session.user.id)
        .single()
      
      if (!profile) {
        const profileData: ProfileInsert = {
          id: data.session.user.id,
          email: data.session.user.email!,
          name: data.session.user.user_metadata?.name || 'User',
          avatar_url: data.session.user.user_metadata?.avatar_url,
          units_system: 'metric',
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
        }
        
        await supabase.from('profiles').insert(profileData)
      }
      
      // Update last login
      await this.updateLastLogin(data.session.user.id)
    }
    
    return data
  }

  // Sign out
  static async signOut(): Promise<void> {
    const { error } = await supabase.auth.signOut()
    if (error) throw error
  }

  // Get current user
  static async getCurrentUser(): Promise<any> {
    const { data: { user }, error } = await supabase.auth.getUser()
    if (error) throw error
    return user
  }

  // Get user profile
  static async getUserProfile(userId: string): Promise<Profile> {
    const { data, error } = await supabase
      .from('profiles')
      .select('*')
      .eq('id', userId)
      .single()
    
    if (error) throw error
    return data
  }

  // Update user profile
  static async updateUserProfile(userId: string, updates: ProfileUpdate): Promise<Profile> {
    const { data, error } = await supabase
      .from('profiles')
      .update({
        ...updates,
        updated_at: new Date().toISOString()
      })
      .eq('id', userId)
      .select()
      .single()
    
    if (error) throw error
    return data
  }

  // Update last login timestamp
  static async updateLastLogin(userId: string): Promise<void> {
    const { error } = await supabase
      .from('profiles')
      .update({
        last_login: new Date().toISOString()
      })
      .eq('id', userId)
    
    if (error) {
      console.error('Error updating last login:', error)
    }
  }

  // Delete user account
  static async deleteAccount(userId: string): Promise<void> {
    // Delete all user data first
    await supabase.from('step_data').delete().eq('user_id', userId)
    await supabase.from('walk_sessions').delete().eq('user_id', userId)
    await supabase.from('saved_routes').delete().eq('user_id', userId)
    await supabase.from('goals').delete().eq('user_id', userId)
    await supabase.from('friendships').delete().eq('user_id', userId)
    await supabase.from('friendships').delete().eq('friend_id', userId)
    await supabase.from('challenge_participants').delete().eq('user_id', userId)
    await supabase.from('notifications').delete().eq('user_id', userId)
    
    // Delete profile
    await supabase.from('profiles').delete().eq('id', userId)
    
    // Delete user from auth
    const { error } = await supabase.auth.admin.deleteUser(userId)
    if (error) throw error
  }

  // Reset password
  static async resetPassword(email: string): Promise<void> {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/auth/reset-password`
    })
    
    if (error) throw error
  }

  // Update password
  static async updatePassword(newPassword: string): Promise<void> {
    const { error } = await supabase.auth.updateUser({
      password: newPassword
    })
    
    if (error) throw error
  }

  // Get session
  static async getSession(): Promise<any> {
    const { data: { session }, error } = await supabase.auth.getSession()
    if (error) throw error
    return session
  }

  // Refresh session
  static async refreshSession(): Promise<any> {
    const { data: { session }, error } = await supabase.auth.refreshSession()
    if (error) throw error
    return session
  }

  // Listen to auth state changes
  static onAuthStateChange(callback: (event: string, session: any) => void) {
    return supabase.auth.onAuthStateChange(callback)
  }

  // Check if user is authenticated
  static async isAuthenticated(): Promise<boolean> {
    try {
      const user = await this.getCurrentUser()
      return !!user
    } catch {
      return false
    }
  }

  // Get user's timezone
  static getUserTimezone(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone
  }

  // Validate email format
  static isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  // Validate password strength
  static validatePassword(password: string): { isValid: boolean; errors: string[] } {
    const errors: string[] = []
    
    if (password.length < 8) {
      errors.push('Password must be at least 8 characters long')
    }
    
    if (!/[A-Z]/.test(password)) {
      errors.push('Password must contain at least one uppercase letter')
    }
    
    if (!/[a-z]/.test(password)) {
      errors.push('Password must contain at least one lowercase letter')
    }
    
    if (!/\d/.test(password)) {
      errors.push('Password must contain at least one number')
    }
    
    return {
      isValid: errors.length === 0,
      errors
    }
  }
} 