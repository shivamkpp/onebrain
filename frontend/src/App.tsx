import { useState, type FormEvent } from 'react';
import { Api, type KnowledgeItem, type Session } from './api';

function message(error: unknown): string {
  return error instanceof Error ? error.message : 'The request could not be completed.';
}

export default function App() {
  const [connection, setConnection] = useState<{ api: Api; session: Session } | null>(null);
  return (
    <main>
      <header>
        <p className="eyebrow">ONEBRAIN / ENGINEERING KNOWLEDGE</p>
        <h1>Keep the context.</h1>
        <p>Capture a useful note and keep its source close.</p>
      </header>
      {connection ? (
        <Workspace api={connection.api} session={connection.session} onSignOut={() => setConnection(null)} />
      ) : (
        <SignIn onConnect={(api, session) => setConnection({ api, session })} />
      )}
    </main>
  );
}

function SignIn({ onConnect }: { onConnect: (api: Api, session: Session) => void }) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const fields = new FormData(form);
    const api = new Api(String(fields.get('username')), String(fields.get('password')));
    setBusy(true);
    setError('');
    try {
      const session = await api.connect();
      form.reset();
      onConnect(api, session);
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="panel sign-in">
      <p className="eyebrow">LOCAL DEVELOPMENT</p>
      <h2>Open your workspace</h2>
      <p>Use a synthetic account from the local setup instructions.</p>
      <form onSubmit={submit}>
        <label>Account<select name="username" defaultValue="contributor">
          <option value="contributor">contributor</option>
          <option value="reader">reader</option>
          <option value="outsider">outsider</option>
          <option value="external">external</option>
        </select></label>
        <label>Password<input name="password" type="password" autoComplete="current-password" required maxLength={200} /></label>
        <button disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
      {error && <p role="alert">{error}</p>}
    </section>
  );
}

function Workspace({ api, session, onSignOut }: { api: Api; session: Session; onSignOut: () => void }) {
  const [item, setItem] = useState<KnowledgeItem | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const scopes = session.scopes.filter(scope => scope.canContribute);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const fields = new FormData(form);
    const sourceTitle = String(fields.get('sourceTitle') ?? '').trim();
    const sourceUrl = String(fields.get('sourceUrl') ?? '').trim();
    if (Boolean(sourceTitle) !== Boolean(sourceUrl)) {
      setError('Provide both a source title and URL, or leave both empty.');
      return;
    }
    setBusy(true);
    setError('');
    setItem(null);
    try {
      const saved = await api.create({
        title: String(fields.get('title')),
        body: String(fields.get('body')),
        visibilityScopeId: String(fields.get('scope')),
        source: sourceUrl ? { title: sourceTitle, url: sourceUrl } : null,
      });
      // Read through the API after creation so the detail reflects the persisted record.
      setItem(await api.get(saved.id));
      form.reset();
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  async function open(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const fields = new FormData(event.currentTarget);
    setError('');
    setItem(null);
    setBusy(true);
    try {
      setItem(await api.get(String(fields.get('id')).trim()));
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="account-bar"><span>Signed in as <strong>{session.username}</strong></span>
        <button className="secondary" onClick={onSignOut}>Sign out</button></div>
      <div className="workspace">
        <section className="panel">
          <h2>Write a knowledge note</h2>
          {scopes.length ? <form onSubmit={create}>
            <label>Title<input name="title" required maxLength={200} /></label>
            <label>Note<textarea name="body" required maxLength={20000} rows={7} /></label>
            <label>Visibility scope<select name="scope" required>
              {scopes.map(scope => <option key={scope.id} value={scope.id}>{scope.name}</option>)}
            </select></label>
            <fieldset>
              <legend>Supporting source (optional)</legend>
              <label>Source title<input name="sourceTitle" maxLength={200} /></label>
              <label>Source URL<input name="sourceUrl" type="url" maxLength={2048} placeholder="https://example.com/decision" /></label>
            </fieldset>
            <p className="hint">Saved as a draft. Search and publication are coming later.</p>
            <button disabled={busy}>{busy ? 'Working…' : 'Save note'}</button>
          </form> : <p>This account can read its permitted notes but cannot create notes.</p>}
        </section>
        <div>
          <section className="panel">
            <h2>Open a saved note</h2>
            <form onSubmit={open}>
              <label>Note ID<input name="id" required pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" /></label>
              <button className="secondary" disabled={busy}>Open note</button>
            </form>
          </section>
          {error && <p className="error" role="alert">{error}</p>}
          {item && <NoteDetail item={item} />}
        </div>
      </div>
    </>
  );
}

function NoteDetail({ item }: { item: KnowledgeItem }) {
  // Links are never fetched by the server, and untrusted content is rendered as plain text.
  const safeUrl = item.source && /^https?:\/\//i.test(item.source.url) ? item.source.url : null;
  return (
    <article className="panel" aria-label="Saved note">
      <p className="eyebrow">DRAFT · REVISION {item.revision}</p>
      <h2>{item.title}</h2>
      <p className="note-body">{item.body}</p>
      {safeUrl && <p><a href={safeUrl} target="_blank" rel="noopener noreferrer">{item.source?.title}</a></p>}
      <dl>
        <dt>Author</dt><dd>{item.createdBy}</dd>
        <dt>Created</dt><dd><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString()}</time></dd>
        <dt>Scope</dt><dd>{item.visibilityScopeId}</dd>
        <dt>Note ID</dt><dd data-testid="note-id">{item.id}</dd>
      </dl>
    </article>
  );
}
