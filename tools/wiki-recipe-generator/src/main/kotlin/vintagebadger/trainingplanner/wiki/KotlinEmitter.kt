package vintagebadger.trainingplanner.wiki

import vintagebadger.trainingplanner.models.Skill
import java.nio.file.Files
import java.nio.file.Path

class KotlinEmitter {
    fun write(output: Path, methods: List<ResolvedMethod>) {
        Files.createDirectories(output.parent)
        Files.writeString(output, render(output.fileName.toString().removeSuffix(".kt"), methods))
    }

    private fun render(objectName: String, methods: List<ResolvedMethod>): String {
        return buildString {
            appendLine("package vintagebadger.trainingplanner.data")
            appendLine()
            appendLine("import vintagebadger.trainingplanner.models.ItemReference")
            appendLine("import vintagebadger.trainingplanner.models.Skill")
            appendLine("import vintagebadger.trainingplanner.models.TrainingMethod")
            appendLine("import vintagebadger.trainingplanner.models.TrainingStep")
            appendLine()
            appendLine("object ${objectName.toKotlinIdentifier()} {")
            appendLine("    val methods = listOf(")
            methods.forEachIndexed { index, method ->
                method.warnings.forEach { appendLine("        // REVIEW: $it") }
                append(renderMethod(method, "        "))
                if (index != methods.lastIndex) appendLine(",") else appendLine()
            }
            appendLine("    )")
            appendLine("}")
        }
    }

    private fun renderMethod(method: ResolvedMethod, indent: String): String {
        return buildString {
            appendLine("${indent}TrainingMethod(")
            appendLine("$indent    name = ${method.name.kotlinString()},")
            appendLine("$indent    wikiPage = ${method.wikiPage.kotlinString()},")
            appendLine("$indent    totalXp = ${method.totalXp.doubleLiteral()},")
            appendLine("$indent    skill = ${skillExpression(method.skill)},")
            appendLine("$indent    maxLevel = ${method.maxLevel},")
            appendLine("$indent    output = ${renderItems(method.output)},")
            appendLine("$indent    steps = listOf(")
            method.steps.forEachIndexed { index, step ->
                append(renderStep(step, "$indent        "))
                if (index != method.steps.lastIndex) appendLine(",") else appendLine()
            }
            appendLine("$indent    )")
            append("$indent)")
        }
    }

    private fun renderStep(step: ResolvedStep, indent: String): String {
        return buildString {
            appendLine("${indent}TrainingStep(")
            appendLine("$indent    step = ${step.step.kotlinString()},")
            appendLine("$indent    name = ${step.name.kotlinString()},")
            appendLine("$indent    skill = ${skillExpression(step.skill)},")
            appendLine("$indent    level = ${step.level},")
            appendLine("$indent    xp = ${step.xp.doubleLiteral()},")
            appendLine("$indent    input = ${renderItems(step.input)},")
            appendLine("$indent    output = ${renderItems(step.output)},")
            append("$indent)")
        }
    }

    private fun renderItems(items: List<ItemRef>): String {
        if (items.isEmpty()) return "emptyList()"
        return "listOf(${items.joinToString(", ") { "ItemReference(${it.name.kotlinString()}, ${it.id}, ${it.quantity})" }})"
    }

    private fun skillExpression(skill: String): String {
        val enumValue = Skill.entries.firstOrNull { it.displayName.equals(skill, ignoreCase = true) }
        return enumValue?.let { "Skill.${it.name}.displayName" } ?: skill.kotlinString()
    }

    private fun String.toKotlinIdentifier(): String {
        val cleaned = replace(Regex("[^A-Za-z0-9_]"), "_")
        return if (cleaned.firstOrNull()?.isJavaIdentifierStart() == true) cleaned else "_$cleaned"
    }

    private fun String.kotlinString(): String {
        return "\"" + flatMap { char ->
            when (char) {
                '\\' -> "\\\\".toList()
                '"' -> "\\\"".toList()
                '\n' -> "\\n".toList()
                '\r' -> "\\r".toList()
                '\t' -> "\\t".toList()
                else -> listOf(char)
            }
        }.joinToString("") + "\""
    }

    private fun Double.doubleLiteral(): String {
        val text = toString()
        return if (text.contains('.')) text else "$text.0"
    }
}
