import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectsService } from '../../../core/services/projects.service';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ToastService } from '../../../core/services/toast.service';
import { Project } from '../../../core/models/project.model';

@Component({
  selector: 'app-project-list',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './project-list.html',
  styleUrl: './project-list.css',
})
export class ProjectList implements OnInit {
  private readonly projectsService = inject(ProjectsService);
  private readonly confirmService = inject(ConfirmService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly projects = signal<Project[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);

  readonly form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.projectsService.findAll().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toastService.show('No se pudieron cargar los proyectos', 'error');
      },
    });
  }

  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.creating.set(true);
    const value = this.form.getRawValue();

    this.projectsService
      .create({ name: value.name!, description: value.description || null, status: null })
      .subscribe({
        next: (project) => {
          this.projects.update((projects) => [...projects, project]);
          this.form.reset();
          this.creating.set(false);
          this.toastService.show('Proyecto creado');
        },
        error: () => {
          this.creating.set(false);
          this.toastService.show('No se pudo crear el proyecto', 'error');
        },
      });
  }

  toggleArchive(project: Project): void {
    const newStatus = project.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE';

    this.projectsService
      .update(project.id, { name: project.name, description: project.description, status: newStatus })
      .subscribe({
        next: (updated) => {
          this.projects.update((projects) =>
            projects.map((p) => (p.id === updated.id ? updated : p)),
          );
          this.toastService.show(
            newStatus === 'ARCHIVED' ? 'Proyecto archivado' : 'Proyecto reactivado',
          );
        },
        error: () => this.toastService.show('No se pudo actualizar el proyecto', 'error'),
      });
  }

  async remove(project: Project): Promise<void> {
    const confirmed = await this.confirmService.ask(`Eliminar el proyecto "${project.name}"?`);
    if (!confirmed) {
      return;
    }

    this.projectsService.delete(project.id).subscribe({
      next: () => {
        this.projects.update((projects) => projects.filter((p) => p.id !== project.id));
        this.toastService.show('Proyecto eliminado');
      },
      error: () => this.toastService.show('No se pudo eliminar el proyecto', 'error'),
    });
  }
}
