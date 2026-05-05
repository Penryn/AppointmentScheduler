import { expect, type Page, test } from '@playwright/test';

test.describe('visual regression', () => {
  test('anonymous login page stays visually stable', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('#login-form')).toBeVisible();
    await expect(page).toHaveScreenshot('login-page.png', { fullPage: true });
  });

  test('retail registration page stays visually stable', async ({ page }) => {
    await page.goto('/customers/new/retail');
    await expect(page.locator('form')).toBeVisible();
    await expect(page).toHaveScreenshot('retail-registration-page.png', { fullPage: true });
  });

  test('customer appointment list stays visually stable', async ({ page }) => {
    await login(page, 'customer_r');
    await page.goto('/appointments/all');
    await expect(page.locator('#appointments')).toBeVisible();
    await expect(page).toHaveScreenshot('customer-appointments-page.png', { fullPage: true });
  });

  test('admin customer list stays visually stable', async ({ page }) => {
    await login(page, 'admin');
    await page.goto('/customers/all?size=5');
    await expect(page.locator('#customers')).toBeVisible();
    await expect(page).toHaveScreenshot('admin-customers-page.png', { fullPage: true });
  });
});

async function login(page: Page, username: string) {
  await page.goto('/login');
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(process.env.E2E_PASSWORD || 'qwerty123');
  await page.locator('button[type="submit"]').click();
  await page.waitForURL((url: URL) => !url.pathname.includes('/login'), { timeout: 10_000 });
}
