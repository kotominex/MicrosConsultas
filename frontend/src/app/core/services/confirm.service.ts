import { Injectable, signal } from '@angular/core';

interface ConfirmRequest {
  message: string;
  resolve: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly requestSignal = signal<ConfirmRequest | null>(null);

  readonly request = this.requestSignal.asReadonly();

  ask(message: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.requestSignal.set({ message, resolve });
    });
  }

  resolve(value: boolean): void {
    this.requestSignal()?.resolve(value);
    this.requestSignal.set(null);
  }
}
