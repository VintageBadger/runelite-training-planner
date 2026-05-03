package vintagebadger.trainingplanner.wiki

data class ItemRef(val name: String, val id: Int, val quantity: Int)

fun itemReference(name: String, id: Int, quantity: Int) = ItemRef(normalizeItemName(name), id, quantity)

object HerbCleaning {
    data class Herb(
        val cleanName: String,
        val grimyName: String,
        val cleanId: Int,
        val grimyId: Int,
        val level: Int,
        val xp: Double
    )

    val known = listOf(
        Herb("Guam leaf", "Grimy guam leaf", 249, 199, 1, 2.5),
        Herb("Marrentill", "Grimy marrentill", 251, 201, 5, 3.8),
        Herb("Tarromin", "Grimy tarromin", 253, 203, 11, 5.0),
        Herb("Harralander", "Grimy harralander", 255, 205, 20, 6.3),
        Herb("Ranarr weed", "Grimy ranarr weed", 257, 207, 25, 7.5),
        Herb("Toadflax", "Grimy toadflax", 2998, 3049, 30, 8.0),
        Herb("Irit leaf", "Grimy irit leaf", 259, 209, 40, 8.8),
        Herb("Avantoe", "Grimy avantoe", 261, 211, 48, 10.0),
        Herb("Kwuarm", "Grimy kwuarm", 263, 213, 54, 11.3),
        Herb("Snapdragon", "Grimy snapdragon", 3000, 3051, 59, 11.8),
        Herb("Cadantine", "Grimy cadantine", 265, 215, 65, 12.5),
        Herb("Lantadyme", "Grimy lantadyme", 2481, 2485, 67, 13.1),
        Herb("Dwarf weed", "Grimy dwarf weed", 267, 217, 70, 13.8),
        Herb("Torstol", "Grimy torstol", 269, 219, 75, 15.0),
    ).associateBy { normalizeIdentity(it.cleanName) }
}
