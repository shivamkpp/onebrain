import { afterEach, expect, test, vi } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

const session = {
  username: 'contributor', csrfToken: 'synthetic-csrf', csrfHeader: 'X-CSRF-TOKEN',
  scopes: [{ id: '30000000-0000-0000-0000-000000000001', name: 'Platform', canContribute: true }],
};
const note = {
  id: '40000000-0000-0000-0000-000000000001', title: 'Synthetic note', body: '<script>synthetic</script>',
  source: { title: 'Synthetic source', url: 'https://example.com/source' },
  visibilityScopeId: session.scopes[0].id, createdBy: 'contributor', revision: 1,
  itemType: 'NOTE', lifecycleStatus: 'DRAFT',
  createdAt: '2026-11-01T12:00:00Z', updatedAt: '2026-11-01T12:00:00Z',
};
const ok = (body: unknown) => ({ ok: true, status: 200, json: async () => body });

async function login() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Password'), 'synthetic-password');
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  await screen.findByRole('heading', { name: 'Write a knowledge note' });
  return user;
}

test('creates a note with CSRF and reads its persisted detail as plain text', async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(ok(session))
    .mockResolvedValueOnce(ok(note)).mockResolvedValueOnce(ok(note));
  vi.stubGlobal('fetch', fetchMock);
  render(<App />);
  const user = await login();
  await user.type(screen.getByLabelText('Title', { exact: true }), note.title);
  await user.type(screen.getByLabelText('Note', { exact: true }), note.body);
  await user.type(screen.getByLabelText('Source title'), note.source.title);
  await user.type(screen.getByLabelText('Source URL'), note.source.url);
  await user.click(screen.getByRole('button', { name: 'Save note' }));
  const detail = await screen.findByRole('article', { name: 'Saved note' });
  expect(within(detail).getByText(note.body)).toBeTruthy();
  expect(detail.querySelector('script')).toBeNull();
  expect(within(detail).getByRole('link').getAttribute('href')).toBe(note.source.url);
  expect(fetchMock.mock.calls[1][1].headers['X-CSRF-TOKEN']).toBe('synthetic-csrf');
  expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({
    title: note.title, body: note.body, source: note.source, visibilityScopeId: note.visibilityScopeId,
  });
  expect(fetchMock.mock.calls[2][0]).toBe(`/api/v1/knowledge/${note.id}`);
});

test('an incomplete source reference does not submit a note', async () => {
  const fetchMock = vi.fn().mockResolvedValue(ok(session));
  vi.stubGlobal('fetch', fetchMock);
  render(<App />);
  const user = await login();
  await user.type(screen.getByLabelText('Title', { exact: true }), 'Synthetic note');
  await user.type(screen.getByLabelText('Note', { exact: true }), 'Synthetic body');
  await user.type(screen.getByLabelText('Source title'), 'Missing URL');
  await user.click(screen.getByRole('button', { name: 'Save note' }));
  expect((await screen.findByRole('alert')).textContent).toContain('both a source title and URL');
  expect(fetchMock).toHaveBeenCalledTimes(1);
});

test('a failed lookup clears the old detail and displays a safe error', async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(ok(session)).mockResolvedValueOnce(ok(note))
    .mockResolvedValueOnce({ ok: false, status: 404 });
  vi.stubGlobal('fetch', fetchMock);
  render(<App />);
  const user = await login();
  await user.type(screen.getByLabelText('Note ID'), note.id);
  await user.click(screen.getByRole('button', { name: 'Open note' }));
  await screen.findByRole('article', { name: 'Saved note' });
  await user.click(screen.getByRole('button', { name: 'Open note' }));
  expect((await screen.findByRole('alert')).textContent).toContain('not found or unavailable');
  expect(screen.queryByRole('article', { name: 'Saved note' })).toBeNull();
});

test('read-only accounts have lookup access without a creation form', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok({
    ...session, username: 'reader', scopes: [{ ...session.scopes[0], canContribute: false }],
  })));
  render(<App />);
  await login();
  expect(screen.queryByRole('button', { name: 'Save note' })).toBeNull();
  expect(screen.getByRole('button', { name: 'Open note' })).toBeTruthy();
});
