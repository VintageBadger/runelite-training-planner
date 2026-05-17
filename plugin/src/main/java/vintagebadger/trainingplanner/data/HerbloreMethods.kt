package vintagebadger.trainingplanner.data

import vintagebadger.trainingplanner.models.ItemReference
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingMethod
import vintagebadger.trainingplanner.models.TrainingStep

object HerbloreMethods {
    val methods = listOf(
        TrainingMethod(
            name = "Attack potion",
            wikiPage = "Attack potion",
            totalXp = 27.5,
            skill = Skill.HERBLORE.displayName,
            maxLevel = 3,
            output = listOf(ItemReference("Attack potion(3)", 121, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Clean grimy guam leaf",
                    name = "Grimy guam leaf -> Guam leaf",
                    skill = Skill.HERBLORE.displayName,
                    level = 1,
                    xp = 2.5,
                    input = listOf(ItemReference("Grimy guam leaf", 199, 1)),
                    output = listOf(ItemReference("Guam leaf", 249, 1)),
                ),
                TrainingStep(
                    step = "Make Guam potion (unf)",
                    name = "Guam leaf + Vial of water -> Guam potion (unf)",
                    skill = Skill.HERBLORE.displayName,
                    level = 3,
                    xp = 0.0,
                    input = listOf(ItemReference("Guam leaf", 249, 1), ItemReference("Vial of water", 227, 1)),
                    output = listOf(ItemReference("Guam potion (unf)", 91, 1)),
                ),
                TrainingStep(
                    step = "Make Attack potion(3)",
                    name = "Guam potion (unf) + Eye of newt -> Attack potion(3)",
                    skill = Skill.HERBLORE.displayName,
                    level = 3,
                    xp = 25.0,
                    input = listOf(ItemReference("Guam potion (unf)", 91, 1), ItemReference("Eye of newt", 221, 1)),
                    output = listOf(ItemReference("Attack potion(3)", 121, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Strength potion",
            wikiPage = "Strength potion",
            totalXp = 55.0,
            skill = Skill.HERBLORE.displayName,
            maxLevel = 12,
            output = listOf(ItemReference("Strength potion(3)", 115, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Clean grimy tarromin",
                    name = "Grimy tarromin -> Tarromin",
                    skill = Skill.HERBLORE.displayName,
                    level = 11,
                    xp = 5.0,
                    input = listOf(ItemReference("Grimy tarromin", 203, 1)),
                    output = listOf(ItemReference("Tarromin", 253, 1)),
                ),
                TrainingStep(
                    step = "Make Tarromin potion (unf)",
                    name = "Tarromin + Vial of water -> Tarromin potion (unf)",
                    skill = Skill.HERBLORE.displayName,
                    level = 12,
                    xp = 0.0,
                    input = listOf(ItemReference("Tarromin", 253, 1), ItemReference("Vial of water", 227, 1)),
                    output = listOf(ItemReference("Tarromin potion (unf)", 95, 1)),
                ),
                TrainingStep(
                    step = "Make Strength potion(3)",
                    name = "Tarromin potion (unf) + Limpwurt root -> Strength potion(3)",
                    skill = Skill.HERBLORE.displayName,
                    level = 12,
                    xp = 50.0,
                    input = listOf(ItemReference("Tarromin potion (unf)", 95, 1), ItemReference("Limpwurt root", 225, 1)),
                    output = listOf(ItemReference("Strength potion(3)", 115, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Prayer potion",
            wikiPage = "Prayer potion",
            totalXp = 95.0,
            skill = Skill.HERBLORE.displayName,
            maxLevel = 38,
            output = listOf(ItemReference("Prayer potion(3)", 139, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Clean grimy ranarr weed",
                    name = "Grimy ranarr weed -> Ranarr weed",
                    skill = Skill.HERBLORE.displayName,
                    level = 25,
                    xp = 7.5,
                    input = listOf(ItemReference("Grimy ranarr weed", 207, 1)),
                    output = listOf(ItemReference("Ranarr weed", 257, 1)),
                ),
                TrainingStep(
                    step = "Make Ranarr potion (unf)",
                    name = "Ranarr weed + Vial of water -> Ranarr potion (unf)",
                    skill = Skill.HERBLORE.displayName,
                    level = 30,
                    xp = 0.0,
                    input = listOf(ItemReference("Ranarr weed", 257, 1), ItemReference("Vial of water", 227, 1)),
                    output = listOf(ItemReference("Ranarr potion (unf)", 99, 1)),
                ),
                TrainingStep(
                    step = "Make Prayer potion(3)",
                    name = "Ranarr potion (unf) + Snape grass -> Prayer potion(3)",
                    skill = Skill.HERBLORE.displayName,
                    level = 38,
                    xp = 87.5,
                    input = listOf(ItemReference("Ranarr potion (unf)", 99, 1), ItemReference("Snape grass", 231, 1)),
                    output = listOf(ItemReference("Prayer potion(3)", 139, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Super restore",
            wikiPage = "Super restore",
            totalXp = 154.3,
            skill = Skill.HERBLORE.displayName,
            maxLevel = 63,
            output = listOf(ItemReference("Super restore(3)", 3026, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Clean grimy snapdragon",
                    name = "Grimy snapdragon -> Snapdragon",
                    skill = Skill.HERBLORE.displayName,
                    level = 59,
                    xp = 11.8,
                    input = listOf(ItemReference("Grimy snapdragon", 3051, 1)),
                    output = listOf(ItemReference("Snapdragon", 3000, 1)),
                ),
                TrainingStep(
                    step = "Make Snapdragon potion (unf)",
                    name = "Snapdragon + Vial of water -> Snapdragon potion (unf)",
                    skill = Skill.HERBLORE.displayName,
                    level = 63,
                    xp = 0.0,
                    input = listOf(ItemReference("Snapdragon", 3000, 1), ItemReference("Vial of water", 227, 1)),
                    output = listOf(ItemReference("Snapdragon potion (unf)", 3004, 1)),
                ),
                TrainingStep(
                    step = "Make Super restore(3)",
                    name = "Snapdragon potion (unf) + Red spiders' eggs -> Super restore(3)",
                    skill = Skill.HERBLORE.displayName,
                    level = 63,
                    xp = 142.5,
                    input = listOf(ItemReference("Snapdragon potion (unf)", 3004, 1), ItemReference("Red spiders' eggs", 223, 1)),
                    output = listOf(ItemReference("Super restore(3)", 3026, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Ranging potion",
            wikiPage = "Ranging potion",
            totalXp = 376.3,
            skill = Skill.HERBLORE.displayName,
            maxLevel = 72,
            output = listOf(ItemReference("Ranging potion(3)", 169, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Clean grimy dwarf weed",
                    name = "Grimy dwarf weed -> Dwarf weed",
                    skill = Skill.HERBLORE.displayName,
                    level = 70,
                    xp = 13.8,
                    input = listOf(ItemReference("Grimy dwarf weed", 217, 1)),
                    output = listOf(ItemReference("Dwarf weed", 267, 1)),
                ),
                TrainingStep(
                    step = "Make Dwarf weed potion (unf)",
                    name = "Dwarf weed + Vial of water -> Dwarf weed potion (unf)",
                    skill = Skill.HERBLORE.displayName,
                    level = 72,
                    xp = 0.0,
                    input = listOf(ItemReference("Dwarf weed", 267, 1), ItemReference("Vial of water", 227, 1)),
                    output = listOf(ItemReference("Dwarf weed potion (unf)", 109, 1)),
                ),
                TrainingStep(
                    step = "Make Jug of water",
                    name = "Jug -> Jug of water",
                    skill = "",
                    level = 0,
                    xp = 0.0,
                    input = listOf(ItemReference("Jug", 1935, 1)),
                    output = listOf(ItemReference("Jug of water", 1937, 1)),
                ),
                TrainingStep(
                    step = "Make Zamorak's unfermented wine",
                    name = "Zamorak's grapes + Jug of water -> Zamorak's unfermented wine",
                    skill = Skill.COOKING.displayName,
                    level = 65,
                    xp = 0.0,
                    input = listOf(ItemReference("Zamorak's grapes", 20749, 1), ItemReference("Jug of water", 1937, 1)),
                    output = listOf(ItemReference("Zamorak's unfermented wine", 20752, 1)),
                ),
                TrainingStep(
                    step = "Make Wine of zamorak",
                    name = "Zamorak's unfermented wine -> Wine of zamorak",
                    skill = Skill.COOKING.displayName,
                    level = 65,
                    xp = 200.0,
                    input = listOf(ItemReference("Zamorak's unfermented wine", 20752, 1)),
                    output = listOf(ItemReference("Wine of zamorak", 245, 1)),
                ),
                TrainingStep(
                    step = "Make Ranging potion(3)",
                    name = "Dwarf weed potion (unf) + Wine of zamorak -> Ranging potion(3)",
                    skill = Skill.HERBLORE.displayName,
                    level = 72,
                    xp = 162.5,
                    input = listOf(ItemReference("Dwarf weed potion (unf)", 109, 1), ItemReference("Wine of zamorak", 245, 1)),
                    output = listOf(ItemReference("Ranging potion(3)", 169, 1)),
                )
            )
        )
    )
}
