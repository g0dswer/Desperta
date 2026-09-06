package com.desperta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MissionLogicTest {
  @Test
  public void barcodeRequiresExactPayload() {
    assertTrue(MissionLogic.barcodeMatches("DESPERTA-ACORDAR-2026", "DESPERTA-ACORDAR-2026"));
    assertFalse(MissionLogic.barcodeMatches("DESPERTA-ACORDAR-2026", "DESPERTA-ACORDAR-2026 "));
    assertFalse(MissionLogic.barcodeMatches("DESPERTA-ACORDAR-2026", "DESPERTA-ACORDAR-2025"));
    assertFalse(MissionLogic.barcodeMatches("", ""));
  }

  @Test
  public void progressIsBoundedAndRequiresAllRepetitions() {
    assertEquals(1, MissionLogic.incrementBounded(0, 1));
    assertEquals(3, MissionLogic.incrementBounded(4, 3));
    assertFalse(MissionLogic.isComplete(2, 3));
    assertTrue(MissionLogic.isComplete(3, 3));
    assertEquals(1, MissionLogic.safeCount(0));
    assertEquals(100, MissionLogic.safeCount(1000));
  }

  @Test
  public void typingMathSpeechAndLabelsAreVerified() {
    assertTrue(MissionLogic.typingMatches("Acorde agora", "Acorde agora"));
    assertFalse(MissionLogic.typingMatches("Acorde agora", "acorde agora"));
    assertFalse(MissionLogic.typingMatches("Acorde agora", "Acorde agora "));
    assertTrue(MissionLogic.mathAnswerMatches(-4, " -4 "));
    assertFalse(MissionLogic.mathAnswerMatches(4, "4.0"));
    assertTrue(MissionLogic.spokenWordMatches("ação", "ACAO"));
    assertTrue(MissionLogic.spokenWordMatches("café", "diga café agora"));
    assertFalse(MissionLogic.spokenWordMatches("cat", "cattle"));
    assertTrue(MissionLogic.labelMatches("garrafa", "Garrafa de água"));
    assertFalse(MissionLogic.labelMatches("garrafa", "copo"));
  }

  @Test
  public void colorAndShakeChecksRejectWrongInputAndDebounce() {
    assertTrue(MissionLogic.colorMatches(0x00ff00, 0x00ff00, 0));
    assertFalse(MissionLogic.colorMatches(0x00ff00, 0xff0000, 10));
    assertTrue(MissionLogic.shakeDetected(0f, 0f, 30f, 1000L, -1L));
    assertFalse(MissionLogic.shakeDetected(0f, 0f, 30f, 1200L, 1000L));
    assertTrue(MissionLogic.shakeDetected(0f, 0f, 30f, 1700L, 1000L));
  }

  @Test
  public void photoSignatureNeedsDetailAndMatchesSimilarHashes() {
    int[] flat = new int[64];
    for (int i = 0; i < flat.length; i++) flat[i] = 128;
    assertFalse(MissionLogic.hasMeaningfulVariance(flat));

    int[] image = new int[64];
    int[] brighter = new int[64];
    for (int i = 0; i < image.length; i++) {
      image[i] = (i % 8 < 4 ? 20 : 220);
      brighter[i] = image[i] + 20;
    }
    assertTrue(MissionLogic.hasMeaningfulVariance(image));
    assertTrue(
        MissionLogic.photoMatches(
            MissionLogic.averageHash(image), MissionLogic.averageHash(brighter)));
    assertFalse(
        MissionLogic.photoMatches(MissionLogic.averageHash(image), MissionLogic.averageHash(flat)));
  }

  @Test
  public void squatRequiresDownThenUp() {
    MissionLogic.SquatState state = new MissionLogic.SquatState(MissionLogic.SquatPhase.UP, 0);
    state = MissionLogic.updateSquat(state, 170f);
    assertEquals(0, state.repetitions);
    state = MissionLogic.updateSquat(state, 95f);
    assertEquals(MissionLogic.SquatPhase.DOWN, state.phase);
    assertEquals(0, state.repetitions);
    state = MissionLogic.updateSquat(state, 170f);
    assertEquals(MissionLogic.SquatPhase.UP, state.phase);
    assertEquals(1, state.repetitions);
  }
}
