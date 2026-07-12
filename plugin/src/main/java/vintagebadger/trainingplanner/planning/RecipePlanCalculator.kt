package vintagebadger.trainingplanner.planning

import vintagebadger.trainingplanner.data.RecipeRequirementEdge
import vintagebadger.trainingplanner.data.ResolvedRecipeGraph
import vintagebadger.trainingplanner.data.ResolvedRecipeNode

enum class SolverKind {
    DIRECT_BREAKPOINT,
    MONOTONIC_SEARCH,
}

data class PlanNodeResult(
    val itemId: Int,
    val itemName: String,
    val craftable: Boolean,
    val demandUnits: Long,
    val ownedUsed: Long,
    val craftActions: Long,
    val outputQuantity: Long,
    val producedUnits: Long,
    val surplusUnits: Long,
    val xpPerActionTenths: Long,
    val xpGainedTenths: Long,
    val unitsToAcquire: Long,
)

data class PlanResult(
    val solverKind: SolverKind,
    val rootItemId: Int,
    val rootActions: Long,
    val targetXpTenths: Long,
    val totalXpTenths: Long,
    val extraXpTenths: Long,
    val nodes: List<PlanNodeResult>,
)

class PlanCalculationException(message: String) : IllegalArgumentException(message)

class RecipePlanCalculator {
    fun solve(
        graph: ResolvedRecipeGraph,
        targetXpTenths: Long,
        ownedQuantities: Map<Int, Long> = emptyMap(),
    ): PlanResult {
        val target = targetXpTenths.coerceAtLeast(0L)
        val usesBatchDependencies = graph.nodes.values.any {
            it.itemId != graph.rootItemId && it.craftable && it.outputQuantity != 1L
        }
        val solverKind = if (usesBatchDependencies) {
            SolverKind.MONOTONIC_SEARCH
        } else {
            SolverKind.DIRECT_BREAKPOINT
        }
        if (target == 0L) return evaluate(graph, target, ownedQuantities, 0L, solverKind)
        if (graph.nodes.values.none { it.craftable && it.skillXpTenths > 0L }) {
            throw PlanCalculationException("Selected recipe graph does not grant target-skill XP")
        }

        val rootActions = if (usesBatchDependencies) {
            solveByMonotonicSearch(graph, target, ownedQuantities)
        } else {
            solveByBreakpoints(graph, target, ownedQuantities)
        }
        val result = evaluate(graph, target, ownedQuantities, rootActions, solverKind)
        verifyMinimum(graph, result, ownedQuantities)
        return result
    }

    fun evaluate(
        graph: ResolvedRecipeGraph,
        targetXpTenths: Long,
        ownedQuantities: Map<Int, Long>,
        rootActions: Long,
        solverKind: SolverKind = SolverKind.MONOTONIC_SEARCH,
    ): PlanResult {
        require(rootActions >= 0L) { "Root actions cannot be negative" }
        val incomingDemand = mutableMapOf<Int, Long>()
        val results = mutableListOf<PlanNodeResult>()
        val outgoing = graph.edges.groupBy(RecipeRequirementEdge::parentItemId)
        var totalXp = 0L

        graph.topologicalOrder.forEach { itemId ->
            val node = graph.nodes.getValue(itemId)
            val isRoot = itemId == graph.rootItemId
            val demand = if (isRoot) {
                multiply(rootActions, node.outputQuantity)
            } else {
                incomingDemand[itemId] ?: 0L
            }
            val owned = if (isRoot) 0L else (ownedQuantities[itemId] ?: 0L).coerceAtLeast(0L)
            val ownedUsed = minOf(demand, owned)
            val shortage = demand - ownedUsed
            val craftActions = when {
                isRoot -> rootActions
                node.craftable -> ceilDiv(shortage, node.outputQuantity)
                else -> 0L
            }
            val produced = if (node.craftable) multiply(craftActions, node.outputQuantity) else 0L
            val surplus = if (node.craftable) maxOf(0L, produced - shortage) else 0L
            val unitsToAcquire = if (node.craftable) 0L else shortage
            val xpGained = multiply(craftActions, node.skillXpTenths)
            totalXp = add(totalXp, xpGained)
            outgoing[itemId].orEmpty().forEach { edge ->
                val childDemand = multiply(craftActions, edge.quantityPerAction)
                incomingDemand[edge.ingredientItemId] = add(
                    incomingDemand[edge.ingredientItemId] ?: 0L,
                    childDemand,
                )
            }

            results += node.toResult(
                demand = demand,
                ownedUsed = ownedUsed,
                craftActions = craftActions,
                produced = produced,
                surplus = surplus,
                xpGained = xpGained,
                unitsToAcquire = unitsToAcquire,
            )
        }

        val target = targetXpTenths.coerceAtLeast(0L)
        return PlanResult(
            solverKind = solverKind,
            rootItemId = graph.rootItemId,
            rootActions = rootActions,
            targetXpTenths = target,
            totalXpTenths = totalXp,
            extraXpTenths = maxOf(0L, totalXp - target),
            nodes = results,
        )
    }

    private fun solveByMonotonicSearch(
        graph: ResolvedRecipeGraph,
        target: Long,
        owned: Map<Int, Long>,
    ): Long {
        fun xpAt(actions: Long): Long = evaluate(
            graph,
            target,
            owned,
            actions,
            SolverKind.MONOTONIC_SEARCH,
        ).totalXpTenths

        var high = 1L
        while (xpAt(high) < target) {
            if (high > Long.MAX_VALUE / 2L) {
                throw PlanCalculationException("Required action count exceeds supported range")
            }
            high *= 2L
        }
        var low = 0L
        while (low + 1L < high) {
            val middle = low + (high - low) / 2L
            if (xpAt(middle) >= target) high = middle else low = middle
        }
        return high
    }

    private fun solveByBreakpoints(
        graph: ResolvedRecipeGraph,
        target: Long,
        owned: Map<Int, Long>,
    ): Long {
        val incoming = graph.edges.groupBy(RecipeRequirementEdge::ingredientItemId)
        var segmentStart = 0L

        repeat(graph.nodes.size + 1) {
            val expressions = mutableMapOf<Int, Affine>()
            var xpExpression = Affine.ZERO

            graph.topologicalOrder.forEach { itemId ->
                val node = graph.nodes.getValue(itemId)
                val expression = if (itemId == graph.rootItemId) {
                    Affine(1L, 0L)
                } else if (!node.craftable) {
                    Affine.ZERO
                } else {
                    val demand = incoming[itemId].orEmpty().fold(Affine.ZERO) { total, edge ->
                        total + expressions.getValue(edge.parentItemId) * edge.quantityPerAction
                    }
                    val ownedUnits = (owned[itemId] ?: 0L).coerceAtLeast(0L)
                    if (demand.valueAt(segmentStart) > ownedUnits) {
                        Affine(demand.slope, subtract(demand.offset, ownedUnits))
                    } else {
                        Affine.ZERO
                    }
                }
                expressions[itemId] = expression
                xpExpression += expression * node.skillXpTenths
            }

            val nextBreakpoint = graph.topologicalOrder.asSequence()
                .filter { it != graph.rootItemId }
                .map { graph.nodes.getValue(it) }
                .filter { it.craftable && expressions.getValue(it.itemId) == Affine.ZERO }
                .mapNotNull { node ->
                    val demand = incoming[node.itemId].orEmpty().fold(Affine.ZERO) { total, edge ->
                        total + expressions.getValue(edge.parentItemId) * edge.quantityPerAction
                    }
                    if (demand.slope <= 0L) {
                        null
                    } else {
                        val ownedUnits = (owned[node.itemId] ?: 0L).coerceAtLeast(0L)
                        Math.floorDiv(subtract(ownedUnits, demand.offset), demand.slope) + 1L
                    }
                }
                .filter { it > segmentStart }
                .minOrNull()

            if (xpExpression.slope > 0L) {
                val needed = subtract(target, xpExpression.offset)
                val candidate = maxOf(segmentStart, ceilDiv(maxOf(0L, needed), xpExpression.slope))
                if (nextBreakpoint == null || candidate < nextBreakpoint) return candidate
            }
            segmentStart = nextBreakpoint
                ?: throw PlanCalculationException("Selected recipe graph cannot reach the XP target")
        }
        throw PlanCalculationException("Could not resolve recipe breakpoints")
    }

    private fun verifyMinimum(
        graph: ResolvedRecipeGraph,
        result: PlanResult,
        owned: Map<Int, Long>,
    ) {
        if (result.totalXpTenths < result.targetXpTenths) {
            throw PlanCalculationException("Calculated plan does not reach the XP target")
        }
        if (result.rootActions > 0L) {
            val previous = evaluate(
                graph,
                result.targetXpTenths,
                owned,
                result.rootActions - 1L,
                result.solverKind,
            )
            if (previous.totalXpTenths >= result.targetXpTenths) {
                throw PlanCalculationException("Calculated plan is not the minimum action count")
            }
        }
    }

    private fun ResolvedRecipeNode.toResult(
        demand: Long,
        ownedUsed: Long,
        craftActions: Long,
        produced: Long,
        surplus: Long,
        xpGained: Long,
        unitsToAcquire: Long,
    ) = PlanNodeResult(
        itemId = itemId,
        itemName = itemName,
        craftable = craftable,
        demandUnits = demand,
        ownedUsed = ownedUsed,
        craftActions = craftActions,
        outputQuantity = outputQuantity,
        producedUnits = produced,
        surplusUnits = surplus,
        xpPerActionTenths = skillXpTenths,
        xpGainedTenths = xpGained,
        unitsToAcquire = unitsToAcquire,
    )

    private data class Affine(val slope: Long, val offset: Long) {
        fun valueAt(value: Long): Long = add(multiply(slope, value), offset)
        operator fun plus(other: Affine) = Affine(add(slope, other.slope), add(offset, other.offset))
        operator fun times(value: Long) = Affine(multiply(slope, value), multiply(offset, value))

        companion object {
            val ZERO = Affine(0L, 0L)
        }
    }

    private companion object {
        fun ceilDiv(value: Long, divisor: Long): Long {
            require(value >= 0L) { "Dividend cannot be negative" }
            require(divisor > 0L) { "Divisor must be positive" }
            return if (value == 0L) 0L else 1L + (value - 1L) / divisor
        }

        fun add(left: Long, right: Long): Long = try {
            Math.addExact(left, right)
        } catch (_: ArithmeticException) {
            throw PlanCalculationException("Plan quantity exceeds supported range")
        }

        fun subtract(left: Long, right: Long): Long = try {
            Math.subtractExact(left, right)
        } catch (_: ArithmeticException) {
            throw PlanCalculationException("Plan quantity exceeds supported range")
        }

        fun multiply(left: Long, right: Long): Long = try {
            Math.multiplyExact(left, right)
        } catch (_: ArithmeticException) {
            throw PlanCalculationException("Plan quantity exceeds supported range")
        }
    }
}
