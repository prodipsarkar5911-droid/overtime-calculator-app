package com.example.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.ShiftRecord
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExcelExporter {

    private val MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val DAY_OF_WEEK_NAMES = arrayOf(
        "Unknown", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    )

    /**
     * Generates a styled Excel (.xlsx) file in the app's cache/files directory
     * and returns the content Uri via FileProvider.
     */
    fun exportMonthlyReportToFile(
        context: Context,
        year: Int,
        month: Int,
        shifts: List<ShiftRecord>
    ): Pair<File, Uri> {
        val monthName = if (month in 1..12) MONTH_NAMES[month - 1] else "Month_$month"
        val fileName = "Overtime_Report_${monthName}_${year}.xlsx"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { outStream ->
            writeReportToStream(outStream, year, month, shifts)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return Pair(file, uri)
    }

    /**
     * Writes the monthly report directly to any OutputStream (e.g. Document Uri output stream).
     */
    fun writeReportToStream(
        outputStream: OutputStream,
        year: Int,
        month: Int,
        shifts: List<ShiftRecord>
    ) {
        val workbook = XSSFWorkbook()
        val monthName = if (month in 1..12) MONTH_NAMES[month - 1] else "Month_$month"
        val sheet = workbook.createSheet("${monthName} $year")
        sheet.isDisplayGridlines = true

        val colorMap = DefaultIndexedColorMap()

        // Colors
        val navyColor = XSSFColor(byteArrayOf(30.toByte(), 58.toByte(), 138.toByte()), colorMap) // #1E3A8A
        val lightBlueColor = XSSFColor(byteArrayOf(239.toByte(), 246.toByte(), 255.toByte()), colorMap) // #EFF6FF
        val sundayColor = XSSFColor(byteArrayOf(254.toByte(), 243.toByte(), 199.toByte()), colorMap) // #FEF3C7 amber
        val zebraColor = XSSFColor(byteArrayOf(248.toByte(), 250.toByte(), 252.toByte()), colorMap) // #F8FAFC
        val totalRowColor = XSSFColor(byteArrayOf(224.toByte(), 231.toByte(), 255.toByte()), colorMap) // #E0E7FF

        // Fonts
        val titleFont = workbook.createFont().apply {
            fontName = "Calibri"
            fontHeightInPoints = 14.toShort()
            bold = true
            color = IndexedColors.WHITE.index
        }

        val subTitleFont = workbook.createFont().apply {
            fontName = "Calibri"
            fontHeightInPoints = 10.toShort()
            italic = true
            color = IndexedColors.GREY_50_PERCENT.index
        }

        val headerFont = workbook.createFont().apply {
            fontName = "Calibri"
            fontHeightInPoints = 11.toShort()
            bold = true
            color = IndexedColors.WHITE.index
        }

        val boldFont = workbook.createFont().apply {
            fontName = "Calibri"
            fontHeightInPoints = 11.toShort()
            bold = true
        }

        val normalFont = workbook.createFont().apply {
            fontName = "Calibri"
            fontHeightInPoints = 10.toShort()
        }

        val sundayFont = workbook.createFont().apply {
            fontName = "Calibri"
            fontHeightInPoints = 10.toShort()
            bold = true
            color = IndexedColors.DARK_RED.index
        }

        // Cell Styles
        val titleStyle = workbook.createCellStyle().apply {
            setFont(titleFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(navyColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val metaStyle = workbook.createCellStyle().apply {
            setFont(subTitleFont)
            alignment = HorizontalAlignment.LEFT
        }

        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(navyColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val normalCenterStyle = workbook.createCellStyle().apply {
            setFont(normalFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val normalRightStyle = workbook.createCellStyle().apply {
            setFont(normalFont)
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val zebraCenterStyle = workbook.createCellStyle().apply {
            setFont(normalFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(zebraColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val zebraRightStyle = workbook.createCellStyle().apply {
            setFont(normalFont)
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(zebraColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val sundayCenterStyle = workbook.createCellStyle().apply {
            setFont(sundayFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(sundayColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val sundayRightStyle = workbook.createCellStyle().apply {
            setFont(sundayFont)
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(sundayColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val totalTitleStyle = workbook.createCellStyle().apply {
            setFont(boldFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(totalRowColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.MEDIUM
            borderBottom = BorderStyle.DOUBLE
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val totalValueStyle = workbook.createCellStyle().apply {
            setFont(boldFont)
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(totalRowColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.MEDIUM
            borderBottom = BorderStyle.DOUBLE
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val otTotalValueStyle = workbook.createCellStyle().apply {
            val otTotalFont = workbook.createFont().apply {
                fontName = "Calibri"
                fontHeightInPoints = 11.toShort()
                bold = true
                color = IndexedColors.DARK_BLUE.index
            }
            setFont(otTotalFont)
            alignment = HorizontalAlignment.RIGHT
            verticalAlignment = VerticalAlignment.CENTER
            setFillForegroundColor(totalRowColor)
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderTop = BorderStyle.MEDIUM
            borderBottom = BorderStyle.DOUBLE
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        var rowIdx = 0

        // 1. Title Row
        val titleRow = sheet.createRow(rowIdx++)
        titleRow.heightInPoints = 28f
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("OVERTIME & SHIFT TRACKER - $monthName $year")
        titleCell.cellStyle = titleStyle

        for (c in 1..7) {
            val cell = titleRow.createCell(c)
            cell.cellStyle = titleStyle
        }
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 7))

        // 2. Metadata Rows
        val generatedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Calendar.getInstance().time)
        val metaRow1 = sheet.createRow(rowIdx++)
        val metaCell1 = metaRow1.createCell(0)
        metaCell1.setCellValue("Generated: $generatedTime  |  Standard Rule: 10 hrs standard deduction (Mon-Sat)  |  Sunday: Full Worked Hours as Overtime")
        metaCell1.cellStyle = metaStyle
        sheet.addMergedRegion(CellRangeAddress(1, 1, 0, 7))

        rowIdx++ // Blank row for spacing

        // 3. Table Column Headers
        val headerRow = sheet.createRow(rowIdx++)
        headerRow.heightInPoints = 24f
        val headers = arrayOf(
            "Date",
            "Day of Week",
            "In Time",
            "Out Time",
            "Total Worked (Hrs)",
            "Standard Duty (Hrs)",
            "Overtime (Hrs)",
            "Notes / Remarks"
        )

        headers.forEachIndexed { i, title ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // 4. Data Rows
        var sumTotalWorked = 0.0
        var sumStandardDuty = 0.0
        var sumOvertime = 0.0

        val sortedShifts = shifts.sortedWith(
            compareBy<ShiftRecord> { it.dateString }
                .thenBy { it.inHour }
                .thenBy { it.inMinute }
        )

        for ((index, shift) in sortedShifts.withIndex()) {
            val dataRow = sheet.createRow(rowIdx++)
            dataRow.heightInPoints = 20f

            val isSunday = shift.isSunday
            val isZebra = index % 2 == 1

            val cStyle = when {
                isSunday -> sundayCenterStyle
                isZebra -> zebraCenterStyle
                else -> normalCenterStyle
            }

            val rStyle = when {
                isSunday -> sundayRightStyle
                isZebra -> zebraRightStyle
                else -> normalRightStyle
            }

            val dayName = if (shift.dayOfWeek in 1..7) DAY_OF_WEEK_NAMES[shift.dayOfWeek] else ""
            val dayDisplay = if (isSunday) "$dayName (Full OT)" else dayName

            // Col 0: Date
            val cell0 = dataRow.createCell(0)
            cell0.setCellValue(shift.dateString)
            cell0.cellStyle = cStyle

            // Col 1: Day of Week
            val cell1 = dataRow.createCell(1)
            cell1.setCellValue(dayDisplay)
            cell1.cellStyle = cStyle

            // Col 2: In Time
            val cell2 = dataRow.createCell(2)
            cell2.setCellValue(shift.inTimeFormatted)
            cell2.cellStyle = cStyle

            // Col 3: Out Time
            val cell3 = dataRow.createCell(3)
            cell3.setCellValue(shift.outTimeFormatted)
            cell3.cellStyle = cStyle

            // Col 4: Total Worked Hours
            val cell4 = dataRow.createCell(4)
            cell4.setCellValue(String.format(Locale.US, "%.2f", shift.totalHours))
            cell4.cellStyle = rStyle

            // Col 5: Standard Duty Hours
            val cell5 = dataRow.createCell(5)
            cell5.setCellValue(String.format(Locale.US, "%.2f", shift.standardDutyHours))
            cell5.cellStyle = rStyle

            // Col 6: Overtime Hours
            val cell6 = dataRow.createCell(6)
            cell6.setCellValue(String.format(Locale.US, "%.2f", shift.overtimeHours))
            cell6.cellStyle = rStyle

            // Col 7: Notes
            val cell7 = dataRow.createCell(7)
            val noteText = if (shift.notes.isNotBlank()) {
                shift.notes
            } else if (isSunday) {
                "Sunday Special OT"
            } else if (shift.overtimeHours > 0) {
                "${String.format(Locale.US, "%.1f", shift.overtimeHours)}h Overtime"
            } else {
                "Regular Duty"
            }
            cell7.setCellValue(noteText)
            cell7.cellStyle = cStyle

            sumTotalWorked += shift.totalHours
            sumStandardDuty += shift.standardDutyHours
            sumOvertime += shift.overtimeHours
        }

        // 5. Total Overtime Summary Row at Bottom
        val summaryRow = sheet.createRow(rowIdx++)
        summaryRow.heightInPoints = 24f

        // Span columns 0 to 3 for "TOTAL OVERTIME SUMMARY"
        val sumCell0 = summaryRow.createCell(0)
        sumCell0.setCellValue("TOTAL OVERTIME SUMMARY")
        sumCell0.cellStyle = totalTitleStyle

        for (c in 1..3) {
            val cell = summaryRow.createCell(c)
            cell.cellStyle = totalTitleStyle
        }
        val summaryStartRow = rowIdx - 1
        sheet.addMergedRegion(CellRangeAddress(summaryStartRow, summaryStartRow, 0, 3))

        // Total Worked Hours
        val sumCell4 = summaryRow.createCell(4)
        sumCell4.setCellValue(String.format(Locale.US, "%.2f hrs", sumTotalWorked))
        sumCell4.cellStyle = totalValueStyle

        // Total Standard Duty Hours
        val sumCell5 = summaryRow.createCell(5)
        sumCell5.setCellValue(String.format(Locale.US, "%.2f hrs", sumStandardDuty))
        sumCell5.cellStyle = totalValueStyle

        // Total Overtime Hours (Grand Total OT)
        val sumCell6 = summaryRow.createCell(6)
        sumCell6.setCellValue(String.format(Locale.US, "%.2f hrs OT", sumOvertime))
        sumCell6.cellStyle = otTotalValueStyle

        // Col 7: Summary tag
        val sumCell7 = summaryRow.createCell(7)
        sumCell7.setCellValue("${sortedShifts.size} Shifts Logged")
        sumCell7.cellStyle = totalTitleStyle

        // Auto-fit column widths
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
            // Add slight padding to prevent truncation on different devices
            val currentWidth = sheet.getColumnWidth(i)
            sheet.setColumnWidth(i, (currentWidth + 1200).coerceAtMost(256 * 40))
        }

        workbook.write(outputStream)
        workbook.close()
    }
}
