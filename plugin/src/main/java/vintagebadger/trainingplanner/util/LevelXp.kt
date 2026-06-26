package vintagebadger.trainingplanner.util

import net.runelite.api.Experience

/**
 * Total XP required to advance from `startLevel` to `endLevel`.
 * Levels are clamped to the range [1, 126].
 * Returns 0 when endLevel <= startLevel
 */
fun totalXpBetween(startLevel: Int, endLevel: Int): Int {
    val start = startLevel.coerceIn(1, 126)
    val end = endLevel.coerceIn(1, 126)
    return maxOf(0, Experience.getXpForLevel(end) - Experience.getXpForLevel(start))
}