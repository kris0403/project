package anran.hdcode2.physical

import anran.hdcode2.library.PresentProperty
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

class SquareDrawer(prop:PresentProperty) extends Drawer {
  
  
  def GetPainter(bitmap:Bitmap,px:Int,py:Int)={
    var colorBuffer=Map[Int,Bitmap]()
  
    val canvas=new Canvas(bitmap)
    
    canvas.drawColor(Color.WHITE) 
    (y:Int,x:Int,color:Int)=>{
      val tx=px+x*prop.DataUnitWidth;
      val ty=py+y*prop.DataUnitHeight;
      
      colorBuffer.get(color) match{
        case Some(b)=>{
          canvas.drawBitmap(b, tx, ty, null)
        }
        case None=>{
          val b:Bitmap=Bitmap.createBitmap(prop.DataUnitWidth,prop.DataUnitHeight,Bitmap.Config.RGB_565)
          val c:Canvas=new Canvas(b)
          c.drawColor(color)
          canvas.drawBitmap(b, tx,ty, null)
          colorBuffer+=(color->b)
        }
      }
    }
  }
}