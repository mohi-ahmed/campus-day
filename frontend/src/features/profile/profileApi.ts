import { apiRequest } from '../../lib/api'
import type { DemoUser } from '../meal/types'
import type { AcademicYear, StudentGender } from '../public/authApi'

export type StudentProfile = {
  studentId: string
  name: string
  email: string
  academicYear: AcademicYear
  phoneNumber: string
  gender: StudentGender
  profileImageUrl: string | null
  mealManagerAccess: boolean
}

export type UpdateStudentProfile = {
  fullName: string
  academicYear: AcademicYear
  gender: StudentGender
  phoneNumber: string
}

const academicYearNumber: Record<AcademicYear, 1 | 2 | 3 | 4 | 5> = {
  FIRST_YEAR: 1,
  SECOND_YEAR: 2,
  THIRD_YEAR: 3,
  FOURTH_YEAR: 4,
  FINAL_YEAR: 5,
}

function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'ST'
}

export function toAppUser(profile: StudentProfile): DemoUser {
  const year = academicYearNumber[profile.academicYear]
  return {
    id: profile.studentId,
    name: profile.name,
    email: profile.email,
    initials: initials(profile.name),
    academicYear: year,
    context: profile.mealManagerAccess ? 'manager' : year === 5 ? 'final-year' : 'regular',
    isMealManager: profile.mealManagerAccess,
    profileImageUrl: profile.profileImageUrl,
  }
}

export const profileApi = {
  me: () => apiRequest<StudentProfile>('/api/students/me/profile', undefined, 'Profile'),

  update: (payload: UpdateStudentProfile) => apiRequest<StudentProfile>('/api/students/me/profile', {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, 'Profile update'),

  uploadImage: (file: File) => {
    const body = new FormData()
    body.append('file', file)
    return apiRequest<StudentProfile>('/api/students/me/profile/image', {
      method: 'POST',
      body,
    }, 'Profile photo upload')
  },

  removeImage: () => apiRequest<void>('/api/students/me/profile/image', {
    method: 'DELETE',
  }, 'Profile photo removal'),
}
