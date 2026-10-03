package com.github.fastpaths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FastPathsTest {

    @Test
    @DisplayName("Verify step height physics for path blocks vs full blocks")
    void testStepHeightPhysics() {
        double vanillaPlayerStepHeight = 0.6;
        double pathStepBoost = 0.4;
        double totalPathStepHeight = vanillaPlayerStepHeight + pathStepBoost;

        assertEquals(1.0, totalPathStepHeight, 0.0001, "Total step height on paths should be 1.0");

        // Height of a dirt path block top: 15/16 = 0.9375
        double pathHeight = 15.0 / 16.0;

        // Player standing on dirt path at Y=0 has foot Y at 0.9375
        double playerFootOnPath = pathHeight;

        // Next block at Y=1 is also dirt path: top is 1.0 + 0.9375 = 1.9375
        double nextPathHeight = 1.0 + pathHeight;
        double diffPathToPath = nextPathHeight - playerFootOnPath;

        assertEquals(1.0, diffPathToPath, 0.0001, "Height diff between consecutive path blocks is 1.0");
        assertTrue(diffPathToPath <= totalPathStepHeight, "Player CAN step up from path to path without jumping");

        // Next block at Y=1 is a full block (e.g. grass/stone): top is 1.0 + 1.0 = 2.0
        double nextFullBlockHeight = 2.0;
        double diffPathToFullBlock = nextFullBlockHeight - playerFootOnPath;

        assertEquals(1.0625, diffPathToFullBlock, 0.0001, "Height diff between path and full block is 1.0625");
        assertFalse(diffPathToFullBlock <= totalPathStepHeight, "Player CANNOT step up from path to full block without jumping");

        // Player on grass block at Y=0 (foot Y = 1.0) trying to step onto dirt path at Y=1 (top 1.9375)
        double diffGrassToPath = nextPathHeight - 1.0;
        assertEquals(0.9375, diffGrassToPath, 0.0001);
        assertFalse(diffGrassToPath <= vanillaPlayerStepHeight, "Player on grass (step height 0.6) cannot step onto path without jumping");
    }

    @Test
    @DisplayName("Verify speed effect amplifier calculations")
    void testSpeedEffectCalculations() {
        // In Minecraft: level 1 is amplifier 0 (Speed I)
        // level 2 is amplifier 1 (Speed II)
        int level1Amplifier = Math.clamp(1 - 1, 0, 255);
        int level2Amplifier = Math.clamp(2 - 1, 0, 255);
        int level255Amplifier = Math.clamp(255 - 1, 0, 255);

        assertEquals(0, level1Amplifier, "Speed 1 should have amplifier 0");
        assertEquals(1, level2Amplifier, "Speed 2 should have amplifier 1 (Speed II)");
        assertEquals(254, level255Amplifier, "Speed 255 should have amplifier 254");
    }

    @Test
    @DisplayName("Verify clamp logic for path-speed config 0-255")
    void testPathSpeedClamping() {
        assertEquals(0, Math.clamp(-5, 0, 255));
        assertEquals(0, Math.clamp(0, 0, 255));
        assertEquals(1, Math.clamp(1, 0, 255));
        assertEquals(2, Math.clamp(2, 0, 255));
        assertEquals(255, Math.clamp(255, 0, 255));
        assertEquals(255, Math.clamp(300, 0, 255));
    }
}
