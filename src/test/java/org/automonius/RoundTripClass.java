package org.automonius;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class RoundTripTest {

    @Test
    void testScenarioRoundTrip() {
        // Create scenario
        TestScenario scenario = new TestScenario("Login Scenario");
        scenario.addStep(new TestStep("1", "openBrowser", "Browser", ""));
        scenario.addStep(new TestStep("2", "navigate", "Browser", "https://example.com"));
        scenario.addExtra("ExpectedResult");

        // Save scenario
        File outFile = new File("src/main/resources/project/LoginScenario.xlsx");
        MainController.saveTestScenarioToFile(scenario, outFile);

        // Load scenario back
        TestScenario loaded = MainController.importScenarioFromFile(outFile);

        // Verify
        assertEquals(scenario.getName(), loaded.getName());
        assertEquals(scenario.getExtras(), loaded.getExtras());
        assertEquals(scenario.getSteps().size(), loaded.getSteps().size());
    }
}
