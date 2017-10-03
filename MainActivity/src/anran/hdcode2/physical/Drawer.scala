package anran.hdcode2.physical

import android.graphics.Bitmap

trait Drawer {
  type DataUnitPainter=(Int,Int,Int)=>Unit
  def GetPainter(bitmap:Bitmap,px:Int,py:Int):DataUnitPainter
}