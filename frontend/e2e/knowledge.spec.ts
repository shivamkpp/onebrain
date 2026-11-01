import { expect, test, type Page } from '@playwright/test';

async function signIn(page: Page, account: string) {
  await page.getByLabel('Account').selectOption(account);
  await page.getByLabel('Password').fill(process.env.ONEBRAIN_LOCAL_PASSWORD ?? 'onebrain-local');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByText('Signed in as')).toBeVisible();
}

test('note survives reload, is readable by a scoped reader, and is hidden from outsiders', async ({ page }) => {
  await page.goto('/');
  await signIn(page, 'contributor');
  const title = `Synthetic platform note ${Date.now()}`;
  await page.getByLabel('Title', { exact: true }).fill(title);
  await page.getByLabel('Note', { exact: true }).fill('Synthetic decision: keep the first service simple.');
  await page.getByLabel('Source title').fill('Synthetic architecture note');
  await page.getByLabel('Source URL').fill('https://example.com/synthetic/architecture');
  await page.getByRole('button', { name: 'Save note' }).click();
  const detail = page.getByRole('article', { name: 'Saved note' });
  await expect(detail.getByRole('heading', { name: title })).toBeVisible();
  await expect(detail.getByRole('link')).toHaveAttribute('href', 'https://example.com/synthetic/architecture');
  const id = await detail.getByTestId('note-id').innerText();

  await page.reload();
  await signIn(page, 'reader');
  await expect(page.getByRole('button', { name: 'Save note' })).toHaveCount(0);
  await page.getByLabel('Note ID').fill(id);
  await page.getByRole('button', { name: 'Open note' }).click();
  await expect(detail.getByRole('heading', { name: title })).toBeVisible();

  for (const account of ['outsider', 'external']) {
    await page.getByRole('button', { name: 'Sign out' }).click();
    await signIn(page, account);
    await page.getByLabel('Note ID').fill(id);
    await page.getByRole('button', { name: 'Open note' }).click();
    await expect(page.getByRole('alert')).toHaveText('Note not found or unavailable to this account.');
    await expect(detail).toHaveCount(0);
    await expect(page.getByText(title)).toHaveCount(0);
  }
});
