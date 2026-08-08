export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE' | 'OVERDUE';

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  dueDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskRequest {
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus | null;
  dueDate: string;
}
