import { mkdir } from 'node:fs/promises'
import { chromium } from 'playwright-core'

const browser = await chromium.launch({
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  headless: true,
})

const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
const baseUrl = process.env.STUDY_BASE_URL ?? 'http://127.0.0.1:4173'
const errors = []
page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()) })
page.on('pageerror', (error) => errors.push(error.message))

await mkdir('verification', { recursive: true })
await page.goto(`${baseUrl}/study`, { waitUntil: 'networkidle' })
await page.getByRole('heading', { name: /Good (morning|afternoon|evening)/ }).waitFor()

const results = []

const pageState = async (label) => {
  const state = await page.evaluate(() => ({
    textLength: document.body.innerText.trim().length,
    overflow: document.documentElement.scrollWidth - document.documentElement.clientWidth,
    errorOverlay: Boolean(document.querySelector('.vite-error-overlay, #webpack-dev-server-client-overlay')),
  }))
  results.push({ flow: label, ...state })
}

await pageState('today-load')
await page.screenshot({ path: 'verification/study-today-1440.png', fullPage: true })

await page.getByPlaceholder('Quick add: Tomorrow 8 PM Pharmacology revise').fill('Tomorrow 8 PM Pharmacology revise chapter 4')
await page.getByPlaceholder('Quick add: Tomorrow 8 PM Pharmacology revise').press('Enter')
await page.getByRole('button', { name: 'Tasks', exact: true }).click()
await page.getByText('Pharmacology revise chapter 4', { exact: true }).waitFor()
results.push({ flow: 'quick-add-task', passed: true })

await page.getByRole('button', { name: 'New task', exact: true }).click()
await page.getByLabel('Title').fill('Java stream practice')
await page.getByLabel('Subject').fill('Advanced Java')
await page.getByLabel('Type').fill('Coding exercise')
const priorityOptions = await page.locator('select[name="priority"] option').allTextContents()
await page.getByLabel('Priority').selectOption('High')
await page.screenshot({ path: 'verification/custom-task-modal.png', fullPage: true })
await page.getByRole('button', { name: 'Save', exact: true }).click()
const customTask = page.locator('.study-planner-row').filter({ hasText: 'Java stream practice' })
await customTask.waitFor()
const customTaskText = await customTask.textContent()
const customTypeFilter = page.getByRole('button', { name: 'Coding exercise', exact: true })
await customTypeFilter.click()
await customTask.waitFor()
results.push({
  flow: 'custom-task-fields',
  passed: customTaskText?.includes('Advanced Java') && customTaskText.includes('Coding exercise') && priorityOptions.join(',') === 'High,Med,Low',
  priorityOptions,
})

await page.getByRole('button', { name: 'Routine', exact: true }).click()
await page.getByRole('button', { name: 'Add block' }).click()
await page.getByLabel('Subject').fill('Microbiology')
await page.getByLabel('Type').fill('Lab class')
await page.getByLabel('Topic').fill('Microbiology review')
await page.getByLabel('Location').fill('Reading room')
await page.screenshot({ path: 'verification/custom-routine-modal.png', fullPage: true })
await page.getByRole('button', { name: 'Save', exact: true }).click()
const customRoutine = page.locator('.study-row').filter({ hasText: 'Microbiology review' })
await customRoutine.waitFor()
const customRoutineText = await customRoutine.textContent()
results.push({ flow: 'custom-routine-fields', passed: customRoutineText?.includes('Microbiology') && customRoutineText.includes('Lab class') })

await page.getByRole('button', { name: 'Focus', exact: true }).click()
const focusTaskOptions = await page.getByLabel('Task').locator('option').allTextContents()
const hardcodedSubjectPickerCount = await page.locator('.study-subject-picker').count()
const timerModes = page.locator('.study-focus__modes')
await timerModes.getByRole('button', { name: /Short break/ }).click()
const shortBreakTime = await page.locator('.study-focus__ring strong').textContent()
await timerModes.getByRole('button', { name: /Long break/ }).click()
const longBreakTime = await page.locator('.study-focus__ring strong').textContent()
await timerModes.getByRole('button', { name: /^Focus/ }).click()
results.push({
  flow: 'user-created-focus-tasks-and-breaks',
  passed: focusTaskOptions.includes('Java stream practice') && !focusTaskOptions.includes('Autonomic drugs') && hardcodedSubjectPickerCount === 0 && shortBreakTime === '05:00' && longBreakTime === '15:00',
  focusTaskOptions,
  hardcodedSubjectPickerCount,
  shortBreakTime,
  longBreakTime,
})
await page.screenshot({ path: 'verification/focus-task-and-breaks.png', fullPage: true })
const timerBefore = await page.locator('.study-focus__ring strong').textContent()
await page.getByRole('button', { name: 'Start', exact: true }).click()
await page.waitForTimeout(1200)
await page.getByRole('button', { name: 'Pause', exact: true }).click()
const timerAfter = await page.locator('.study-focus__ring strong').textContent()
results.push({ flow: 'focus-timer', passed: timerBefore !== timerAfter, timerBefore, timerAfter })

await page.getByRole('button', { name: 'Taskbot', exact: true }).click()
await page.getByRole('button', { name: 'Plan my day', exact: true }).click()
await page.getByText(/Plan: finish the next focus block/).waitFor()
await page.getByPlaceholder('Write a task or ask for a study plan').fill('Revise pathology after dinner')
await page.getByRole('button', { name: 'Send to Taskbot' }).click()
await page.getByText(/Revise pathology after dinner/).waitFor()
results.push({ flow: 'taskbot-plan', passed: true })

await page.getByRole('button', { name: 'Calendar', exact: true }).click()
const firstRange = await page.locator('.study-calendar-controls > div:last-child strong').textContent()
await page.getByRole('button', { name: 'Next period' }).click()
const nextRange = await page.locator('.study-calendar-controls > div:last-child strong').textContent()
results.push({ flow: 'calendar-navigation', passed: firstRange !== nextRange, firstRange, nextRange })

await page.setViewportSize({ width: 390, height: 844 })
await page.goto(`${baseUrl}/study`, { waitUntil: 'networkidle' })
await pageState('mobile-load')
await page.screenshot({ path: 'verification/study-today-390.png', fullPage: true })

await browser.close()

const failed = errors.length > 0 || results.some((result) => result.passed === false || result.textLength === 0 || result.overflow > 1 || result.errorOverlay)
console.log(JSON.stringify({ passed: !failed, errors, results }, null, 2))
if (failed) process.exitCode = 1
