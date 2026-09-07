package com.desperta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A complete visual identity for Desperta.
 *
 * <p>Identities are deliberately independent from the legacy light/dark preference. The preference
 * is kept for migrations and for old releases, but every new install starts in {@link #RETRO}.
 * Screens should obtain an instance with {@link #current(Context)} and use its palette and
 * drawables instead of branching on a separate theme flag.
 */
public final class Identity {
  public static final String RETRO = "retro";
  public static final String NINETIES = "nineties";
  public static final String MATRIX = "matrix";
  public static final String TERMINAL = "terminal";
  public static final String KEY = "identity";

  public static final String[] IDS = {RETRO, NINETIES, MATRIX, TERMINAL};
  public static final String[] NAMES = {"Retrofuturista", "Anos 90", "Matrix", "Terminal antigo"};
  public static final String[] DESCRIPTIONS = {
    "Creme, petróleo e laranja queimado em um painel espacial tátil.",
    "Cinza, azul e teal com relevos e botões de software dos anos 90.",
    "Preto profundo, verde esmeralda e detalhes de código luminoso.",
    "Tela preta, fósforo verde e tipografia monoespaçada essencial."
  };

  public final String id;
  public final String name;
  public final int bg;
  public final int surface;
  public final int raised;
  public final int fg;
  public final int muted;
  public final int accent;
  public final int onAccent;
  public final int positive;
  public final int error;
  public final boolean light;
  public final Typeface bodyFont;
  public final Typeface displayFont;

  /**
   * Dedicated clock face; Retro uses the plain-zero display face while other identities follow
   * their display face.
   */
  public final Typeface timeFont;

  private static final Map<String, Typeface> FONT_CACHE = new HashMap<>();
  private static final Map<TextView, Integer> STYLE_FLAGS = new WeakHashMap<>();

  private Identity(Context context, String requestedId) {
    id = normalize(requestedId);
    int index = indexOf(id);
    name = NAMES[index];
    Typeface fallbackBody = Typeface.create("sans-serif", Typeface.NORMAL);
    Typeface fallbackDisplay = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    if (RETRO.equals(id)) {
      bg = Color.rgb(245, 231, 201);
      surface = Color.rgb(255, 246, 218);
      raised = Color.rgb(228, 207, 165);
      fg = Color.rgb(6, 44, 64);
      muted = Color.rgb(80, 112, 128);
      accent = Color.rgb(198, 85, 11);
      onAccent = Color.rgb(255, 245, 226);
      positive = Color.rgb(38, 121, 91);
      error = Color.rgb(182, 49, 38);
      light = true;
      bodyFont = fallbackBody;
      displayFont = loadFont(context, "fonts/Audiowide-Regular.ttf", fallbackDisplay);
      // A small OFL-derived face keeps Retro's zero round and its colon circular, matching the
      // reference clock without reusing the slashed segmented glyph used by other identities.
      timeFont = loadFont(context, "fonts/DespertaDisplay-Regular.ttf", fallbackDisplay);
    } else if (NINETIES.equals(id)) {
      bg = Color.rgb(0, 128, 128);
      surface = Color.rgb(192, 192, 192);
      raised = Color.rgb(216, 216, 216);
      fg = Color.rgb(0, 31, 63);
      muted = Color.rgb(48, 74, 91);
      accent = Color.rgb(0, 0, 128);
      onAccent = Color.WHITE;
      positive = Color.rgb(0, 110, 92);
      error = Color.rgb(128, 0, 0);
      light = true;
      // PixelifySans keeps the compact square bitmap rhythm of the reference while remaining
      // readable in body-sized labels; VT323's tall display proportions are reserved for Terminal.
      bodyFont = loadFont(context, "fonts/PixelifySans-Variable.ttf", fallbackBody);
      displayFont = bodyFont;
      timeFont = displayFont;
    } else if (MATRIX.equals(id)) {
      bg = Color.rgb(2, 8, 5);
      surface = Color.rgb(5, 19, 14);
      raised = Color.rgb(8, 35, 24);
      fg = Color.rgb(160, 235, 182);
      muted = Color.rgb(107, 159, 121);
      accent = Color.rgb(49, 255, 120);
      onAccent = fg;
      positive = Color.rgb(82, 255, 141);
      error = Color.rgb(255, 107, 119);
      light = false;
      // Matrix labels are smooth and quiet; reserve the mono face for the logo/display role and
      // the clock. This preserves the reference hierarchy instead of turning every label into a
      // terminal glyph.
      bodyFont = fallbackBody;
      displayFont = loadFont(context, "fonts/ShareTechMono-Regular.ttf", fallbackDisplay);
      timeFont = displayFont;
    } else {
      bg = Color.rgb(2, 6, 3);
      surface = Color.rgb(5, 17, 8);
      raised = Color.rgb(11, 27, 14);
      fg = Color.rgb(121, 216, 122);
      muted = Color.rgb(87, 168, 100);
      accent = Color.rgb(129, 217, 120);
      onAccent = Color.rgb(102, 255, 136);
      positive = Color.rgb(130, 255, 154);
      error = Color.rgb(255, 156, 112);
      light = false;
      bodyFont =
          loadFont(
              context, "fonts/VT323-Regular.ttf", Typeface.create("monospace", Typeface.NORMAL));
      displayFont = bodyFont;
      timeFont = displayFont;
    }
  }

  /** Returns the saved identity, defaulting to the new Retrofuturista experience. */
  public static Identity current(Context context) {
    String value =
        context.getSharedPreferences(Weather.PREFS, Context.MODE_PRIVATE).getString(KEY, RETRO);
    return new Identity(context, value);
  }

  /** Persists one of {@link #IDS}; invalid values safely fall back to Retrofuturista. */
  public static void set(Context context, String value) {
    String normalized = normalize(value);
    context
        .getSharedPreferences(Weather.PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY, normalized)
        .apply();
  }

  public boolean isRetro() {
    return RETRO.equals(id);
  }

  public boolean isNineties() {
    return NINETIES.equals(id);
  }

  public boolean isMatrix() {
    return MATRIX.equals(id);
  }

  public boolean isTerminal() {
    return TERMINAL.equals(id);
  }

  public String description() {
    return DESCRIPTIONS[indexOf(id)];
  }

  /** Background treatment for an identity-aware root view. */
  public Drawable background(Context context) {
    return new IdentityDrawable(this, IdentityDrawable.BACKGROUND, density(context), null);
  }

  /** Panel treatment: enamel-and-screws, desktop bevel, glass matrix, or terminal linework. */
  public Drawable panel(Context context) {
    return new IdentityDrawable(
        this, IdentityDrawable.PANEL, density(context), ReferenceArt.bitmap(context, id));
  }

  /** Primary action treatment, with enough contrast for all four palettes. */
  public Drawable primary(Context context) {
    return new IdentityDrawable(
        this, IdentityDrawable.PRIMARY, density(context), ReferenceArt.bitmap(context, id));
  }

  /** Secondary action treatment, retaining identity-specific surfaces and borders. */
  public Drawable secondary(Context context) {
    return new IdentityDrawable(
        this, IdentityDrawable.SECONDARY, density(context), ReferenceArt.bitmap(context, id));
  }

  /** Applies status/navigation colors and the correct light-system-bar flags. */
  public void applyWindow(Activity activity) {
    activity.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bg));
    activity.getWindow().setStatusBarColor(bg);
    activity.getWindow().setNavigationBarColor(bg);
    if (Build.VERSION.SDK_INT >= 28) activity.getWindow().setNavigationBarDividerColor(bg);
    int flags =
        activity.getWindow().getDecorView().getSystemUiVisibility()
            & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    if (light) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    if (light && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    activity.getWindow().getDecorView().setSystemUiVisibility(flags);
  }

  /** Applies the identity font while leaving an intentional semantic text color untouched. */
  public void styleText(TextView view, boolean display) {
    if (view == null) return;
    synchronized (STYLE_FLAGS) {
      STYLE_FLAGS.put(view, display ? 1 : 0);
    }
    applyFont(view, display ? displayFont : bodyFont);
  }

  /** Marks a clock/large numeric label so applyTree keeps the dedicated Orbitron face. */
  public void styleTime(TextView view) {
    if (view == null) return;
    synchronized (STYLE_FLAGS) {
      STYLE_FLAGS.put(view, 2);
    }
    applyFont(view, timeFont);
  }

  /**
   * Applies fonts and control tint recursively without replacing custom semantic
   * colors/backgrounds.
   */
  public void applyTree(View view) {
    if (view == null) return;
    if (view instanceof TextView) {
      TextView text = (TextView) view;
      int role;
      synchronized (STYLE_FLAGS) {
        Integer saved = STYLE_FLAGS.get(text);
        role = saved == null ? 0 : saved;
      }
      if (role == 2) applyFont(text, timeFont);
      else styleText(text, role == 1);
    }
    if (view instanceof CompoundButton) {
      CompoundButton control = (CompoundButton) view;
      control.setButtonTintList(
          new ColorStateList(
              new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
              new int[] {accent, muted}));
    }
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) applyTree(group.getChildAt(i));
    }
  }

  /**
   * Creates a themed native dialog. Customization is attached at creation time for .show() callers.
   */
  public AlertDialog.Builder dialog(Activity activity) {
    int theme = light ? android.R.style.Theme_Material_Light_Dialog_Alert : R.style.DespertaDialog;
    return new StyledDialogBuilder(activity, theme, this);
  }

  private static final class StyledDialogBuilder extends AlertDialog.Builder {
    private final Identity identity;
    private final Activity activity;

    StyledDialogBuilder(Activity activity, int theme, Identity identity) {
      super(activity, theme);
      this.activity = activity;
      this.identity = identity;
    }

    @Override
    public AlertDialog create() {
      AlertDialog dialog = super.create();
      dialog.setOnShowListener(
          ignored -> {
            if (dialog.getWindow() != null)
              dialog.getWindow().setBackgroundDrawable(identity.panel(activity));
            View decor = dialog.getWindow() == null ? null : dialog.getWindow().getDecorView();
            identity.applyTree(decor);
            identity.retintNativeDialog(decor);
            identity.styleDialogList(dialog);
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (positive != null) positive.setTextColor(identity.accent);
            if (negative != null) negative.setTextColor(identity.accent);
            if (neutral != null) neutral.setTextColor(identity.accent);
          });
      return dialog;
    }
  }

  private static String normalize(String value) {
    if (value == null) return RETRO;
    String candidate = value.trim().toLowerCase(Locale.ROOT);
    for (String id : IDS) if (id.equals(candidate)) return id;
    return RETRO;
  }

  private static int indexOf(String value) {
    for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(value)) return i;
    return 0;
  }

  private static Typeface loadFont(Context context, String asset, Typeface fallback) {
    if (context == null) return fallback;
    synchronized (FONT_CACHE) {
      Typeface cached = FONT_CACHE.get(asset);
      if (cached != null) return cached;
      try {
        Typeface loaded = Typeface.createFromAsset(context.getAssets(), asset);
        FONT_CACHE.put(asset, loaded);
        return loaded;
      } catch (RuntimeException ignored) {
        FONT_CACHE.put(asset, fallback);
        return fallback;
      }
    }
  }

  private void applyFont(TextView view, Typeface base) {
    Typeface existing = view.getTypeface();
    int style = existing == null ? Typeface.NORMAL : existing.getStyle();
    view.setTypeface(Typeface.create(base, style));
    view.setTextScaleX(isNineties() ? .86f : 1f);
    if (isTerminal()) {
      int color = view.getCurrentTextColor();
      view.getPaint()
          .setShader(
              new android.graphics.LinearGradient(
                  0,
                  0,
                  0,
                  3f,
                  new int[] {
                    color,
                    color,
                    Color.argb(
                        Color.alpha(color),
                        Color.red(color) / 3,
                        Color.green(color) / 3,
                        Color.blue(color) / 3)
                  },
                  new float[] {0f, .64f, 1f},
                  android.graphics.Shader.TileMode.REPEAT));
    } else view.getPaint().setShader(null);
    view.setFontFeatureSettings(isTerminal() || isMatrix() ? "tnum" : null);
  }

  private void retintNativeDialog(View view) {
    if (view == null) return;
    if (view instanceof TextView) {
      TextView text = (TextView) view;
      if (view instanceof CheckedTextView) text.setTextColor(fg);
      int color = text.getCurrentTextColor();
      if (!(view instanceof CheckedTextView)
          && (color == Color.BLACK
              || color == Color.WHITE
              || color == 0xff000000
              || color == 0xffffffff)) {
        text.setTextColor(text.getTypeface() != null && text.getTypeface().isBold() ? fg : muted);
      }
      if (view instanceof CheckedTextView) {
        ((CheckedTextView) view)
            .setCheckMarkTintList(
                new ColorStateList(
                    new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                    new int[] {accent, muted}));
      }
    }
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) retintNativeDialog(group.getChildAt(i));
    }
  }

  private void styleDialogList(AlertDialog dialog) {
    ListView list = dialog.getListView();
    if (list == null) return;
    list.setOnScrollListener(
        new AbsListView.OnScrollListener() {
          @Override
          public void onScrollStateChanged(AbsListView view, int scrollState) {
            styleVisibleRows(view);
          }

          @Override
          public void onScroll(
              AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            styleVisibleRows(view);
          }
        });
    styleVisibleRows(list);
  }

  private void styleVisibleRows(AbsListView list) {
    for (int i = 0; i < list.getChildCount(); i++) retintNativeDialog(list.getChildAt(i));
  }

  private float density(Context context) {
    return context.getResources().getDisplayMetrics().density;
  }
}
