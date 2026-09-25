package com.naji.openai;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ScenarioThemes {

    private static final List<List<ScenarioTheme>> ROUND_SLOTS = List.of(
            List.of(
                    new ScenarioTheme("Real world",
                            "a real problem that could really happen, like being lost, stuck, or caught in bad weather"),
                    new ScenarioTheme("Everyday emergency",
                            "a normal day that suddenly becomes a real problem, like a power cut, a broken lift, "
                                    + "a flooded street or a car that will not start")
            ),
            List.of(
                    new ScenarioTheme("Silly comedy",
                            "a very silly, funny problem, like a group of angry geese chasing you or a giant "
                                    + "round cheese rolling down a hill"),
                    new ScenarioTheme("Awkward comedy",
                            "a funny and embarrassing situation with a real problem, like a wedding, a job "
                                    + "interview or a family dinner that goes very wrong")
            ),
            List.of(
                    new ScenarioTheme("Fantasy quest",
                            "a fantasy world with dragons, wizards, magic castles or magic forests"),
                    new ScenarioTheme("Sci-fi",
                            "a science-fiction problem, like aliens coming to Earth, a robot that stops "
                                    + "listening, a broken space station or a day that repeats"),
                    new ScenarioTheme("Monster fun",
                            "a monster problem, like zombies, vampires or a haunted house, but fun and not too scary")
            ),
            List.of(
                    new ScenarioTheme("Weird and impossible",
                            "a dream-like world with strange rules, like gravity turning off or a city made of jelly"),
                    new ScenarioTheme("Tiny or giant",
                            "a world where everything is the wrong size, like being as small as an ant in a "
                                    + "kitchen or standing next to giants")
            ),
            List.of(
                    new ScenarioTheme("Big final",
                            "a huge, exciting problem with very high stakes, like a giant monster in a city, "
                                    + "the sun going out or the world coming apart"),
                    new ScenarioTheme("Crazy mix",
                            "two very different ideas mixed together, like pirates on the moon or dinosaurs at a "
                                    + "wedding, with big imagination and big laughs")
            )
    );

    private ScenarioThemes() {
    }

    public static ScenarioTheme forRound(int roundNumber) {
        int slotIndex = Math.min(Math.max(roundNumber, 1), ROUND_SLOTS.size()) - 1;
        List<ScenarioTheme> options = ROUND_SLOTS.get(slotIndex);
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }
}
