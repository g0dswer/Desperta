# Blind visual detail audit — identity candidates, round 1

Scope: image-only comparison of the four supplied reference/candidate pairs. The references are about 852–862 px wide and the candidates are 1080 px wide; observations below are made after mentally scaling the candidate width to the reference width. Status-bar glyphs/times and countdown/date values were ignored. No source or other review was consulted.

Priority:

- **P0** — missing, blank, or visibly clipped content; blocks identity recognition or basic reading.
- **P1** — major layout, control, typography, or texture mismatch that is immediately visible.
- **P2** — fine-detail mismatch after the P0/P1 items are corrected.

## Retro

### P0

- Both alarm-row artwork slots are visibly clipped into narrow rectangular crops. In the reference, each row has a complete circular, beveled icon with its full outer ring and the small speaker-dot pattern beneath it. In the candidate, the left side of each icon is cut away by a rectangular slot and the cream source-image rectangle is exposed; the circular ring, lower dots, and part of the illustration disappear. Fix the row image container/mask so the complete circular icon and its lower dot detail fit inside the same slot in both rows.
- The `Usar um modelo` control is partly covered by the orange add control at the bottom. Its lower border is hidden, and its reference left icon and right chevron are absent. Give the template row its own complete height and restore the two end icons before placing the add control below it.
- The add control's left plus asset is another rectangular/cropped image rather than the reference's complete round cream button. The reference also has a distinct grooved grip at the far right; the candidate has no grip lines. Restore the circular plus button and the right-hand four-groove detail.

### P1

- The large countdown uses the wrong zero glyph: both candidate zeros contain a diagonal slash, while the Retro reference uses plain rounded rectangular zeros. The candidate colon is made from two square orange blocks; the reference uses two round orange dots. Replace those glyphs with the reference Retro number set rather than reusing the slashed/rectangular display glyph.
- The large `07:30` is substantially narrower and sits differently inside the upcoming panel after width normalization. The reference nearly spans the panel's central width; the candidate leaves much more side space. Match the reference digit size and baseline as a group, including the colon spacing.
- The upcoming panel has lost the fine construction details that make it read as a metal plate: the reference has left/right hairline rules flanking `PRÓXIMO ALARME`, visible corner screws, and a centered bottom rule with a screw. The candidate has only a plain centered title and small flat tan dots at the corners. Restore the side rules, screw rings/highlights, bottom rule, and center screw.
- Each alarm card is missing the reference's colored vertical side rail (orange on the first, blue on the second), the right-side divider, and the detailed screw hardware. The candidate cards read as generic rounded cream rectangles. Restore those repeated rails/divider/screw elements at the same inset as the reference.
- Alarm-row times are rendered as oversized slashed segmented digits, while the reference row times are smaller plain Retro digits. The candidate time block is both farther left (because the icon crop collapses the reserved slot) and wider. Correct the icon slot first, then restore the reference time width and x-position.
- The on/off switches are smaller after normalization and use a different proportion between track and knob. The candidate knob nearly fills the track height and the capsule is short; the reference has a longer capsule with a smaller inset knob and a more pronounced metal outline. Match the reference capsule width, knob inset, and bevel for both states.

### P2

- The candidate upcoming panel and alarm cards are vertically compressed relative to their reference plates while the whole stack is pushed downward; this is why the template/add area collides with the bottom navigation area. Re-establish the reference panel heights and vertical gaps before tuning text baselines.
- The row metadata is cooler gray-blue and lighter than the reference's dark navy print, and the candidate `Trabalho`/`Fim de semana` weight is heavier. Use the reference navy and weight hierarchy for title, label, and metadata.
- Candidate card screws are single flat tan dots. The reference screws have a dark center, light rim, and small highlight; reuse that treatment consistently in the upcoming and alarm plates.

## Nineties

### P0

- The central upcoming display is completely blank in the candidate. The reference has a framed dark-blue display with bright cyan pixel `07:30` digits. Restore the entire display layer, including the cyan digits and its inner bevel; this is the most obvious missing content on the screen.
- The alarm-clock illustration is missing from both alarm rows. In the reference it occupies a dedicated beveled square at the far left of each row. Restore that repeated icon and reserve its width so the time/name block starts at the reference x-position.
- The template row has no reference document/calendar icon, and the add row has no yellow plus square. Restore both left-side icons; the candidate currently presents centered text in empty gray/blue bars.

### P1

- Strong horizontal scanlines cover the candidate's teal background. The Nineties reference has a mostly flat teal field without this CRT stripe pattern. Remove the repeated full-screen lines from this identity; they belong visually to Terminal, not Nineties.
- The header card is too narrow and inset: after normalization the candidate has large side margins, while the reference header nearly reaches both screen edges. The candidate logo begins at the left edge of the header because the left alarm-clock icon is absent; the reference logo is shifted right after that icon. Restore the reference card width and icon/logo spacing.
- The header gear is the wrong icon treatment. The reference uses a black filled cog inside a gray, multi-layer beveled square; the candidate uses a thin navy outline cog with a center dot in a flatter square. Match the filled cog silhouette, button bevel, and shadow.
- The header and cards have mostly flat white/gray outlines. The reference uses several light/dark bevel bands and a clear bottom/right shadow. Restore the multi-band 3D edges on the header, upcoming plate, row cards, row ellipsis buttons, and add bar.
- The title, subtitle, row labels, and template text are anti-aliased modern text in the candidate, while the reference uses a consistent chunky bitmap/pixel face. The candidate `PRÓXIMO ALARME` is especially smooth and thin; use the same pixel raster treatment as the reference for all Nineties copy.
- The candidate upcoming card is shorter and its internal display is much wider than the reference. The blank display currently spans most of the card; the reference has a narrower centered framed display with more gray margin around it. Restore the reference frame width/height and vertical spacing around the sunrise image and subtitle.
- Both switches use blue/lavender tracks and colored knobs. The reference uses a chunky beveled teal/silver switch with a bright light knob in the on state and a silver/gray knob in the off state. Match the reference palette, knob size, and 3D edge treatment.
- The candidate row text begins at the extreme left of the card (around the missing icon slot), whereas the reference time, label, and metadata begin after the square alarm icon. Once the icons are restored, align all three text baselines to the reference column.

### P2

- The candidate row ellipsis controls are close in location but lack the reference's bright inner bevel and deep lower/right shadow. Keep the square footprint but restore those edge bands.
- The template/add bars are lower and have larger side margins than the reference after normalization. Align their width with the row cards and preserve the tighter reference gap between the template bar and the add bar.

## Matrix

### P0

- The reference background is made of many fine, vertically falling columns of green glyphs. The candidate replaces that with sparse, large, isolated digits/letters scattered across the black field. This changes the identity immediately and leaves large areas empty. Restore dense columnar rain with the reference scale, fall direction, opacity gradient, and concentration behind/around the cards.
- Each alarm card's text column is in the wrong place. In the reference, the time is at left and `Trabalho` plus its metadata occupy a separate column to the right of the time. In the candidate, the label and metadata are stacked below the time at the left edge. Move the label/metadata column to the reference x-position and keep the time vertically centered in its own column.

### P1

- The `Desperta` logo is left-aligned in the candidate; the reference centers it across the screen. The candidate word is approximately at the reference y-level but begins near the left margin. Center the logo without moving the right gear control.
- The upcoming panel and both alarm cards are backed by opaque rectangular black slabs that extend beyond their rounded outlines and cover the rain. The reference uses a contained, translucent dark surface whose rounded border is the visible boundary. Remove the rectangular overflow and clip the surface to each rounded shape.
- Candidate panel/card borders are bright neon green and visually thick; the reference uses a much thinner, dimmer green outline with localized glow. Reduce the base stroke brightness/weight and keep glow limited to the reference accents.
- The candidate upcoming panel is wider and lower than the reference after normalization. Its title and especially the subtitle sit progressively too low; the subtitle is close to the bottom border, while the reference leaves a larger balanced lower margin and has a localized bright glow at the bottom center. Match the reference panel width, content baselines, lower margin, and center-bottom glow.
- Alarm rows are taller and more widely separated in the candidate, pushing the lower controls into a different vertical rhythm. Match the reference row height and the smaller gap between rows before positioning the template/add controls.
- The candidate on/off switches are neon green or muted green with matching knobs. The reference on switch has a darker green track with a bright white knob, and the off switch has a dark gray track with a light gray/white knob. Restore the reference contrast and knob/track relationship.
- The reference has a vertical divider before the ellipsis on each alarm card. The candidate divider is absent, leaving the ellipsis floating in the card. Restore the divider at the reference inset.

### P2

- Candidate Matrix text is uniformly monospaced/pixel-bright. The reference mixes a thin futuristic logo and title with smooth body copy and a softer neon glow on the main digits. Reapply that type hierarchy instead of using one pixel face for logo, labels, metadata, and controls.
- The template control is missing the small dotted square icon at its left and therefore centers only the text. Restore the icon and shift the text right to the reference relationship.
- The add control has a plain bright outline and pixel text. The reference has a strong green bloom around a rounded rectangular border and a smooth `+ Alarme` label. Restore the outer glow, radius, and smoother label treatment.
- The template button is separated from the add button by a very large empty region in the candidate. The reference places the add control shortly below the template control. Restore the reference vertical gap and bottom anchoring.

## Terminal

### P0

- The candidate replaces the Terminal reference's textual state tags with graphical switches. The first row must show `[ATIVO]` and the second `[PAUSADO]` in the right-side state column; restore those exact bracketed labels. The candidate also uses bare three-dot glyphs, while the reference uses a bracketed `[...]` control. Restore the brackets and the vertical divider before that control.
- Most of the reference's terminal box outlines are missing. The upcoming panel, both alarm rows, template box, and add box should have continuous green perimeter lines with the reference corner/plus markers. The candidate shows only short corner stubs around opaque black rectangles. Restore complete borders and corner markers, then clip each black surface to its intended box.
- The candidate template control is a wide full-screen bar; the reference is a small centered box with side vertical bars and `[ USAR UM MODELO ]` text. Restore the reference width, centered position, uppercase/bracketed label, and side bars.
- The add control label is wrong and incomplete: candidate `+ Alarme` is missing the reference brackets and the word `NOVO`. Restore `[ + NOVO ALARME ]` and its surrounding corner markers.

### P1

- The candidate header content is roughly 45–55 normalized px lower than the reference. The bright horizontal separator under the header is entirely absent, so the header floats into the upcoming panel. Restore the separator and move the logo/settings line up to the reference y-position.
- The upcoming panel begins too low and its large timer is much smaller/narrower than the reference. The reference `07:30` occupies most of the panel's central width and height; the candidate leaves large side and vertical gaps. Increase the timer glyph size/width and restore its reference baseline.
- Candidate terminal digits and text are solid chunky green. The reference has thinner CRT/scanline/glitch texture inside the glyphs, especially the main timer. Apply the reference raster texture without making the surrounding scanlines brighter.
- Candidate alarm-card content is consistently about one line-height lower than the reference after normalization. Move each time/label/metadata group up within the restored box geometry and retain the reference vertical spacing.
- Candidate alarm times are substantially narrower than the reference. Use the taller, wider segmented terminal glyphs so `07:30` and `09:00` occupy the same left-column width as the reference.
- The candidate scanlines are too coarse and conspicuous: thick dark-green horizontal bands dominate the background. The reference uses finer, subtler CRT lines. Reduce line thickness/contrast and preserve the more even fine-grain texture.

### P2

- `[AJUSTES]` is smaller/narrower and begins farther left in the candidate than in the reference. Keep it right-aligned near the reference edge and match the bracketed pixel width.
- Candidate row toggles remain visually prominent even though they should be text tags. After replacing them with tags, keep the right-side column width and divider aligned so the ellipsis control retains the reference position.
- The candidate add box is pushed close to the bottom navigation area with a very large blank gap above it. The reference template and add controls form a compact lower pair; restore that spacing and bottom offset after the template box is resized.

## Cross-screen repeat issues worth fixing once

- The same left-slot clipping/asset substitution appears in Retro row icons and Retro/Nineties/Matrix lower controls. Verify every repeated icon/control is rendered inside a bounded shape with the full source asset visible before tuning typography.
- Candidate layouts generally use generic rounded/rectangular surfaces and bright full borders where the references use identity-specific edge construction: Retro metal screws/bevels, Nineties gray 3D bevels, Matrix dim translucent neon outlines, and Terminal continuous line boxes with corner markers. Keep those treatments separate per identity.
- Main display glyphs are being reused across identities. Retro plain zeros/round colon, Matrix slashed neon zeros, and Terminal tall CRT segmented digits are visibly different reference systems; select each identity's glyph geometry independently.
