import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/visual',
  outputDir: 'target/playwright-results',
  reporter: [
    ['list'],
    ['html', { outputFolder: 'target/playwright-report', open: 'never' }],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:8080',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } },
    },
    {
      name: 'mobile-chromium',
      use: { ...devices['Pixel 5'] },
    },
  ],
});
