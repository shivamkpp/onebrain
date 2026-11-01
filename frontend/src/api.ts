export type Scope = {
  id: string;
  organizationId: string;
  ownerTeamId: string;
  name: string;
  canContribute: boolean;
};

export type Session = {
  username: string;
  scopes: Scope[];
  csrfToken: string;
  csrfHeader: string;
};

export type SourceReference = { title: string; url: string };
export type CreateNote = {
  title: string;
  body: string;
  visibilityScopeId: string;
  source: SourceReference | null;
};

export type KnowledgeItem = CreateNote & {
  id: string;
  organizationId: string;
  ownerTeamId: string;
  itemType: 'NOTE';
  lifecycleStatus: 'DRAFT';
  revision: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); }
}

// Credentials live only in this client instance in memory; never in storage or a URL.
export class Api {
  private readonly authorization: string;
  private session: Session | null = null;

  constructor(username: string, password: string) {
    const bytes = new TextEncoder().encode(`${username}:${password}`);
    this.authorization = `Basic ${btoa(String.fromCharCode(...bytes))}`;
  }

  async connect(): Promise<Session> {
    this.session = await this.request<Session>('/session');
    return this.session;
  }

  async create(note: CreateNote): Promise<KnowledgeItem> {
    if (!this.session) throw new Error('Sign in to continue.');
    return this.request<KnowledgeItem>('/knowledge', {
      method: 'POST',
      headers: { [this.session.csrfHeader]: this.session.csrfToken },
      body: JSON.stringify(note),
    });
  }

  async get(id: string): Promise<KnowledgeItem> {
    return this.request<KnowledgeItem>(`/knowledge/${encodeURIComponent(id)}`);
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
      response = await fetch(`/api/v1${path}`, {
        ...options,
        credentials: 'same-origin',
        headers: {
          Authorization: this.authorization,
          ...(options.body ? { 'Content-Type': 'application/json' } : {}),
          ...options.headers,
        },
      });
    } catch {
      throw new Error('Cannot reach OneBrain. Check that the backend is running.');
    }
    if (!response.ok) {
      const messages: Record<number, string> = {
        400: 'Check the note fields and source URL.',
        401: 'The username or password was not accepted.',
        403: 'You do not have permission, or your session expired. Sign in again.',
        404: 'Note not found or unavailable to this account.',
      };
      throw new ApiError(response.status, messages[response.status] ?? 'The request could not be completed.');
    }
    return response.json() as Promise<T>;
  }
}
