package anran.hdcode2.lib

import java.awt.image.BufferedImage
import java.awt.Color

trait Drawer {
  type DataUnitPainter=(Int,Int,Color)=>Unit
  def GetPainter(bitmap:BufferedImage,px:Int,py:Int):DataUnitPainter
}