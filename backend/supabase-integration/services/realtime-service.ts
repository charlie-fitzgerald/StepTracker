import { supabase } from '../supabase-client'
import type { RealtimeChannel } from '@supabase/supabase-js'

export interface StepUpdate {
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

export interface WalkSessionUpdate {
  id: string
  user_id: string
  name: string | null
  start_time: string
  end_time: string | null
  distance_meters: number | null
  is_public: boolean
  created_at: string
}

export interface ChallengeUpdate {
  challenge_id: string
  user_id: string
  current_progress: number
  joined_at: string
}

export interface FriendActivity {
  type: 'walk_started' | 'goal_achieved' | 'challenge_completed' | 'step_milestone'
  userId: string
  userName: string
  data: any
  timestamp: string
}

export class RealtimeService {
  private channels: Map<string, RealtimeChannel> = new Map()

  // Subscribe to step updates for current user
  static subscribeToStepUpdates(callback: (payload: { new: StepUpdate; old: StepUpdate | null }) => void): RealtimeChannel {
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

  // Subscribe to walk session updates
  static subscribeToWalkSessions(callback: (payload: { new: WalkSessionUpdate; old: WalkSessionUpdate | null }) => void): RealtimeChannel {
    return supabase
      .channel('walk_sessions')
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'walk_sessions'
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to public walk sessions (friend activity)
  static subscribeToPublicWalks(callback: (payload: { new: WalkSessionUpdate; old: WalkSessionUpdate | null }) => void): RealtimeChannel {
    return supabase
      .channel('public_walks')
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'walk_sessions',
          filter: 'is_public=eq.true'
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to challenge updates
  static subscribeToChallengeUpdates(challengeId: string, callback: (payload: { new: ChallengeUpdate; old: ChallengeUpdate | null }) => void): RealtimeChannel {
    return supabase
      .channel(`challenge_${challengeId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'challenge_participants',
          filter: `challenge_id=eq.${challengeId}`
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to goal achievements
  static subscribeToGoalAchievements(callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel('goal_achievements')
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'notifications',
          filter: 'type=eq.achievement'
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to friend requests
  static subscribeToFriendRequests(callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel('friend_requests')
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'friendships',
          filter: 'status=eq.pending'
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to notifications
  static subscribeToNotifications(userId: string, callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel(`notifications_${userId}`)
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'notifications',
          filter: `user_id=eq.${userId}`
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to route sharing
  static subscribeToRouteSharing(callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel('route_sharing')
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'saved_routes',
          filter: 'is_public=eq.true'
        },
        callback
      )
      .subscribe()
  }

  // Subscribe to step milestones
  static subscribeToStepMilestones(callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel('step_milestones')
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'step_data'
        },
        (payload) => {
          const newSteps = payload.new.steps
          const oldSteps = payload.old?.steps || 0
          
          // Check for milestones (10k, 50k, 100k, etc.)
          const milestones = [10000, 50000, 100000, 500000, 1000000]
          const achievedMilestone = milestones.find(milestone => 
            oldSteps < milestone && newSteps >= milestone
          )
          
          if (achievedMilestone) {
            callback({
              type: 'step_milestone',
              userId: payload.new.user_id,
              milestone: achievedMilestone,
              totalSteps: newSteps,
              timestamp: new Date().toISOString()
            })
          }
        }
      )
      .subscribe()
  }

  // Subscribe to presence (who's online)
  static subscribeToPresence(callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel('presence')
      .on('presence', { event: 'sync' }, callback)
      .on('presence', { event: 'join' }, callback)
      .on('presence', { event: 'leave' }, callback)
      .subscribe()
  }

  // Track user presence
  static async trackPresence(userId: string, status: 'online' | 'offline' | 'walking' = 'online'): Promise<void> {
    const { error } = await supabase
      .channel('presence')
      .track({
        user_id: userId,
        status,
        last_seen: new Date().toISOString()
      })
    
    if (error) throw error
  }

  // Broadcast message to specific channel
  static async broadcastMessage(channel: string, message: any): Promise<void> {
    const { error } = await supabase
      .channel(channel)
      .send({
        type: 'broadcast',
        event: 'message',
        payload: message
      })
    
    if (error) throw error
  }

  // Send friend activity notification
  static async sendFriendActivity(activity: FriendActivity): Promise<void> {
    // Create notification for friends
    const { data: friends } = await supabase
      .from('friendships')
      .select('friend_id')
      .eq('user_id', activity.userId)
      .eq('status', 'accepted')
    
    if (friends && friends.length > 0) {
      const notifications = friends.map(friend => ({
        user_id: friend.friend_id,
        type: 'friend_activity',
        title: 'Friend Activity',
        message: `${activity.userName} ${this.getActivityMessage(activity.type)}`,
        data: activity,
        is_read: false
      }))
      
      await supabase.from('notifications').insert(notifications)
    }
  }

  // Get activity message for notifications
  private static getActivityMessage(type: string): string {
    switch (type) {
      case 'walk_started':
        return 'started a walk'
      case 'goal_achieved':
        return 'achieved a goal'
      case 'challenge_completed':
        return 'completed a challenge'
      case 'step_milestone':
        return 'reached a step milestone'
      default:
        return 'had some activity'
    }
  }

  // Subscribe to all real-time events for a user
  static subscribeToAllUserEvents(userId: string, callbacks: {
    onStepUpdate?: (payload: any) => void
    onWalkSession?: (payload: any) => void
    onNotification?: (payload: any) => void
    onFriendActivity?: (payload: any) => void
    onChallengeUpdate?: (payload: any) => void
  }): RealtimeChannel {
    const channel = supabase.channel(`user_events_${userId}`)
    
    // Step updates
    if (callbacks.onStepUpdate) {
      channel.on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'step_data',
          filter: `user_id=eq.${userId}`
        },
        callbacks.onStepUpdate
      )
    }
    
    // Walk sessions
    if (callbacks.onWalkSession) {
      channel.on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'walk_sessions',
          filter: `user_id=eq.${userId}`
        },
        callbacks.onWalkSession
      )
    }
    
    // Notifications
    if (callbacks.onNotification) {
      channel.on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'notifications',
          filter: `user_id=eq.${userId}`
        },
        callbacks.onNotification
      )
    }
    
    // Friend activity
    if (callbacks.onFriendActivity) {
      channel.on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'walk_sessions',
          filter: 'is_public=eq.true'
        },
        callbacks.onFriendActivity
      )
    }
    
    return channel.subscribe()
  }

  // Unsubscribe from all channels
  static async unsubscribeAll(): Promise<void> {
    const { error } = await supabase.removeAllChannels()
    if (error) throw error
  }

  // Get online friends
  static async getOnlineFriends(userId: string): Promise<string[]> {
    const { data: friends } = await supabase
      .from('friendships')
      .select('friend_id')
      .eq('user_id', userId)
      .eq('status', 'accepted')
    
    if (!friends) return []
    
    // Get presence data for friends
    const { data: presence } = await supabase
      .channel('presence')
      .getPresence()
    
    const onlineFriends = friends
      .map(friend => friend.friend_id)
      .filter(friendId => 
        presence && 
        Object.values(presence).some((user: any) => 
          user.user_id === friendId && 
          user.status === 'online'
        )
      )
    
    return onlineFriends
  }

  // Send typing indicator
  static async sendTypingIndicator(channel: string, userId: string, isTyping: boolean): Promise<void> {
    await supabase
      .channel(channel)
      .send({
        type: 'broadcast',
        event: 'typing',
        payload: {
          user_id: userId,
          is_typing: isTyping,
          timestamp: new Date().toISOString()
        }
      })
  }

  // Subscribe to typing indicators
  static subscribeToTypingIndicators(channel: string, callback: (payload: any) => void): RealtimeChannel {
    return supabase
      .channel(channel)
      .on('broadcast', { event: 'typing' }, callback)
      .subscribe()
  }
} 