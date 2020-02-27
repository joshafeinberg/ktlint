package com.pinterest.ktlint.internal

import com.pinterest.ktlint.core.LintError
import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Loads the baseline file if one is provided.
 *
 * @param baselineFilePath the path to the xml baseline file
 * @return a [CurrentBaseline] with the file details
 */
internal fun loadBaseline(baselineFilePath: String): CurrentBaseline {
    if (baselineFilePath.isBlank()) {
        return CurrentBaseline(null, false)
    }

    var baselineRules: Map<String, List<LintError>>? = null
    var baselineGenerationNeeded = true
    val baselineFile = Paths.get(baselineFilePath).toFile()
    if (baselineFile.exists()) {
        try {
            baselineRules = parseBaseline(baselineFile)
            baselineGenerationNeeded = false
        } catch (e: IOException) {
            System.err.println("Unable to parse baseline file: $baselineFilePath")
            baselineGenerationNeeded = true
        } catch (e: ParserConfigurationException) {
            System.err.println("Unable to parse baseline file: $baselineFilePath")
            baselineGenerationNeeded = true
        } catch (e: SAXException) {
            System.err.println("Unable to parse baseline file: $baselineFilePath")
            baselineGenerationNeeded = true
        }
    }

    // delete the old file if one exists
    if (baselineGenerationNeeded && baselineFile.exists()) {
        baselineFile.delete()
    }

    return CurrentBaseline(baselineRules, baselineGenerationNeeded)
}

/**
 * Parses the file to generate a mapping of [LintError]
 *
 * @param baselineFile the file containing the current baseline
 * @return a mapping of file names to a list of all [LintError] in that file
 */
private fun parseBaseline(baselineFile: File): Map<String, List<LintError>> {
    val baselineRules = HashMap<String, MutableList<LintError>>()
    val builderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = builderFactory.newDocumentBuilder()
    val doc = docBuilder.parse(baselineFile)
    val filesList = doc.getElementsByTagName("file")
    for (i in 0 until filesList.length) {
        val fileElement = filesList.item(i) as Element
        val fileName = fileElement.getAttribute("name")
        val baselineErrors = parseBaselineErrorsByFile(fileElement)
        baselineRules[fileName] = baselineErrors
    }
    return baselineRules
}

/**
 * Parses the errors inside each file tag in the xml
 *
 * @param element the xml "file" element
 * @return a list of [LintError] for that file
 */
private fun parseBaselineErrorsByFile(element: Element): MutableList<LintError> {
    val errors = mutableListOf<LintError>()
    val errorsList = element.getElementsByTagName("error")
    for (i in 0 until errorsList.length) {
        val errorElement = errorsList.item(i) as Element
        errors.add(LintError(
            line = errorElement.getAttribute("line").toInt(),
            col = errorElement.getAttribute("column").toInt(),
            ruleId = errorElement.getAttribute("source"),
            detail = errorElement.getAttribute("message")
        ))
    }
    return errors
}

internal class CurrentBaseline(
    val baselineRules: Map<String, List<LintError>>?,
    val baselineGenerationNeeded: Boolean
)
