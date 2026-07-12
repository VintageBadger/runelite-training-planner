package vintagebadger.trainingplanner.data

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.wiki.FlatRecipe
import vintagebadger.trainingplanner.wiki.OutputItemRecipes
import vintagebadger.trainingplanner.wiki.RecipeGraph
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.ArrayDeque

data class ResolvedRecipeNode(
    val itemId: Int,
    val itemName: String,
    val craftable: Boolean,
    val methodKey: String = "",
    val methodName: String = "",
    val outputQuantity: Long = 1,
    val skillXpTenths: Long = 0,
    val levelRequirement: Int = 0,
)

data class RecipeRequirementEdge(
    val parentItemId: Int,
    val ingredientItemId: Int,
    val quantityPerAction: Long,
)

data class ResolvedRecipeGraph(
    val rootItemId: Int,
    val nodes: Map<Int, ResolvedRecipeNode>,
    val edges: List<RecipeRequirementEdge>,
    val topologicalOrder: List<Int>,
)

class RecipeGraphResolutionException(message: String) : IllegalArgumentException(message)

class TrainingRecipeRepository(
    private val gson: Gson,
    private val graphOverride: RecipeGraph? = null,
) {
    private val log = LoggerFactory.getLogger(TrainingRecipeRepository::class.java)

    private val graph: RecipeGraph by lazy {
        graphOverride?.let { return@lazy it }
        val stream = TrainingRecipeRepository::class.java.getResourceAsStream(RESOURCE_PATH)
        if (stream == null) {
            log.debug("Training recipes resource not found: {}", RESOURCE_PATH)
            RecipeGraph()
        } else {
            stream.use {
                InputStreamReader(it, Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, RecipeGraph::class.java) ?: RecipeGraph()
                }
            }
        }
    }

    private val recipesById: Map<Int, OutputItemRecipes> by lazy {
        graph.recipes.associateBy { it.id }
    }

    fun methodsFor(skill: Skill, maxLevel: Int): List<OutputItemRecipes> {
        return graph.recipes
            .filter { output ->
                val hasSelectedSkill = output.methods.firstOrNull()
                    ?.skills
                    ?.any { it.skill.equals(skill.displayName, ignoreCase = true) && it.level <= maxLevel }
                hasSelectedSkill == true
            }
    }

    fun recipeById(itemId: Int): OutputItemRecipes? = recipesById[itemId]

    fun resolveGraph(
        output: OutputItemRecipes,
        skill: Skill,
        selectedMethodKeys: Map<Int, String> = emptyMap(),
    ): ResolvedRecipeGraph {
        val nodes = linkedMapOf<Int, ResolvedRecipeNode>()
        val edgeQuantities = linkedMapOf<Pair<Int, Int>, Long>()
        val visiting = mutableListOf<Int>()
        val visited = mutableSetOf<Int>()

        fun resolve(itemId: Int, fallbackName: String) {
            val cycleAt = visiting.indexOf(itemId)
            if (cycleAt >= 0) {
                val cycle = (visiting.subList(cycleAt, visiting.size) + itemId)
                    .joinToString(" -> ") { recipesById[it]?.name ?: it.toString() }
                throw RecipeGraphResolutionException("Recipe cycle detected: $cycle")
            }
            if (itemId in visited) return

            val itemRecipes = recipesById[itemId]
                ?: if (itemId == output.id) output else null
            val selected = itemRecipes?.let { selectMethod(it, selectedMethodKeys[itemId]) }
            if (itemRecipes == null || selected == null) {
                nodes[itemId] = ResolvedRecipeNode(
                    itemId = itemId,
                    itemName = itemRecipes?.name ?: fallbackName,
                    craftable = false,
                )
                visited += itemId
                return
            }

            val (methodIndex, method) = selected
            val skillRequirement = method.skills.firstOrNull {
                it.skill.equals(skill.displayName, ignoreCase = true)
            }
            nodes[itemId] = ResolvedRecipeNode(
                itemId = itemId,
                itemName = itemRecipes.name,
                craftable = true,
                methodKey = effectiveMethodKey(itemRecipes.id, methodIndex, method),
                methodName = method.method,
                outputQuantity = method.outputQuantity.takeIf { it > 0 }?.toLong() ?: 1L,
                skillXpTenths = skillRequirement?.xp?.let(::xpToTenths) ?: 0L,
                levelRequirement = skillRequirement?.level ?: 0,
            )

            visiting += itemId
            method.requires.forEach { ingredient ->
                val edgeKey = itemId to ingredient.id
                edgeQuantities[edgeKey] = Math.addExact(
                    edgeQuantities[edgeKey] ?: 0L,
                    ingredient.quantity.toLong(),
                )
                resolve(ingredient.id, ingredient.name)
            }
            visiting.removeAt(visiting.lastIndex)
            visited += itemId
        }

        resolve(output.id, output.name)
        val edges = edgeQuantities.map { (ids, quantity) ->
            RecipeRequirementEdge(ids.first, ids.second, quantity)
        }
        return ResolvedRecipeGraph(
            rootItemId = output.id,
            nodes = nodes,
            edges = edges,
            topologicalOrder = topologicalOrder(nodes.keys, edges),
        )
    }

    private fun selectMethod(output: OutputItemRecipes, selectedKey: String?): IndexedValue<FlatRecipe>? {
        val methods = output.methods.withIndex()
        if (selectedKey == null) return methods.firstOrNull()
        return methods.firstOrNull { (index, method) ->
            effectiveMethodKey(output.id, index, method) == selectedKey
        } ?: throw RecipeGraphResolutionException(
            "Selected recipe method '$selectedKey' was not found for ${output.name}",
        )
    }

    private fun effectiveMethodKey(outputId: Int, methodIndex: Int, method: FlatRecipe): String {
        return method.methodKey?.takeIf { it.isNotBlank() }
            ?: "legacy-$outputId-$methodIndex"
    }

    private fun xpToTenths(xp: Double): Long {
        return BigDecimal.valueOf(xp)
            .movePointRight(1)
            .setScale(0, RoundingMode.UNNECESSARY)
            .longValueExact()
    }

    private fun topologicalOrder(
        itemIds: Set<Int>,
        edges: List<RecipeRequirementEdge>,
    ): List<Int> {
        val incoming = itemIds.associateWith { 0 }.toMutableMap()
        val outgoing = edges.groupBy(RecipeRequirementEdge::parentItemId)
        edges.forEach { edge ->
            incoming[edge.ingredientItemId] = (incoming[edge.ingredientItemId] ?: 0) + 1
        }

        val ready = ArrayDeque(itemIds.filter { incoming[it] == 0 })
        val result = mutableListOf<Int>()
        while (ready.isNotEmpty()) {
            val itemId = ready.removeFirst()
            result += itemId
            outgoing[itemId].orEmpty().forEach { edge ->
                val remaining = (incoming[edge.ingredientItemId] ?: 0) - 1
                incoming[edge.ingredientItemId] = remaining
                if (remaining == 0) ready.addLast(edge.ingredientItemId)
            }
        }
        if (result.size != itemIds.size) {
            throw RecipeGraphResolutionException("Selected recipe methods contain a cycle")
        }
        return result
    }

    companion object {
        private const val RESOURCE_PATH = "/vintagebadger/trainingplanner/training-recipes.json"
    }
}
