import { jsPDF } from 'jspdf'
import type { CollectionRow } from './billingApi'

function csvCell(value: string | number | null | undefined): string {
  const text = String(value ?? '')
  const safe = /^[=+\-@]/.test(text) ? `'${text}` : text
  return `"${safe.replaceAll('"', '""')}"`
}

function amount(value: string | null): string {
  return value == null ? 'Pending' : `BDT ${Number(value).toFixed(2)}`
}

function fileMonth(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`
}

export function exportCollectionsCsv(rows: CollectionRow[], year: number, month: number): void {
  const data = [
    ['Final Year student', 'Student ID', 'Total bill (BDT)', 'Paid (BDT)', 'Remaining (BDT)', 'Status'],
    ...rows.map((row) => [row.studentName, row.studentId, row.total, row.paid, row.outstanding,
      row.pricePending ? 'Price pending' : row.status === 'FINALIZED' ? Number(row.outstanding) === 0 ? 'Paid' : 'Due' : 'Not finalized']),
  ]
  const csv = '\uFEFF' + data.map((row) => row.map(csvCell).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `final-year-collections-${fileMonth(year, month)}.csv`
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function exportCollectionsPdf(rows: CollectionRow[], year: number, month: number): void {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' })
  const widths = [150, 105, 85, 80, 95]
  const headers = ['Student', 'Student ID', 'Total', 'Paid', 'Remaining']
  const left = 40
  const pageBottom = doc.internal.pageSize.getHeight() - 40
  let y = 95

  const header = () => {
    doc.setFillColor(237, 243, 240)
    doc.setDrawColor(211, 221, 216)
    doc.rect(left, y, 515, 25, 'FD')
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8)
    let x = left
    headers.forEach((label, index) => { doc.text(label, x + 5, y + 16); x += widths[index] })
    y += 25
  }

  doc.setProperties({ title: `Final Year collections ${fileMonth(year, month)}` })
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(20)
  doc.text('Final Year collections', 40, 48)
  doc.setFont('helvetica', 'normal')
  doc.setFontSize(10)
  doc.text(`Billing month: ${fileMonth(year, month)}`, 40, 68)
  header()
  rows.forEach((row) => {
    if (y + 30 > pageBottom) { doc.addPage(); y = 40; header() }
    const cells = [row.studentName, row.studentId, amount(row.total), amount(row.paid), amount(row.outstanding)]
    doc.setDrawColor(223, 231, 227)
    doc.rect(left, y, 515, 30)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    let x = left
    cells.forEach((value, index) => {
      const text = doc.splitTextToSize(value, widths[index] - 10) as string[]
      doc.text(text.slice(0, 2), x + 5, y + 13)
      x += widths[index]
    })
    y += 30
  })
  doc.save(`final-year-collections-${fileMonth(year, month)}.pdf`)
}
