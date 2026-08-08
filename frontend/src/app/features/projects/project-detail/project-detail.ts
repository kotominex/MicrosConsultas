import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProjectsService } from '../../../core/services/projects.service';
import { TasksService } from '../../../core/services/tasks.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ToastService } from '../../../core/services/toast.service';
import { Project } from '../../../core/models/project.model';
import { Task, TaskStatus } from '../../../core/models/task.model';

const TASK_STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'DONE', 'OVERDUE'];

@Component({
  selector: 'app-project-detail',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './project-detail.html',
  styleUrl: './project-detail.css',
})
export class ProjectDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly projectsService = inject(ProjectsService);
  private readonly tasksService = inject(TasksService);
  private readonly confirmService = inject(ConfirmService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly project = signal<Project | null>(null);
  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);
  readonly statuses = TASK_STATUSES;

  private projectId!: number;

  readonly form = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    dueDate: ['', Validators.required],
  });

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadProject();
    this.loadTasks();
  }

  loadProject(): void {
    this.projectsService.findById(this.projectId).subscribe({
      next: (project) => this.project.set(project),
      error: () => this.toastService.show('No se pudo cargar el proyecto', 'error'),
    });
  }

  loadTasks(): void {
    this.loading.set(true);
    this.tasksService.findAllByProject(this.projectId).subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.show('No se pudieron cargar las tareas', 'error');
      },
    });
  }

  createTask(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.creating.set(true);
    const value = this.form.getRawValue();

    this.tasksService
      .create({
        projectId: this.projectId,
        title: value.title!,
        description: value.description || null,
        status: null,
        dueDate: value.dueDate!,
      })
      .subscribe({
        next: (task) => {
          this.tasks.update((tasks) => [...tasks, task]);
          this.form.reset();
          this.creating.set(false);
          this.toastService.show('Tarea creada');
        },
        error: (err) => {
          this.creating.set(false);
          this.toastService.show(
            err.status === 409 ? 'No se pueden crear tareas en un proyecto archivado' : 'No se pudo crear la tarea',
            'error',
          );
        },
      });
  }

  updateStatus(task: Task, status: TaskStatus): void {
    this.tasksService
      .update(task.id, {
        projectId: task.projectId,
        title: task.title,
        description: task.description,
        status,
        dueDate: task.dueDate,
      })
      .subscribe({
        next: (updated) => {
          this.tasks.update((tasks) => tasks.map((t) => (t.id === updated.id ? updated : t)));
          this.toastService.show('Tarea actualizada');
        },
        error: () => this.toastService.show('No se pudo actualizar la tarea', 'error'),
      });
  }

  async remove(task: Task): Promise<void> {
    const confirmed = await this.confirmService.ask(`Eliminar la tarea "${task.title}"?`);
    if (!confirmed) {
      return;
    }

    this.tasksService.delete(task.id).subscribe({
      next: () => {
        this.tasks.update((tasks) => tasks.filter((t) => t.id !== task.id));
        this.toastService.show('Tarea eliminada');
      },
      error: () => this.toastService.show('No se pudo eliminar la tarea', 'error'),
    });
  }
}
