package vintagebadger.trainingplanner.data

import vintagebadger.trainingplanner.models.ItemReference
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingMethod
import vintagebadger.trainingplanner.models.TrainingStep

object SmithingMethods {
    val methods = listOf(
        TrainingMethod(
            name = "Bronze dagger",
            wikiPage = "Bronze dagger",
            totalXp = 18.7,
            skill = Skill.SMITHING.displayName,
            maxLevel = 1,
            output = listOf(ItemReference("Bronze dagger", 1205, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Bronze bar",
                    name = "Copper ore + Tin ore -> Bronze bar",
                    skill = Skill.SMITHING.displayName,
                    level = 1,
                    xp = 6.2,
                    input = listOf(ItemReference("Copper ore", 436, 1), ItemReference("Tin ore", 438, 1)),
                    output = listOf(ItemReference("Bronze bar", 2349, 1)),
                ),
                TrainingStep(
                    step = "Make Bronze dagger",
                    name = "Bronze bar -> Bronze dagger",
                    skill = Skill.SMITHING.displayName,
                    level = 1,
                    xp = 12.5,
                    input = listOf(ItemReference("Bronze bar", 2349, 1)),
                    output = listOf(ItemReference("Bronze dagger", 1205, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Iron dagger",
            wikiPage = "Iron dagger",
            totalXp = 37.5,
            skill = Skill.SMITHING.displayName,
            maxLevel = 15,
            output = listOf(ItemReference("Iron dagger", 1203, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Iron bar",
                    name = "Iron ore -> Iron bar",
                    skill = Skill.SMITHING.displayName,
                    level = 15,
                    xp = 12.5,
                    input = listOf(ItemReference("Iron ore", 440, 1)),
                    output = listOf(ItemReference("Iron bar", 2351, 1)),
                ),
                TrainingStep(
                    step = "Make Iron dagger",
                    name = "Iron bar -> Iron dagger",
                    skill = Skill.SMITHING.displayName,
                    level = 15,
                    xp = 25.0,
                    input = listOf(ItemReference("Iron bar", 2351, 1)),
                    output = listOf(ItemReference("Iron dagger", 1203, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Steel platebody",
            wikiPage = "Steel platebody",
            totalXp = 205.0,
            skill = Skill.SMITHING.displayName,
            maxLevel = 48,
            output = listOf(ItemReference("Steel platebody", 1119, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Steel bar",
                    name = "Iron ore + Coal x2 -> Steel bar",
                    skill = Skill.SMITHING.displayName,
                    level = 30,
                    xp = 17.5,
                    input = listOf(ItemReference("Iron ore", 440, 1), ItemReference("Coal", 453, 2)),
                    output = listOf(ItemReference("Steel bar", 2353, 1)),
                ),
                TrainingStep(
                    step = "Make Steel platebody",
                    name = "Steel bar x5 -> Steel platebody",
                    skill = Skill.SMITHING.displayName,
                    level = 48,
                    xp = 187.5,
                    input = listOf(ItemReference("Steel bar", 2353, 5)),
                    output = listOf(ItemReference("Steel platebody", 1119, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Mithril scimitar",
            wikiPage = "Mithril scimitar",
            totalXp = 130.0,
            skill = Skill.SMITHING.displayName,
            maxLevel = 55,
            output = listOf(ItemReference("Mithril scimitar", 1329, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Mithril bar",
                    name = "Mithril ore + Coal x4 -> Mithril bar",
                    skill = Skill.SMITHING.displayName,
                    level = 50,
                    xp = 30.0,
                    input = listOf(ItemReference("Mithril ore", 447, 1), ItemReference("Coal", 453, 4)),
                    output = listOf(ItemReference("Mithril bar", 2359, 1)),
                ),
                TrainingStep(
                    step = "Make Mithril scimitar",
                    name = "Mithril bar x2 -> Mithril scimitar",
                    skill = Skill.SMITHING.displayName,
                    level = 55,
                    xp = 100.0,
                    input = listOf(ItemReference("Mithril bar", 2359, 2)),
                    output = listOf(ItemReference("Mithril scimitar", 1329, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Adamant platebody",
            wikiPage = "Adamant platebody",
            totalXp = 350.0,
            skill = Skill.SMITHING.displayName,
            maxLevel = 88,
            output = listOf(ItemReference("Adamant platebody", 1123, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Adamantite bar",
                    name = "Adamantite ore + Coal x6 -> Adamantite bar",
                    skill = Skill.SMITHING.displayName,
                    level = 70,
                    xp = 37.5,
                    input = listOf(ItemReference("Adamantite ore", 449, 1), ItemReference("Coal", 453, 6)),
                    output = listOf(ItemReference("Adamantite bar", 2361, 1)),
                ),
                TrainingStep(
                    step = "Make Adamant platebody",
                    name = "Adamantite bar x5 -> Adamant platebody",
                    skill = Skill.SMITHING.displayName,
                    level = 88,
                    xp = 312.5,
                    input = listOf(ItemReference("Adamantite bar", 2361, 5)),
                    output = listOf(ItemReference("Adamant platebody", 1123, 1)),
                )
            )
        )
    )
}
