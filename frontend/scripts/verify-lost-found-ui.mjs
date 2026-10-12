import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright-core'

const browser = await chromium.launch({
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  headless: true,
})

const baseUrl = process.env.LOST_FOUND_BASE_URL ?? 'http://127.0.0.1:4173'
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
const errors = []
const results = []

page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()) })
page.on('pageerror', (error) => errors.push(error.message))

await mkdir('verification', { recursive: true })
await page.goto(`${baseUrl}/lost-found`, { waitUntil: 'networkidle' })
await page.getByRole('heading', { name: /Lost something/ }).waitFor()

for (const [width, height] of [[390, 844], [768, 1024], [1440, 960]]) {
  await page.setViewportSize({ width, height })
  await page.waitForTimeout(180)
  const state = await page.evaluate(() => ({
    textLength: document.body.innerText.trim().length,
    overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    errorOverlay: Boolean(document.querySelector('.vite-error-overlay, #webpack-dev-server-client-overlay')),
    cards: document.querySelectorAll('.lf-card').length,
  }))
  results.push({ flow: `viewport-${width}`, ...state })
  await page.screenshot({ path: `verification/lost-found-${width}.png`, fullPage: true })
}

await page.setViewportSize({ width: 390, height: 844 })
await page.evaluate(() => window.scrollTo(0, 0))
await page.getByRole('button', { name: 'Report an item' }).click()
await page.getByRole('heading', { name: 'Report an item' }).waitFor()
const modalBox = await page.locator('.lf-modal').boundingBox()
const viewportCenter = await page.evaluate(() => window.scrollY + window.innerHeight / 2)
results.push({
  flow: 'report-modal-centered',
  passed: Boolean(modalBox && Math.abs((modalBox.y + modalBox.height / 2) - viewportCenter) < 24),
  modalBox,
})
await page.locator('input[name="title"]').fill('Grey calculator pouch')
await page.locator('input[name="place"]').fill('CSE Lab 3')
await page.getByRole('button', { name: 'Post item' }).click()
await page.getByRole('button', { name: 'Posted' }).waitFor()
await page.waitForTimeout(1300)
await page.getByText('Grey calculator pouch', { exact: true }).waitFor()
results.push({ flow: 'text-only-post', passed: true })

await page.locator('.lf-card').filter({ hasText: 'Black leather wallet' }).click()
await page.locator('#lf-detail-title').waitFor()
await page.getByPlaceholder('Write a comment').fill('I will check the security desk too.')
await page.getByRole('button', { name: 'Send comment' }).click()
await page.getByText('I will check the security desk too.', { exact: true }).waitFor()
await page.locator('.lf-detail').getByRole('button', { name: 'I found it', exact: true }).click()
await page.getByRole('heading', { name: 'I found this item' }).waitFor()
await page.getByPlaceholder('Where is it now? How can they get it back?').fill('I found it beside the library help desk.')
await page.getByRole('button', { name: 'Send message' }).click()
await page.getByText('I found it beside the library help desk.', { exact: true }).waitFor()
results.push({ flow: 'detail-comment-message', passed: true })
await page.waitForTimeout(380)
await page.screenshot({ path: 'verification/lost-found-thread-390.png' })

await page.getByRole('button', { name: 'Close dialog' }).click()
await page.getByRole('button', { name: 'Open messages' }).click()
await page.getByRole('heading', { name: 'Messages' }).waitFor()
const inboxCount = await page.locator('.lf-inbox__list > button').count()
results.push({ flow: 'message-inbox', passed: inboxCount >= 3, inboxCount })
await page.waitForTimeout(380)
await page.screenshot({ path: 'verification/lost-found-inbox-390.png' })

await browser.close()

const failed = errors.length > 0 || results.some((result) => result.passed === false || result.textLength === 0 || result.overflow > 1 || result.errorOverlay)
console.log(JSON.stringify({ passed: !failed, errors, results }, null, 2))
if (failed) process.exitCode = 1
