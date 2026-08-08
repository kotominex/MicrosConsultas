export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export interface Project {
  id: number;
  name: string;
  description: string | null;
  status: ProjectStatus;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  description: string | null;
  status: ProjectStatus | null;
}
