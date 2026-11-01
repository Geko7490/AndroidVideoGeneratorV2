package com.vex.videoai

import com.vex.videoai.production.ProductionProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionProfileTest {
 @Test fun profilesScaleResourceUsage() {
  assertTrue(ProductionProfile.LOW.llmThreads<ProductionProfile.MID.llmThreads)
  assertTrue(ProductionProfile.MID.llmThreads<ProductionProfile.HIGH.llmThreads)
  assertTrue(ProductionProfile.LOW.contextSize<ProductionProfile.MID.contextSize)
  assertTrue(ProductionProfile.MID.maxTokens<ProductionProfile.HIGH.maxTokens)
 }
 @Test fun higherProfilesUseDenserCameraMotionFrames() {
  assertTrue(ProductionProfile.LOW.frameStepMs>ProductionProfile.MID.frameStepMs)
  assertTrue(ProductionProfile.MID.frameStepMs>ProductionProfile.HIGH.frameStepMs)
 }
 @Test fun threadCountNeverExceedsAvailableProcessors() {
  assertEquals(2,ProductionProfile.HIGH.threadsFor(2))
  assertEquals(6,ProductionProfile.HIGH.threadsFor(6))
 }
 @Test fun unknownProfileDefaultsToMid() {
  assertEquals(ProductionProfile.MID,ProductionProfile.fromId("missing"))
 }
}
