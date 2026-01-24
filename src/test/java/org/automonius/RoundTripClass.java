package org.automonius;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void testScenarioRoundTrip() {
        // --- Create scenario ---
        TestScenario scenario = new TestScenario("Login Scenario");
        scenario.addStep(new TestStep("1", "openBrowser", "Browser", ""));
        scenario.addStep(new TestStep("2", "navigate", "Browser", "https://example.com"));
        scenario.addExtra("ExpectedResult");

        // --- Save scenario ---
        File outFile = tempDir.resolve("LoginScenario.xlsx").toFile();
        MainController.saveTestScenarioToFile(scenario, outFile);

        assertTrue(outFile.exists(), "Scenario file should be created");

        // --- Load scenario back ---
        TestScenario loaded = MainController.importScenarioFromFile(outFile);
        assertNotNull(loaded, "Loaded scenario should not be null");

        // --- Verify name ---
        assertEquals(scenario.getName(), loaded.getName(), "Scenario names should match");

        // --- Verify extras ---
        assertTrue(loaded.getExtras().containsKey("ExpectedResult"),
                "Loaded scenario should contain ExpectedResult extra");

        // --- Verify step count ---
        assertEquals(scenario.getSteps().size(), loaded.getSteps().size(),
                "Step counts should match");

        // --- Verify step contents ---
        for (int i = 0; i < scenario.getSteps().size(); i++) {
            TestStep original = scenario.getSteps().get(i);
            TestStep reloaded = loaded.getSteps().get(i);

            assertEquals(original.getItem(), reloaded.getItem(), "Item mismatch at step " + i);
            assertEquals(original.getAction(), reloaded.getAction(), "Action mismatch at step " + i);
            assertEquals(original.getObject(), reloaded.getObject(), "Object mismatch at step " + i);
            assertEquals(original.getInput(), reloaded.getInput(), "Input mismatch at step " + i);
        }
    }
}
