package vintagebadger.trainingplanner.data

import vintagebadger.trainingplanner.models.ItemReference
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingMethod
import vintagebadger.trainingplanner.models.TrainingStep

object CraftingMethods {
    val methods = listOf(
        TrainingMethod(
            name = "Opal ring",
            wikiPage = "Opal ring",
            totalXp = 38.7,
            skill = Skill.CRAFTING.displayName,
            maxLevel = 20,
            output = listOf(ItemReference("Opal ring", 21081, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Opal",
                    name = "Uncut opal -> Opal",
                    skill = Skill.CRAFTING.displayName,
                    level = 1,
                    xp = 15.0,
                    input = listOf(ItemReference("Uncut opal", 1625, 1)),
                    output = listOf(ItemReference("Opal", 1609, 1)),
                ),
                TrainingStep(
                    step = "Make Silver bar",
                    name = "Silver ore -> Silver bar",
                    skill = Skill.SMITHING.displayName,
                    level = 20,
                    xp = 13.7,
                    input = listOf(ItemReference("Silver ore", 442, 1)),
                    output = listOf(ItemReference("Silver bar", 2355, 1)),
                ),
                TrainingStep(
                    step = "Make Opal ring",
                    name = "Opal + Silver bar -> Opal ring",
                    skill = Skill.CRAFTING.displayName,
                    level = 1,
                    xp = 10.0,
                    input = listOf(ItemReference("Opal", 1609, 1), ItemReference("Silver bar", 2355, 1)),
                    output = listOf(ItemReference("Opal ring", 21081, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Sapphire necklace",
            wikiPage = "Sapphire necklace",
            totalXp = 127.5,
            skill = Skill.CRAFTING.displayName,
            maxLevel = 40,
            output = listOf(ItemReference("Sapphire necklace", 1656, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Sapphire",
                    name = "Uncut sapphire -> Sapphire",
                    skill = Skill.CRAFTING.displayName,
                    level = 20,
                    xp = 50.0,
                    input = listOf(ItemReference("Uncut sapphire", 1623, 1)),
                    output = listOf(ItemReference("Sapphire", 1607, 1)),
                ),
                TrainingStep(
                    step = "Make Gold bar",
                    name = "Gold ore -> Gold bar",
                    skill = Skill.SMITHING.displayName,
                    level = 40,
                    xp = 22.5,
                    input = listOf(ItemReference("Gold ore", 444, 1)),
                    output = listOf(ItemReference("Gold bar", 2357, 1)),
                ),
                TrainingStep(
                    step = "Make Sapphire necklace",
                    name = "Sapphire + Gold bar -> Sapphire necklace",
                    skill = Skill.CRAFTING.displayName,
                    level = 22,
                    xp = 55.0,
                    input = listOf(ItemReference("Sapphire", 1607, 1), ItemReference("Gold bar", 2357, 1)),
                    output = listOf(ItemReference("Sapphire necklace", 1656, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Emerald ring",
            wikiPage = "Emerald ring",
            totalXp = 145.0,
            skill = Skill.CRAFTING.displayName,
            maxLevel = 40,
            output = listOf(ItemReference("Emerald ring", 1639, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Emerald",
                    name = "Uncut emerald -> Emerald",
                    skill = Skill.CRAFTING.displayName,
                    level = 27,
                    xp = 67.5,
                    input = listOf(ItemReference("Uncut emerald", 1621, 1)),
                    output = listOf(ItemReference("Emerald", 1605, 1)),
                ),
                TrainingStep(
                    step = "Make Gold bar",
                    name = "Gold ore -> Gold bar",
                    skill = Skill.SMITHING.displayName,
                    level = 40,
                    xp = 22.5,
                    input = listOf(ItemReference("Gold ore", 444, 1)),
                    output = listOf(ItemReference("Gold bar", 2357, 1)),
                ),
                TrainingStep(
                    step = "Make Emerald ring",
                    name = "Emerald + Gold bar -> Emerald ring",
                    skill = Skill.CRAFTING.displayName,
                    level = 27,
                    xp = 55.0,
                    input = listOf(ItemReference("Emerald", 1605, 1), ItemReference("Gold bar", 2357, 1)),
                    output = listOf(ItemReference("Emerald ring", 1639, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Gold amulet",
            wikiPage = "Gold amulet",
            totalXp = 59.0,
            skill = Skill.CRAFTING.displayName,
            maxLevel = 40,
            output = listOf(ItemReference("Gold amulet", 1692, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Gold bar",
                    name = "Gold ore -> Gold bar",
                    skill = Skill.SMITHING.displayName,
                    level = 40,
                    xp = 22.5,
                    input = listOf(ItemReference("Gold ore", 444, 1)),
                    output = listOf(ItemReference("Gold bar", 2357, 1)),
                ),
                TrainingStep(
                    step = "Make Gold amulet (u)",
                    name = "Gold bar -> Gold amulet (u)",
                    skill = Skill.CRAFTING.displayName,
                    level = 8,
                    xp = 30.0,
                    input = listOf(ItemReference("Gold bar", 2357, 1)),
                    output = listOf(ItemReference("Gold amulet (u)", 1673, 1)),
                ),
                TrainingStep(
                    step = "Make Ball of wool",
                    name = "Wool -> Ball of wool",
                    skill = Skill.CRAFTING.displayName,
                    level = 1,
                    xp = 2.5,
                    input = listOf(ItemReference("Wool", 1737, 1)),
                    output = listOf(ItemReference("Ball of wool", 1759, 1)),
                ),
                TrainingStep(
                    step = "Make Gold amulet",
                    name = "Gold amulet (u) + Ball of wool -> Gold amulet",
                    skill = Skill.CRAFTING.displayName,
                    level = 1,
                    xp = 4.0,
                    input = listOf(ItemReference("Gold amulet (u)", 1673, 1), ItemReference("Ball of wool", 1759, 1)),
                    output = listOf(ItemReference("Gold amulet", 1692, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Unpowered orb",
            wikiPage = "Unpowered orb",
            totalXp = 52.5,
            skill = Skill.CRAFTING.displayName,
            maxLevel = 46,
            output = listOf(ItemReference("Unpowered orb", 567, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Unpowered orb",
                    name = "Molten glass -> Unpowered orb",
                    skill = Skill.CRAFTING.displayName,
                    level = 46,
                    xp = 52.5,
                    input = listOf(ItemReference("Molten glass", 1775, 1)),
                    output = listOf(ItemReference("Unpowered orb", 567, 1)),
                )
            )
        )
    )
}
