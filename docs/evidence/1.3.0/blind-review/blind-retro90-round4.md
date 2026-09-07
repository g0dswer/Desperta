# Blind visual comparison — Round 4

This is a render-only comparison of the two reference PNGs and the two supplied candidate PNGs. I did not inspect implementation or source files. Reference and candidate widths were normalized before comparing geometry (retro: 853 px vs 1080 px, scale 0.790; nineties: 862 px vs 1080 px, scale 0.798). OS status bars, gesture/navigation bars, the displayed clock, and the runtime countdown value were ignored as requested.

Neither candidate is visually identical to its reference.

## Retro

### P0

- None. The card stack, hero panel, paper palette, and overall retro silhouette are close enough that there is no single screen-level identity failure comparable to the nineties vertical-flow problem below.

### P1

- **Alarm-row time typography and copy placement:** On both alarm rows, the candidate `07:30`/`09:00` glyphs are visibly larger and wider than the reference after width normalization (roughly 40–50% more horizontal span and noticeably more height). The candidate copy block begins about 2.5–3 percentage points of screen width too far left. The row title is also a little larger/heavier. Reduce the row-time glyph scale/width to the reference metrics and move the text block right while keeping the card and overflow column fixed.

- **Alarm-row switches:** The candidate switch tracks are approximately 15% narrower than the reference, leaving a visibly larger gap before the divider. Match the reference track/knob width and the control's right-side alignment; keep the overflow dots at the reference x position.

- **Alarm-row secondary text color:** Candidate `Seg–Sex · QR / Código de barras` and `Sáb–Dom · Matemática` render as a muted blue/teal, while the reference uses a much darker navy. Match the reference ink color and weight, including the small orange separator dot.

- **Paper texture:** The candidate has repeated broad vertical tonal bands across the next-alarm card, both alarm cards, and the model row. The reference has a substantially more even warm paper field with fine grain and localized aging marks. Remove the conspicuous striping and restore the reference's uniform grain/stain distribution.

- **“Usar um modelo” row alignment:** The candidate model icon and label sit about 5 percentage points of screen width left of the reference. The candidate label is also somewhat larger/wider. Shift the icon/label group right to the reference anchor and match the label's scale and width.

- **“+ Alarme” label alignment:** The candidate label is centered around roughly 58% of screen width; the reference is around 46%. This produces an obvious oversized gap between the plus control and the label. Move the label left by about 12 percentage points of screen width and match its reference width/letter spacing.

### P2

- **Hero art vertical proportions:** The candidate sun begins slightly higher and the orange stripe band is a little thicker than the reference after normalization. Align the sun top and stripe-band thickness/baseline with the reference; the orbit paths and overall hero width are otherwise close.

- **Next-alarm heading and rails:** The candidate `PRÓXIMO ALARME` is slightly narrower/smaller (about 5–10%), while the horizontal rails reach closer to the card edges. Match the heading font metrics and shorten/reposition both rails to the reference endpoints.

- **Next-alarm lower line:** The candidate bottom information line is slightly narrower/smaller than the reference. Match its size, tracking, and vertical placement around the divider line.

- **Bevel and shadow finish:** The candidate cards preserve the general double-outline construction, but the reference has stronger layered warm bevels and more dimensional edge shadows, especially on the lower cards and the orange action bar. Match the visible stroke hierarchy and shadow depth after the geometry fixes.

## Nineties

### P0

- **Broken vertical flow before “+ Alarme”:** The candidate leaves a blank teal gap of roughly 16–17% of screen width between the bottom of “Usar um modelo” and the top of the blue action bar. The reference gap is only about 2–3% of screen width. Pull the action bar up directly below the model row and restore the reference bottom padding. This is the largest screen-level mismatch.

- **Main-panel height and downstream stack:** The candidate main panel is about 9–10 percentage points of screen width shorter than the reference. Consequently, the two alarm rows and the model row begin roughly 7–8 percentage points of screen width too early relative to the main panel top, while the action bar is too low. Restore the reference main-panel height and the cumulative y positions of the row stack.

### P1

- **Main-panel width:** The candidate panel spans about 96% of screen width (left/right insets near 2%), versus about 91% in the reference (insets near 4.5%). Narrow and center the panel to the reference bounds; the same correction applies to the row cards, which are also slightly too wide and left-biased.

- **Header icon/title scale:** The candidate header alarm-clock icon is larger and begins about 2–3 percentage points of screen width too far left. The candidate `Desperta` wordmark is roughly 15–20% wider/larger than the reference. Match the icon bounds and reduce the wordmark metrics to the reference.

- **Header settings control:** The candidate settings button is roughly 30–35% wider/taller than the reference and its gear is a thin navy outline. The reference uses a smaller, dark filled/pixel-style gear. Match the button rectangle, bevel, gear silhouette, fill, and placement.

- **Main-panel heading:** Candidate `PRÓXIMO ALARME` is approximately 35–40% wider than the reference and begins about 2 percentage points of screen width too far left. Reduce the pixel-font size/width and set the baseline and horizontal rule endpoints to the reference geometry.

- **Countdown bezel and display:** The candidate countdown frame is slightly wider but visibly shorter, and it is a single flat grey surround. The reference has a taller, layered metallic bevel with bright and dark inset strokes. Match the bezel layers and display bounds, then reduce the candidate digits by roughly 5–10% in both width and height. The candidate cyan segments are flatter/brighter; restore the reference's textured/graded segment appearance.

- **Main-panel surface:** Candidate panel grey is darker and flatter. The reference is a lighter silver-grey with a subtle metallic gradient/noise. Match that surface value and texture across the panel and its bezel.

- **Alarm-row typography:** Candidate row times, titles, and detail lines are visibly wider/larger than the reference (about 5–20%, depending on line). Match the reference pixel-font scale, weight, and line spacing; keep the row text anchor near the reference x position, which is otherwise close.

- **Alarm-row controls:** Candidate switch tracks are about 27–30% narrower than the reference and read as flat square blocks. The reference switches are longer, beveled, and more dimensional. Candidate overflow buttons are also about 20% wider. Match the track, knob, menu-button rectangles, bevel highlights, and right-column alignment.

- **Action-bar horizontal alignment:** Even after fixing the vertical gap, the candidate `+ Alarme` label begins about 5 percentage points of screen width to the right of the reference. Shift the label left and match its reference centering/letter spacing; the blue bar itself is otherwise close in color and general form.

### P2

- **Card strokes and shadows:** The candidate preserves the white top/left and dark bottom/right 3D idea, but the reference has more nuanced multi-line bevels and stronger metallic edge contrast. Match the stroke thickness/order and shadow offsets on the header, main panel, rows, model row, and action bar.

- **Pixel-art icon sizing:** The sunrise image is close in size and centered correctly. The remaining obvious icon discrepancy is the enlarged header alarm clock noted above; the row alarm clocks and model icon are comparatively close after the row geometry is corrected.

- **Small component proportions:** The model-row card is slightly shorter, and the candidate action bar is a little taller than the reference after width normalization. Match those heights once the large vertical-flow corrections are applied.

