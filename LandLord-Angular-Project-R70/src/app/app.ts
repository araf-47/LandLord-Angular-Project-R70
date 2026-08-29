import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastHostComponent } from './shared/toast-host.component';
import { ConfirmDialogComponent } from './shared/confirm-dialog.component';
import { ThemeToggleComponent } from './shared/theme-toggle.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastHostComponent, ConfirmDialogComponent, ThemeToggleComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
