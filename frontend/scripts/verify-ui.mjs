import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright-core'

const browser = await chromium.launch({
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  headless: true,
})
const page = await browser.newPage()
const errors = []
page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()) })
page.on('pageerror', (error) => errors.push(error.message))

await mkdir('verification', { recursive: true })
await page.goto('http://127.0.0.1:4173/meal', { waitUntil: 'networkidle' })

const widths = [375, 390, 430, 768, 820, 1024, 1280, 1440, 1920]
const results = []

for (const width of widths) {
  await page.setViewportSize({ width, height: width < 768 ? 844 : width < 1024 ? 1024 : 900 })
  await page.waitForTimeout(250)
  const state = await page.evaluate(() => ({
    title: document.querySelector('h1')?.textContent?.trim(),
    bodyTextLength: document.body.innerText.trim().length,
    overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    errorOverlay: Boolean(document.querySelector('.vite-error-overlay, #webpack-dev-server-client-overlay')),
    bottomNavVisible: getComputedStyle(document.querySelector('.bottom-nav')).display !== 'none',
    sidebarVisible: getComputedStyle(document.querySelector('.sidebar')).position !== 'fixed',
  }))
  if ([375, 820, 1440, 1920].includes(width)) await page.screenshot({ path: `verification/dashboard-${width}.png`, fullPage: true })
  results.push({ width, ...state })
}

await page.setViewportSize({ width: 390, height: 844 })
await page.locator('.bottom-nav a[href="/meal/absence"]').click()
await page.getByRole('heading', { name: 'Absence notice' }).waitFor()
results.push({ flow: 'regular-absence', url: page.url(), heading: await page.locator('h1').first().textContent() })
await page.locator('.bottom-nav a[href="/meal/bills"]').click()
await page.getByRole('heading', { name: 'Bills & payments' }).waitFor()
results.push({ flow: 'regular-bills', url: page.url(), heading: await page.locator('h1').first().textContent() })

await page.selectOption('.demo-context select', 'final-year')
await page.locator('.bottom-nav a[href="/meal/order"]').click()
await page.getByRole('heading', { name: 'Place order' }).waitFor()
await page.locator('.order-meal').nth(2).waitFor()
results.push({ flow: 'final-year-order', url: page.url(), heading: await page.locator('h1').first().textContent(), eatingControls: await page.locator('.order-meal').count() })
await page.screenshot({ path: 'verification/final-year-order-390.png', fullPage: true })

await page.selectOption('.demo-context select', 'manager')
await page.locator('.bottom-nav a[href="/meal/manage"]').click()
await page.getByRole('heading', { name: 'Manager dashboard' }).waitFor()
await page.locator('.metric').nth(3).waitFor()
await page.setViewportSize({ width: 1280, height: 900 })
await page.waitForTimeout(250)
results.push({ flow: 'manager-dashboard', url: page.url(), heading: await page.locator('h1').first().textContent(), metrics: await page.locator('.metric').count() })
await page.screenshot({ path: 'verification/manager-dashboard-1280.png', fullPage: true })

await page.locator('.sidebar a[href="/meal/manage/fraction"]').click()
await page.getByRole('heading', { name: 'Digital fraction sheet' }).waitFor()
await page.locator('tbody tr').nth(4).waitFor()
await page.setViewportSize({ width: 1024, height: 900 })
await page.waitForTimeout(250)
results.push({ flow: 'fraction-sheet', url: page.url(), heading: await page.locator('h1').first().textContent(), rows: await page.locator('tbody tr').count() })
await page.screenshot({ path: 'verification/fraction-sheet-1024.png', fullPage: true })

const managerRoutes = [
  ['/meal/manage/period', 'Management period'],
  ['/meal/manage/menu', 'Menu management'],
  ['/meal/manage/prices', 'Price management'],
  ['/meal/manage/orders', 'Final Year order overview'],
  ['/meal/manage/cooking-plan', 'Cooking plan'],
  ['/meal/manage/stock', 'Ingredients & stock'],
  ['/meal/manage/kitchen-issue', 'Kitchen issue'],
  ['/meal/manage/purchases', 'Purchases'],
  ['/meal/manage/suppliers', 'Suppliers & dues'],
  ['/meal/manage/bills', 'Monthly bills'],
  ['/meal/manage/collections', 'Final Year collections'],
  ['/meal/manage/handover', 'Stock handover'],
  ['/meal/manage/reports', 'Reports'],
]
for (const [route, heading] of managerRoutes) {
  await page.locator(`.sidebar a[href="${route}"]`).click()
  await page.getByRole('heading', { name: heading }).waitFor()
  results.push({ route, heading: await page.locator('h1').first().textContent() })
}
await page.locator('.sidebar a[href="/meal/manage/purchases"]').click()
await page.getByRole('heading', { name: 'Purchases' }).waitFor()
await page.locator('a[href="/meal/manage/purchases/PUR-240921"]').click()
await page.getByRole('heading', { name: 'PUR-240921' }).waitFor()
results.push({ flow: 'purchase-detail', url: page.url(), heading: await page.locator('h1').first().textContent() })

await browser.close()

const failed = results.some((item) => item.bodyTextLength === 0 || item.overflow > 1 || item.errorOverlay) || errors.length > 0
console.log(JSON.stringify({ passed: !failed, errors, results }, null, 2))
if (failed) process.exitCode = 1
