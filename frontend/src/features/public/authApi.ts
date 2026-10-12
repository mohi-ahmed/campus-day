import { apiRequest, apiUrl } from '../../lib/api'
import type { AuthAccount, AuthTokenResponse } from './authSession'

export type AcademicYear = 'FIRST_YEAR' | 'SECOND_YEAR' | 'THIRD_YEAR' | 'FOURTH_YEAR' | 'FINAL_YEAR'
export type StudentGender = 'FEMALE' | 'MALE' | 'PREFER_NOT_TO_SAY'

type RegistrationResponse = {
  email: string
  otpExpiresAt: string
  message: string
}

type MessageResponse = { message: string }

export const authApi = {
  register: (payload: {
    fullName: string
    academicYear: AcademicYear
    gender: StudentGender
    phoneNumber: string
    email: string
    password: string
  }) => apiRequest<RegistrationResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, 'Account registration'),

  verifyEmail: (email: string, otp: string) => apiRequest<AuthTokenResponse>('/api/auth/verify-email', {
    method: 'POST',
    body: JSON.stringify({ email, otp }),
  }, 'Email verification'),

  resendVerification: (email: string) => apiRequest<RegistrationResponse>('/api/auth/resend-verification', {
    method: 'POST',
    body: JSON.stringify({ email }),
  }, 'Verification code'),

  login: (email: string, password: string) => apiRequest<AuthTokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  }, 'Sign in'),

  logout: (refreshToken: string) => apiRequest<void>('/api/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  }, 'Sign out'),

  forgotPassword: (email: string) => apiRequest<MessageResponse>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  }, 'Password recovery'),

  resetPassword: (email: string, otp: string, newPassword: string) => apiRequest<MessageResponse>('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ email, otp, newPassword }),
  }, 'Password reset'),

  exchangeOAuthCode: (code: string) => apiRequest<AuthTokenResponse>('/api/auth/oauth/exchange', {
    method: 'POST',
    body: JSON.stringify({ code }),
  }, 'Google sign in'),

  completeProfile: (payload: {
    fullName: string
    academicYear: AcademicYear
    gender: StudentGender
    phoneNumber: string
  }) => apiRequest<AuthTokenResponse>('/api/auth/complete-profile', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, 'Student profile'),

  me: () => apiRequest<AuthAccount>('/api/auth/me', undefined, 'Account'),

  googleAuthorizationUrl: () => apiUrl('/oauth2/authorization/google'),
}
