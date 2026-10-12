import { apiRequest } from '../../lib/api'

export interface DailyStudyTrendRecord {
  date: string
  plannedMinutes: number
  focusedMinutes: number
  completedTasks: number
  completedHabits: number
}

export interface SubjectPerformanceRecord {
  subject: string
  color: string | null
  focusedMinutes: number
  plannedMinutes: number
  completedTasks: number
  totalTasks: number
  completedTopics: number
  totalTopics: number
  taskCompletionPercentage: number
  syllabusCompletionPercentage: number
}

export interface StudyInsightsRecord {
  period: { from: string; to: string; totalDays: number }
  focus: {
    totalMinutes: number
    totalSessions: number
    completedSessions: number
    stoppedSessions: number
    averageSessionMinutes: number
    activeDays: number
    currentStreakDays: number
    longestStreakDays: number
  }
  tasks: {
    totalTasks: number
    completedTasks: number
    pendingTasks: number
    overdueTasks: number
    scheduledTasks: number
    completionPercentage: number
    byPriority: Array<{ label: string; totalTasks: number; completedTasks: number; pendingTasks: number; completionPercentage: number }>
    byType: Array<{ label: string; totalTasks: number; completedTasks: number; pendingTasks: number; completionPercentage: number }>
  }
  syllabus: {
    totalTopics: number
    completedTopics: number
    revisionTopics: number
    learningTopics: number
    notStartedTopics: number
    overdueTopics: number
    completionPercentage: number
  }
  habits: {
    activeHabits: number
    scheduledCompletions: number
    completedCompletions: number
    completionPercentage: number
    habits: Array<{
      habitId: string
      name: string
      scheduledDays: number
      completedDays: number
      completionPercentage: number
      currentStreakDays: number
      longestStreakDays: number
    }>
  }
  routine: {
    activeBlocks: number
    classBlocks: number
    selfStudyBlocks: number
    weeklyPlannedMinutes: number
    weeklyClassMinutes: number
    weeklySelfStudyMinutes: number
  }
  goals: {
    dailyFocusGoalMinutes: number
    focusedMinutesToday: number
    weeklySessionGoal: number
    sessionsThisWeek: number
    topicCompletionGoal: number
    completedTopics: number
  }
  dailyTrend: DailyStudyTrendRecord[]
  subjects: SubjectPerformanceRecord[]
}

const base = '/api/study/insights'

export const studyInsightsApi = {
  summary: (studentId: string, from: string, to: string) =>
    apiRequest<StudyInsightsRecord>(
      `${base}?studentId=${encodeURIComponent(studentId)}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`,
      undefined,
      'Study insights API',
    ),
}
