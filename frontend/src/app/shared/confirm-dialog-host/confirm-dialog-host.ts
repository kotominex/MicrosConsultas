import { Component, HostListener, inject } from '@angular/core';
import { ConfirmService } from '../../core/services/confirm.service';
import { AutofocusDirective } from '../../core/directives/autofocus.directive';

@Component({
  selector: 'app-confirm-dialog-host',
  imports: [AutofocusDirective],
  templateUrl: './confirm-dialog-host.html',
  styleUrl: './confirm-dialog-host.css',
})
export class ConfirmDialogHost {
  private readonly confirmService = inject(ConfirmService);

  readonly request = this.confirmService.request;

  resolve(value: boolean): void {
    this.confirmService.resolve(value);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.request()) {
      this.resolve(false);
    }
  }
}
