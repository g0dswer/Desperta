package com.desperta;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.*;
import android.os.SystemClock;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MissionUiTest {
  Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
  UiDevice ui = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

  void submit(String value, String button) throws Exception {
    ui.findObject(new UiSelector().className("android.widget.EditText")).setText(value);
    ui.findObject(new UiSelector().text(button)).click();
    ui.waitForIdle();
  }

  void result(ActivityScenario<?> s) {
    long end = SystemClock.elapsedRealtime() + 4000;
    while (s.getState() != Lifecycle.State.DESTROYED && SystemClock.elapsedRealtime() < end)
      SystemClock.sleep(100);
    assertEquals(Lifecycle.State.DESTROYED, s.getState());
    assertEquals(Activity.RESULT_OK, s.getResult().getResultCode());
  }

  @Test
  public void typingRejectsWrongInputAndCompletesExactText() throws Exception {
    try (ActivityScenario<MissionActivity> s =
        ActivityScenario.launchActivityForResult(
            new Intent(c, MissionActivity.class)
                .putExtra("type", "typing")
                .putExtra("target", "Bom dia")
                .putExtra("count", 2))) {
      submit("errado", "Verificar texto");
      assertEquals(Lifecycle.State.RESUMED, s.getState());
      submit("Bom dia", "Verificar texto");
      assertEquals(Lifecycle.State.RESUMED, s.getState());
      s.recreate();
      ui.waitForIdle();
      submit("Bom dia", "Verificar texto");
      result(s);
    }
  }

  @Test
  public void mathRejectsWrongThenAcceptsCorrect() throws Exception {
    try (ActivityScenario<MissionActivity> s =
        ActivityScenario.launchActivityForResult(
            new Intent(c, MissionActivity.class)
                .putExtra("type", "math")
                .putExtra("target", "2+3")
                .putExtra("count", 1))) {
      submit("999", "Verificar resposta");
      assertEquals(Lifecycle.State.RESUMED, s.getState());
      submit("5", "Verificar resposta");
      result(s);
    }
  }

  @Test
  public void colorMissionRequiresRequestedTile() throws Exception {
    try (ActivityScenario<MissionActivity> s =
        ActivityScenario.launchActivityForResult(
            new Intent(c, MissionActivity.class).putExtra("type", "colors").putExtra("count", 1))) {
      UiObject prompt = ui.findObject(new UiSelector().textStartsWith("Toque no bloco "));
      assertTrue(prompt.waitForExists(3000));
      String color = prompt.getText().replace("Toque no bloco ", "").replace(".", "");
      String wrong = color.equals("verde") ? "azul" : "verde";
      ui.findObject(new UiSelector().description("Bloco da cor " + wrong)).click();
      assertEquals(Lifecycle.State.RESUMED, s.getState());
      ui.findObject(new UiSelector().description("Bloco da cor " + color)).click();
      result(s);
    }
  }
}
