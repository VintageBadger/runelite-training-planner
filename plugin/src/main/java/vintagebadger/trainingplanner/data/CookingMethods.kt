package vintagebadger.trainingplanner.data

import vintagebadger.trainingplanner.models.ItemReference
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingMethod
import vintagebadger.trainingplanner.models.TrainingStep

object CookingMethods {
    val methods = listOf(
        TrainingMethod(
            name = "Bread",
            wikiPage = "Bread",
            totalXp = 52.6,
            skill = Skill.COOKING.displayName,
            maxLevel = 1,
            output = listOf(ItemReference("Bread", 2309, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Bread dough",
                    name = "Pot of flour + Bucket of water -> Bread dough",
                    skill = "",
                    level = 0,
                    xp = 0.0,
                    input = listOf(ItemReference("Pot of flour", 1933, 1), ItemReference("Bucket of water", 1929, 1)),
                    output = listOf(ItemReference("Bread dough", 2307, 1)),
                ),
                TrainingStep(
                    step = "Make Bread",
                    name = "Bread dough -> Bread",
                    skill = Skill.COOKING.displayName,
                    level = 1,
                    xp = 40.0,
                    input = listOf(ItemReference("Bread dough", 2307, 1)),
                    output = listOf(ItemReference("Bread", 2309, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Cooked chicken",
            wikiPage = "Cooked chicken",
            totalXp = 30.0,
            skill = Skill.COOKING.displayName,
            maxLevel = 1,
            output = listOf(ItemReference("Cooked chicken", 2140, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Cooked chicken",
                    name = "Raw chicken -> Cooked chicken",
                    skill = Skill.COOKING.displayName,
                    level = 1,
                    xp = 30.0,
                    input = listOf(ItemReference("Raw chicken", 2138, 1)),
                    output = listOf(ItemReference("Cooked chicken", 2140, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Lobster",
            wikiPage = "Lobster",
            totalXp = 120.0,
            skill = Skill.COOKING.displayName,
            maxLevel = 40,
            output = listOf(ItemReference("Lobster", 379, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Lobster",
                    name = "Raw lobster -> Lobster",
                    skill = Skill.COOKING.displayName,
                    level = 40,
                    xp = 120.0,
                    input = listOf(ItemReference("Raw lobster", 377, 1)),
                    output = listOf(ItemReference("Lobster", 379, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Swordfish",
            wikiPage = "Swordfish",
            totalXp = 140.0,
            skill = Skill.COOKING.displayName,
            maxLevel = 45,
            output = listOf(ItemReference("Swordfish", 373, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Swordfish",
                    name = "Raw swordfish -> Swordfish",
                    skill = Skill.COOKING.displayName,
                    level = 45,
                    xp = 140.0,
                    input = listOf(ItemReference("Raw swordfish", 371, 1)),
                    output = listOf(ItemReference("Swordfish", 373, 1)),
                )
            )
        ),
        TrainingMethod(
            name = "Apple pie",
            wikiPage = "Apple pie",
            totalXp = 167.6,
            skill = Skill.COOKING.displayName,
            maxLevel = 30,
            output = listOf(ItemReference("Apple pie", 2323, 1)),
            steps = listOf(
                TrainingStep(
                    step = "Make Pastry dough",
                    name = "Pot of flour + Bucket of water -> Pastry dough + Pot + Bucket",
                    skill = "",
                    level = 0,
                    xp = 0.0,
                    input = listOf(ItemReference("Pot of flour", 1933, 1), ItemReference("Bucket of water", 1929, 1)),
                    output = listOf(
                        ItemReference("Pastry dough", 1953, 1),
                        ItemReference("Pot", 1931, 1),
                        ItemReference("Bucket", 1925, 1)
                    ),
                ),
                TrainingStep(
                    step = "Make Pie shell",
                    name = "Pastry dough + Pie dish -> Pie shell",
                    skill = Skill.COOKING.displayName,
                    level = 1,
                    xp = 0.0,
                    input = listOf(ItemReference("Pastry dough", 1953, 1), ItemReference("Pie dish", 2313, 1)),
                    output = listOf(ItemReference("Pie shell", 2315, 1)),
                ),
                TrainingStep(
                    step = "Make Uncooked apple pie",
                    name = "Cooking apple + Pie shell -> Uncooked apple pie",
                    skill = Skill.COOKING.displayName,
                    level = 30,
                    xp = 0.0,
                    input = listOf(ItemReference("Cooking apple", 1955, 1), ItemReference("Pie shell", 2315, 1)),
                    output = listOf(ItemReference("Uncooked apple pie", 2317, 1)),
                ),
                TrainingStep(
                    step = "Make Apple pie",
                    name = "Uncooked apple pie -> Apple pie",
                    skill = Skill.COOKING.displayName,
                    level = 30,
                    xp = 130.0,
                    input = listOf(ItemReference("Uncooked apple pie", 2317, 1)),
                    output = listOf(ItemReference("Apple pie", 2323, 1)),
                )
            )
        )
    )
}
