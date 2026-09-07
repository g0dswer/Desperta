from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib.tables._g_l_y_f import GlyphCoordinates
f=instantiateVariableFont(TTFont('app/src/main/assets/fonts/Orbitron-Variable.ttf'),{'wght':650},inplace=False)
p=TTGlyphPen(None)
# Rounded zero without a diagonal, matching the approved space-age numeric face.
x0,y0,x1,y1=57,0,784,720;r=225
p.moveTo((x0+r,y0));p.qCurveTo((x0,y0),(x0,y0+r));p.lineTo((x0,y1-r));p.qCurveTo((x0,y1),(x0+r,y1));p.lineTo((x1-r,y1));p.qCurveTo((x1,y1),(x1,y1-r));p.lineTo((x1,y0+r));p.qCurveTo((x1,y0),(x1-r,y0));p.closePath()
x0,y0,x1,y1=185,118,656,604;r=120
p.moveTo((x0+r,y0));p.lineTo((x1-r,y0));p.qCurveTo((x1,y0),(x1,y0+r));p.lineTo((x1,y1-r));p.qCurveTo((x1,y1),(x1-r,y1));p.lineTo((x0+r,y1));p.qCurveTo((x0,y1),(x0,y1-r));p.lineTo((x0,y0+r));p.qCurveTo((x0,y0),(x0+r,y0));p.closePath()
f['glyf'][f.getBestCmap()[48]]=p.glyph()
p=TTGlyphPen(None)
p.moveTo((70,720));p.lineTo((756,720));p.lineTo((756,595));p.lineTo((349,0));p.lineTo((161,0));p.lineTo((579,594));p.lineTo((70,594));p.closePath()
f['glyf'][f.getBestCmap()[55]]=p.glyph()
p=TTGlyphPen(None)
p.moveTo((70,720));p.lineTo((555,720));p.qCurveTo((755,720),(755,520));p.qCurveTo((755,410),(650,365));p.qCurveTo((755,320),(755,180));p.qCurveTo((755,0),(555,0));p.lineTo((70,0));p.lineTo((70,125));p.lineTo((530,125));p.qCurveTo((620,125),(620,225));p.qCurveTo((620,310),(530,310));p.lineTo((270,310));p.lineTo((270,425));p.lineTo((530,425));p.qCurveTo((620,425),(620,510));p.qCurveTo((620,595),(530,595));p.lineTo((70,595));p.closePath()
f['glyf'][f.getBestCmap()[51]]=p.glyph()
# Circular colon dots, preserving native text layout and live/alarm accessibility.
c=TTGlyphPen(None)
for cy in [165,490]:
 cx=190;r=65;c.moveTo((cx+r,cy));c.qCurveTo((cx+r,cy+r),(cx,cy+r));c.qCurveTo((cx-r,cy+r),(cx-r,cy));c.qCurveTo((cx-r,cy-r),(cx,cy-r));c.qCurveTo((cx+r,cy-r),(cx+r,cy));c.closePath()
f['glyf'][f.getBestCmap()[58]]=c.glyph()
# Keep the clock's visible width/height ratio faithful to the approved display.
for codepoint in range(48,59):
 name=f.getBestCmap().get(codepoint)
 if not name: continue
 glyph=f['glyf'][name]
 if hasattr(glyph,'coordinates'): glyph.coordinates=GlyphCoordinates([(round(x*.9),y) for x,y in glyph.coordinates])
 advance,bearing=f['hmtx'][name];f['hmtx'][name]=(round(advance*.9),round(bearing*.9))
for rec in f['name'].names:
 if rec.nameID in [1,2,3,4,6]: rec.string=({'1':'Desperta Display','2':'Regular','3':'DespertaDisplay-Regular-1','4':'Desperta Display Regular','6':'DespertaDisplay-Regular'}[str(rec.nameID)]).encode(rec.getEncoding())
f.save('app/src/main/assets/fonts/DespertaDisplay-Regular.ttf')
