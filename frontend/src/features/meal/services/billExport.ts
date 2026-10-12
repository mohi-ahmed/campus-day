import { jsPDF } from 'jspdf'
import type { Bill } from './billingApi'

function amountNumber(value: string | number | null | undefined): number {
  const parsed = Number(value ?? 0)
  return Number.isFinite(parsed) ? parsed : 0
}

export function billCurrentTotal(bill: Bill): string | null {
  if (bill.amount != null) return String(bill.amount)
  if (bill.lines.some((line) => line.pricePending || line.lineTotal == null)) return null
  return bill.lines.reduce((total, line) => total + amountNumber(line.lineTotal), 0).toFixed(2)
}

export function billCurrentOutstanding(bill: Bill): string | null {
  if (bill.outstanding != null) return String(bill.outstanding)
  const total = billCurrentTotal(bill)
  return total == null ? null : Math.max(0, amountNumber(total) - amountNumber(bill.paid)).toFixed(2)
}

function csvCell(value: string | number | null | undefined): string {
  const text = String(value ?? '')
  const safe = /^[=+\-@]/.test(text) ? `'${text}` : text
  return `"${safe.replaceAll('"', '""')}"`
}

export function exportBillCsv(bill: Bill): void {
  const currentTotal = billCurrentTotal(bill)
  const currentOutstanding = billCurrentOutstanding(bill)
  const rows: Array<Array<string | number | null | undefined>> = [
    ['Campus Day Final Year monthly meal bill'],
    ['Student ID', bill.studentId],
    ['Billing month', `${bill.year}-${String(bill.month).padStart(2, '0')}`],
    ['Status', bill.status],
    ['Current total (BDT)', currentTotal],
    ['Paid (BDT)', bill.paid],
    ['Outstanding (BDT)', currentOutstanding],
    [],
    ['Daily order charges'],
    ['Date', 'Meal or item', 'Quantity', 'Unit price (BDT)', 'Line total (BDT)', 'Price status'],
    ...bill.lines.map((line) => [line.mealDate, line.label, line.quantity, line.unitPrice,
      line.lineTotal, line.pricePending ? 'Price pending' : 'Priced']),
    [],
    ['Offline payment history'],
    ['Date', 'Reference', 'Collected by', 'Amount (BDT)'],
    ...bill.paymentHistory.map((payment) => [payment.paymentDate, payment.reference,
      payment.channel === 'MEAL_MANAGER' ? 'Meal Manager' : payment.channel, payment.amount]),
  ]
  const csv = '\uFEFF' + rows.map((row) => row.map(csvCell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `final-year-bill-${bill.year}-${String(bill.month).padStart(2, '0')}-${bill.studentId}.csv`
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

type PdfRow = string[]

function pdfMoney(value: string | null | undefined): string {
  return value == null ? 'Pending' : `BDT ${amountNumber(value).toFixed(2)}`
}

function drawTable(doc: jsPDF, startY: number, headers: string[], rows: PdfRow[], widths: number[]): number {
  const left = 40
  const lineHeight = 11
  const pageBottom = doc.internal.pageSize.getHeight() - 40
  let y = startY

  const drawHeader = () => {
    doc.setFillColor(237, 243, 240)
    doc.setDrawColor(211, 221, 216)
    doc.rect(left, y, widths.reduce((sum, width) => sum + width, 0), 25, 'FD')
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8)
    let x = left
    headers.forEach((header, index) => {
      doc.text(header, x + 5, y + 16)
      x += widths[index]
    })
    y += 25
  }

  drawHeader()
  rows.forEach((row) => {
    const cells = row.map((value, index) => doc.splitTextToSize(value || '', widths[index] - 10) as string[])
    const rowHeight = Math.max(25, Math.max(...cells.map((cell) => cell.length)) * lineHeight + 10)
    if (y + rowHeight > pageBottom) {
      doc.addPage()
      y = 40
      drawHeader()
    }
    doc.setDrawColor(223, 231, 227)
    doc.rect(left, y, widths.reduce((sum, width) => sum + width, 0), rowHeight)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    let x = left
    cells.forEach((cell, index) => {
      doc.text(cell, x + 5, y + 15)
      x += widths[index]
    })
    y += rowHeight
  })
  return y
}

export function exportBillPdf(bill: Bill): void {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' })
  const currentTotal = billCurrentTotal(bill)
  const currentOutstanding = billCurrentOutstanding(bill)
  const month = `${bill.year}-${String(bill.month).padStart(2, '0')}`
  const pageWidth = doc.internal.pageSize.getWidth()

  doc.setProperties({ title: `Campus Day Final Year bill ${month}`, subject: 'Monthly meal order bill' })
  doc.setTextColor(23, 33, 29)
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(20)
  doc.text('Campus Day', 40, 48)
  doc.setTextColor(21, 149, 107)
  doc.setFontSize(10)
  doc.text('FINAL YEAR MONTHLY MEAL BILL', 40, 67)
  doc.setTextColor(23, 33, 29)
  doc.setFont('helvetica', 'normal')
  doc.setFontSize(10)
  doc.text(`Student ID: ${bill.studentId}`, 40, 91)
  doc.text(`Billing month: ${month}`, 40, 108)
  doc.text(`Status: ${bill.status === 'FINALIZED' ? 'Finalized' : 'Current month'}`, 40, 125)

  const summaries = [
    ['Current total', pdfMoney(currentTotal)],
    ['Paid offline', pdfMoney(String(bill.paid))],
    ['Remaining', pdfMoney(currentOutstanding)],
  ]
  const boxGap = 10
  const boxWidth = (pageWidth - 80 - boxGap * 2) / 3
  summaries.forEach(([label, value], index) => {
    const x = 40 + index * (boxWidth + boxGap)
    doc.setDrawColor(211, 221, 216)
    doc.roundedRect(x, 145, boxWidth, 62, 6, 6)
    doc.setTextColor(93, 107, 102)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.text(label, x + 10, 163)
    doc.setTextColor(23, 33, 29)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(13)
    doc.text(value, x + 10, 187)
  })

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(12)
  doc.text('Daily order breakdown', 40, 238)
  let y = drawTable(doc, 250,
    ['Date', 'Meal or item', 'Qty', 'Unit price', 'Line total'],
    bill.lines.map((line) => [line.mealDate, line.label, String(line.quantity),
      line.pricePending ? 'Price pending' : pdfMoney(line.unitPrice), pdfMoney(line.lineTotal)]),
    [78, 180, 45, 105, 107])

  if (y + 80 > doc.internal.pageSize.getHeight() - 40) {
    doc.addPage()
    y = 40
  } else {
    y += 28
  }
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(12)
  doc.text('Offline payment history', 40, y)
  y += 12
  drawTable(doc, y,
    ['Date', 'Reference', 'Recorded by', 'Amount'],
    bill.paymentHistory.map((payment) => [payment.paymentDate, payment.reference || 'Not provided',
      'Meal Manager', pdfMoney(payment.amount)]),
    [90, 175, 150, 100])

  doc.save(`final-year-bill-${month}-${bill.studentId}.pdf`)
}
