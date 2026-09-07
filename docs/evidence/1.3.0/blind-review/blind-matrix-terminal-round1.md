# Blind visual fidelity review — Matrix and Terminal (round 1)

I reviewed only the four supplied PNGs. The reference images are about 852–853 px wide; the candidate images are 1080 px wide. The measurements below scale each reference to a common 1080 px content width before comparing. System status/navigation bars and runtime time/countdown values are excluded.

## Matrix

**Verdict: not identical.** The candidate keeps the same semantic sections (logo, next alarm, two alarm rows, model action, add action), and the main content is centered horizontally in the same general area. The Matrix identity is otherwise substantially different in layout, type, background code, and controls.

### Geometry and spacing

- **Header/logo:** The reference logo is centered at roughly x=50%, with the settings gear aligned on the right side of the same header band. The candidate logo begins around x=7% and ends around x=37%, so it is left aligned; its gear is lower and brighter. Restore the centered logo and align the gear vertically with it.
- **Next-alarm panel:** Reference bounds are approximately x=80–1000, y=365–861 after common-width scaling. Candidate bounds are approximately x=43–1037, y=424–907. The candidate panel is about 74 px wider, starts roughly 60 px lower, and has a larger corner radius. In the reference the hero is visibly narrower than the alarm rows; the candidate gives the hero and rows essentially the same outer width.
- **Alarm rows:** Reference rows are approximately x=52–1027, with first/second y ranges of 928–1160 and 1189–1420. Candidate rows are approximately x=43–1037, at 972–1240 and 1305–1573. Candidate rows are slightly wider, about 35–40 px taller, and have a much larger gap between them. The reference is a compact horizontal row: time at left, title/subtitle in the middle, controls at right. The candidate stacks time, title, and subtitle vertically, leaving the row much taller.
- **Model action:** Reference is a narrow centered button, about x=262–817 and y=1807–1954. Candidate is full card width, about x=43–1037 and y=1620–1753. It is roughly 1.7× too wide and appears about 200 px too high. Reference leaves a large blank interval after the second alarm; candidate places the model action immediately below it.
- **Add-alarm action:** Reference is a medium-width centered CTA, about x=141–937 and y=1995–2206. Candidate is full width, x=43–1037 and y=2168–2301. Candidate is too wide, shorter, and lower. The reference model-to-CTA gap is small; the candidate has a very large gap after its model button, reversing the vertical rhythm.
- **Borders/corners:** Reference alarm rows are a little wider than the hero and use tighter rounded corners. Candidate uses one almost uniform width and visibly rounder, heavier outer corners for every panel.

### Typography and color

- Reference uses a light geometric logo, a clean sans-serif for `PRÓXIMO ALARME`, alarm names, and supporting text, plus a seven-segment display treatment for the times. Candidate uses a monospaced/pixel display face for nearly every label, including `Trabalho`, `Fim de semana`, subtitles, button labels, and the hero heading. This changes both the identity and the text density.
- Candidate alarm times are positioned in the top-left stack and read heavier/more solid. Reference times sit vertically centered in the left column and have a lighter segmented construction.
- Reference supporting text has a clear hierarchy: the countdown line mixes mint text with a white numeric portion, and the alarm subtitle is smaller and softer. Candidate makes most text the same pale green/mint family and removes that contrast.
- Candidate’s visible copy also differs in small details: `Sáb e Dom` appears where the reference uses `Sáb–Dom`; the add action is rendered as `+ Alarme` rather than a large standalone plus followed by `Alarme`.
- Reference is near-black with muted green code and soft mint accents. Candidate’s base is visibly dark green/teal even in empty areas, with brighter saturated green borders and flatter pale-green text. Reduce the base fill and soften the border accent.

### Matrix code, texture, glow, and line weight

- Reference background code is dense, vertical, and columnar: thin streams of repeated glyphs fall from top to bottom, with selected columns brighter than others. Candidate uses sparse, isolated `0/1/7/A` glyphs and square blocks scattered across the canvas. Recreate the vertical streams and their depth/fade pattern.
- Reference code remains subtly visible through the dark glass panels. Candidate panel interiors are mostly opaque, flat dark-green rectangles and hide the background texture.
- Reference has a diffuse green bloom around the large hero digits, a restrained haze in the panels, and a strong soft aura around the final CTA. Candidate has a sharp local glow around the hero digits but little ambient panel/CTA bloom; its panel outlines read as hard neon strokes.
- Reference panel strokes are thin and subdued (roughly 1–2 px at common width), while candidate borders are brighter and commonly 2–3 px. Reserve the heavier luminous treatment for the final CTA and reduce the other borders.

### Icons and controls

- The reference settings gear is a thin green outline aligned to the header. Candidate’s gear is larger/heavier and near-white, with a lower vertical placement.
- Reference alarm controls are vertically centered in each row and include a thin divider before the vertical menu. Candidate controls sit near the top-right of the tall stack and omit the divider.
- Reference enabled toggle uses a glowing green pill with a white thumb at the right; disabled uses a gray pill with a light thumb at the left. Candidate uses a smaller, flatter green enabled toggle and a dim-green disabled thumb, so the two states lack the reference contrast.
- Reference `Usar um modelo` includes a small QR-like icon on the left. Candidate has no icon and centers only the text.
- Reference add CTA uses a large plus icon and a wide luminous rounded outline. Candidate substitutes a small literal `+` in the text and uses a wide but comparatively flat outline.

### Prioritized correction checklist

1. **P0 — Rebuild the vertical geometry:** center the logo; make the hero narrower than the alarm rows; return alarm rows to a compact horizontal layout; move the model action below a large post-card gap; make the model button narrow and centered; make the add CTA medium width with a short model-to-CTA gap.
2. **P0 — Replace the background treatment:** dense vertical cascading Matrix columns, near-black base, code visible through translucent panels, and selected brighter/fading columns.
3. **P1 — Restore the Matrix type system:** clean sans-serif body/labels, light geometric logo, segmented display times, and distinct mint/white hierarchy. Remove the pixel/monospace treatment from body copy and buttons.
4. **P1 — Restore glass and glow:** thinner muted panel strokes, tighter radii, subtle interior haze, hero-digit bloom, and a much stronger soft glow only around the final CTA.
5. **P1 — Match controls:** centered row controls, divider before menu, white toggle thumbs in both states, QR-like model icon, large standalone plus, and the reference gear weight/position.
6. **P2 — Correct micro-copy/spacing:** use `Sáb–Dom`, preserve the reference text spacing and baseline hierarchy, and tune the label/countdown vertical offsets inside the hero.

## Terminal

**Verdict: not identical.** The candidate preserves the broad semantic order and uses a terminal-like bitmap face, so it is closer structurally than Matrix. Its terminal chrome, box grammar, controls, typography treatment, and vertical spacing still diverge visibly.

### Geometry and spacing

- **Header:** Reference title begins near x=5% and sits around common-width y≈160; candidate begins near x=7% and sits around y≈220. Candidate is lower and more inset. Reference `[AJUSTES]` is aligned much closer to the right edge; candidate is smaller/inset toward the center.
- **Header divider:** Reference has a thin continuous green divider from roughly x=42 to x=1036 at common-width y≈264. Candidate has no header divider, only its background stripes.
- **Hero box:** Reference outline is about x=54–1020, y≈350–860 after scaling. Candidate’s dark panel/corner treatment is about x=42–1038, y≈423–921. Candidate begins roughly 70 px too low, ends roughly 60 px too low, and is slightly wider.
- **Alarm boxes:** Reference first/second rows are approximately y≈931–1227 and 1286–1581 at common width, with the same terminal box geometry as the hero. Candidate rows are roughly y≈980–1260 and 1328–1600. The candidate rows are a little lower and wider, with less faithful box outlines.
- **Model action:** Reference is a narrow centered box, about x=220–860 and y≈1710–1845. Candidate is close to full width and appears around y≈1590–1780. It is much too wide and starts roughly 100–120 px too early, so the post-card gap is missing.
- **Add action:** Reference bounds are about x=52–1028 and y≈2045–2263. Candidate is about x=42–1038 and y=2167–2302. Width is close but candidate starts about 120 px too low and is shorter, leaving too little bottom breathing room.
- **Vertical rhythm:** Reference gaps are deliberate: header divider→hero, hero→row 1, row 1→row 2, row 2→model, then model→CTA. Candidate makes the header→hero gap too large, row gaps roughly similar, row 2→model too small, and model→CTA far too large.

### Terminal box grammar and texture

- Reference major boxes are continuous thin green outlines with square corners and a `+` marker at every corner. Candidate hero, alarm, and model panels are mostly dark filled rectangles with short L-shaped corner fragments; they lack the continuous side/top/bottom lines and the plus markers. The add box has a continuous line but still lacks the reference corner markers.
- Reference background is almost black with dense fine horizontal CRT scanlines across the whole canvas. Candidate stripes are much wider, more regularly spaced, and higher contrast, creating large horizontal bands. Panels also suppress the stripe texture instead of letting it continue through.
- Reference text has scanline/dither breakup and slight raster noise inside the strokes. Candidate glyphs are clean, solid, pale bitmap blocks. Apply the same raster/scanline treatment to logo, labels, digits, controls, and buttons.
- Reference is a consistent terminal green range: dim green body text and bright green outlines/digits. Candidate introduces pale mint/near-white primary text and separate neon-green controls, weakening the monochrome terminal palette. Darken and unify the palette.

### Typography, controls, and buttons

- The reference hero label is larger and wider relative to its box. The reference hero digits occupy roughly 600 px of common-width content; candidate digits are roughly 400 px wide and read smaller/narrower inside the panel. Restore the reference digit scale and scanlined segmented construction.
- Candidate `Desperta` is heavier and cleaner than the thin, rasterized reference logo. Candidate `[AJUSTES]` is also lower/right-shifted relative to the header.
- Reference card times are larger and have the same thin CRT treatment as the hero. Candidate card times are smaller and more solid. Preserve the left time/title/subtitle stack, but increase time prominence and match the reference baselines.
- Reference card controls are literal terminal text: `[ATIVO]` and `[...]` on the first row, `[PAUSADO]` and `[...]` on the second, separated from the content by a vertical divider. Candidate replaces these with Android-style toggle pills and bare `...` dots. Remove the pills, restore the bracketed status strings and bracketed ellipsis, and add the divider.
- Reference model button text is `[ USAR UM MODELO ]` with bracket spacing and a narrow centered box. Candidate says `Usar um modelo`, without brackets, in a full-width panel.
- Reference CTA text is `[ + NOVO ALARME ]`, large and centered. Candidate says `+ Alarme`, small and title case, without the brackets or the `NOVO` wording.
- Reference uses square terminal corners and a thin, bright line; candidate’s dark panel fills and partial corner marks make the controls look like modern cards rather than a terminal interface.

### Prioritized correction checklist

1. **P0 — Restore terminal chrome:** dense fine scanlines over the full canvas; near-black base; thin continuous outlines on every hero/card/action box; square corners with `+` corner markers.
2. **P0 — Restore terminal controls/copy:** replace both toggle pills with `[ATIVO]`/`[PAUSADO]`, use `[...]` menus, add the vertical dividers, restore `[ USAR UM MODELO ]` and `[ + NOVO ALARME ]`.
3. **P0 — Fix the vertical layout:** add the header divider, move the hero upward, preserve the row rhythm, add the larger row-2→model gap, and place the CTA at the reference lower position with the reference height.
4. **P1 — Match scale and typography:** enlarge the hero digits and card times to reference proportions; use the thinner rasterized terminal font with scanline breakup throughout; align the title/settings baselines.
5. **P1 — Unify terminal color:** eliminate the pale mint/near-white body treatment, use dim/bright terminal green levels, and keep the outlines thin rather than neon-filled.
6. **P2 — Tune panel interiors and micro-spacing:** allow scanlines to remain visible through panels, match left content insets, and align label, title, subtitle, status, and menu baselines to the reference.

