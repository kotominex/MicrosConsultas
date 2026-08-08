export type Role = 'ADMIN' | 'USER';

export interface AuthenticatedUser {
  userId: number;
  email: string;
  role: Role;
}

export interface TokenResponse {
  token: string;
  expiresInMs: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}
