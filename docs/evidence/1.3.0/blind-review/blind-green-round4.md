# Blind visual comparison — round 4

Scope: visual comparison only. References: `app/src/main/assets/identities/matrix-reference.png` and `terminal-reference.png`. Candidates: `round4/matrix.png` and `terminal.png`. Runtime clock, system bars, and the changing countdown values were ignored as requested.

The pairs are **not visually identical**. Both candidates still have material identity, typography, texture, and layout differences.

Reference widths are 853 px (Matrix) and 852 px (Terminal); candidates are 1080 px. The reference geometry below is considered after scaling it to 1080 px wide (about 1.266x and 1.268x respectively). Horizontal ranges are approximate fractions of viewport width (`x/W`).

## Matrix

### P0 — blocking visual parity differences

- **The background texture is a different visual system.** The reference has sparse, narrow, vertically aligned streams of mostly numeric glyphs, with strong column structure and fading/low-density areas. The candidate has a dense, nearly uniform field of randomly distributed `0/1/7/A/X/< />` characters. The candidate texture is visible at the same density through and behind the panels, while the reference panels are predominantly black with only restrained code showing through. Restore the reference's sparse vertical streams, column spacing, fade, and very low contrast.

- **The main countdown is far too large and visually heavier.** At equal width the reference timer occupies about 53% of the viewport (`x/W` roughly 23–77%); the candidate occupies about 71% (`15–86%`). The candidate's timer is also much taller, with thick bright segments and a broad glow; the reference is a more compact seven-segment display with finer strokes and a contained green haze. Reduce the timer footprint and segment thickness/glow to the reference proportions.

- **The timer treatment and surrounding card do not share the reference's restrained contrast.** Candidate digits and heading are high-brightness mint/neon against a clearly green-tinted card. The reference uses a near-black card, dimmer green outline, and localized glow around the display. Match the reference's black fill, muted border, and localized display glow.

### P1 — major geometry and component differences

- **Main card width/height:** reference is approximately `7.4–92.7%` wide; candidate is `6.5–93.4%`, about 1.6 percentage points wider. After equal-width scaling, the reference card is about 495 px tall; the candidate is about 547 px tall. Candidate corners are visibly more rounded. Narrow the card, reduce its height, and use the reference corner radius.

- **Main-card internal spacing is spread by the oversized timer.** The heading width is close in normalized terms (about 30% in both), but the candidate heading is brighter/heavier. The candidate subtitle sits much lower and the card bottom is pushed down by the timer; restore the reference vertical rhythm and lighter heading/subtitle weight.

- **Logo/gear relationship is wrong.** The wordmark is centered in both images, but the candidate wordmark is paler/heavier and uses smoother, wider-looking glyph strokes. The reference gear center is near 92.5% of width; the candidate gear center is near 87.5%, about 5 percentage points too far inward. Move the gear toward the right edge and match the reference's thinner green outline and darker center.

- **Alarm row geometry is too wide and too short.** Reference rows are about `4.8–95.2%` wide and, after scaling, roughly 230 px tall. Candidate rows are about `4.1–95.9%` wide but only about 192–194 px tall. Candidate row corners are also more rounded. Use the reference row width, height, and radius.

- **Row content is compressed toward the left/right controls.** The candidate time occupies about 18% of width versus about 21% in the reference; the title starts around 31% versus about 34%; the candidate switch is only about 7.6% wide versus about 10.7% and ends earlier. The divider is around 82% versus about 85% in the reference. Restore the reference time/title/control columns, including the larger switch track and later divider.

- **Switch styling is wrong in both states.** The reference active track is a luminous emerald pill; the candidate active track is a smaller, dark green pill. The reference inactive track is gray/metallic with a visible gray rim; the candidate inactive track remains green-black and lacks that gray treatment. Match track width, rim, color, and glow while keeping the knob side/state.

- **The model button has the right normalized width but is too low and too bright.** Both are about 52% wide (`24–76%`), but after equal-width scaling the candidate top is roughly 70–75 px lower. Candidate text is brighter/heavier and the dotted-square icon/frame is more prominent. Move the button up, reduce the border brightness, and match the reference text weight and icon scale.

- **The add button has the right normalized width but the wrong surface treatment and label.** Both are about 74% wide (`13–87%`), but the candidate is lower by roughly 70–80 px after width scaling. The reference has a single bright rounded border with strong outer green glow; the candidate shows a dimmer double outline with little glow. The candidate `+ Alarme` label is narrower and much bolder/whiter than the reference's lighter, wider green label. Restore the single luminous border, glow, spacing, and lighter label.

### P2 — fine visual differences

- Candidate panel and row strokes are brighter and more saturated than the reference's thin muted green strokes; candidate rounded corners are consistently larger.
- Candidate row title/subtitle typography is heavier and paler, while the reference title is closer to white and the secondary line is a quieter green. Match the reference weight and color split.
- Candidate kebab dots and the model icon appear slightly heavier/brighter. Keep their positions after the column correction but reduce stroke/brightness to the reference.
- Candidate code characters remain visible inside the empty lower area with uniform density; the reference has darker gaps and more deliberate vertical stream falloff. This is secondary to the P0 texture correction but should be preserved through the whole viewport.

## Terminal

### P0 — blocking visual parity differences

- **The CRT/terminal rendering is not reproduced in the content.** The reference uses fine scanline/dither texture throughout the logo, digits, headings, labels, and controls. Candidate text and especially the main timer are flat, solid mint blocks with chunky edges. Retain the terminal scanline/grain inside glyphs and use the reference's thinner, more delicate pixel strokes.

- **The main countdown is dramatically oversized.** At equal width the reference occupies roughly 58% of viewport width (`x/W` about 21–78%); the candidate occupies roughly 75% (`12–88%`). Candidate digits are also much taller and filled with broad solid segments rather than the reference's finer scanlined display. Reduce the timer to the reference footprint and segment weight.

### P1 — major geometry and typography differences

- **Header wordmark is too narrow and settings is too far inward.** Reference `Desperta` is about 31% of width and ends near 36% of the viewport; candidate is about 23% and ends near 28%. Reference `[AJUSTES]` ends around 95% of width; candidate ends around 85%, leaving an oversized right margin. Match the reference terminal font scale/spacing and right-align settings to the header rule.

- **Main-card heading and subtitle are too narrow.** Reference heading is about 32% of width and subtitle about 51%; candidate is about 20% and 33%. Candidate uses noticeably chunkier, less extended glyphs. Increase the terminal text scale/letter spacing to the reference proportions while preserving the one-line centered layout.

- **Terminal cards are too wide.** Reference main/alarm panels are about `5.2–94.7%` wide; candidate panels are about `4.1–95.9%`, roughly 2.2 percentage points wider. Narrow all panels and restore the reference side margins.

- **Upper vertical gaps are compressed.** After equal-width scaling, the reference has roughly 68–86 px between the header rule and main card and roughly 70–85 px between the main card and first alarm card. Candidate has about 28 px for both. The candidate gap between alarm cards is about 24 px, while the reference is roughly 60 px before scaling (about 75 px at equal width). Restore the larger terminal vertical gaps.

- **The second-card-to-model gap is then too large.** Candidate leaves roughly 280 px between the second alarm card and model button; the reference is about 110 px before scaling (about 140 px at equal width). Rebalance the stack instead of compensating with a large blank region.

- **Alarm-card inner padding/time scale is wrong.** Candidate row times are only about 17% of viewport width; reference times are about 25%. Candidate content begins closer to the left border (roughly 7% versus reference 10%). Increase the time display and restore the reference left padding. Status/menu/divider columns are broadly aligned, but their glyphs remain too chunky and solid.

- **Model and add button labels are too narrow for their boxes.** Model button width is about 59% in the candidate versus about 57% in the reference, but the candidate label is only about 28% of viewport width versus about 40%. Add button width is about 92% versus about 89%, while its label is about 46% versus about 62%. Match the reference terminal font size, spacing, and label width rather than only resizing the outer boxes.

- **Button/card borders are too bright and thick.** Candidate terminal rules and plus-corner markers read as bright solid neon lines. Reference rules are thinner, lower contrast, and integrated with the CRT scanlines. Reduce stroke weight/brightness and restore the reference corner-marker scale/placement.

### P2 — fine visual differences

- Candidate horizontal scanlines are more conspicuous and greener across the background; the reference scanlines are finer and more restrained against near-black.
- Candidate text color is flatter/paler mint. Reference terminal green has visible scanline modulation and a slightly darker body tone; apply that texture consistently to heading, subtitle, status, menu, and button labels.
- Candidate corner `+` markers and `[ATIVO]`/`[PAUSADO]`/`[...]` glyphs are heavier than the reference even where their approximate columns are correct.

## Concrete correction order

1. Restore the two identity-specific textures: sparse vertical Matrix streams and fine Terminal scanlines/dither.
2. Correct both main countdown displays to the reference width, height, segment weight, and glow.
3. Correct Terminal font metrics and Matrix font weights/colors, including the header/settings and button labels.
4. Rebuild vertical spacing from the reference: Terminal's larger upper/card gaps and smaller second-card-to-model gap; Matrix's shorter main/row cards and higher model/add buttons.
5. Correct panel widths/radii/strokes, Matrix switch geometry/skins, gear position, and the two button border/glow treatments.

