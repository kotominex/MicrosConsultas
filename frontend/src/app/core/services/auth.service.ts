import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config/api.config';
import {
  AuthenticatedUser,
  LoginRequest,
  RegisterRequest,
  TokenResponse,
} from '../models/user.model';

const TOKEN_KEY = 'auth_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserSignal = signal<AuthenticatedUser | null>(this.decodeStoredToken());

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly isAdmin = computed(() => this.currentUserSignal()?.role === 'ADMIN');

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  login(request: LoginRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/login`, request)
      .pipe(tap((response) => this.storeSession(response.token)));
  }

  register(request: RegisterRequest): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/register`, request)
      .pipe(tap((response) => this.storeSession(response.token)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSignal.set(null);
    this.router.navigateByUrl('/login');
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private storeSession(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.currentUserSignal.set(this.decodeToken(token));
  }

  private decodeStoredToken(): AuthenticatedUser | null {
    const token = this.getToken();
    return token ? this.decodeToken(token) : null;
  }

  private decodeToken(token: string): AuthenticatedUser | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return { userId: Number(payload.sub), email: payload.email, role: payload.role };
    } catch {
      return null;
    }
  }
}
