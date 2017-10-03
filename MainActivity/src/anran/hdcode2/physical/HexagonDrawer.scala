package anran.hdcode2.physical

import anran.hdcode2.library.PresentProperty
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

/**
 * When using hexagon, the width must be 6 and height must be 5
 */
class HexagonDrawer(prop:PresentProperty) extends SquareDrawer(prop) {
  private val pattern=Array(Array(0,0,1,1,0,0),Array(0,1,1,1,1,0),Array(1,1,1,1,1,1),Array(1,1,1,1,1,1),
      Array(1,1,1,1,1,1),Array(0,1,1,1,1,0),Array(0,0,1,1,0,0))
  
  override def GetPainter(bitmap:Bitmap,px:Int,py:Int)={
    var colorBuffer=Map[Int,Bitmap]()
  
    val canvas=new Canvas(bitmap)
    
    canvas.drawColor(Color.WHITE) 
    (y:Int,x:Int,color:Int)=>{
      val tx=if(y%2==0)px+x*prop.DataUnitWidth else px+x*prop.DataUnitWidth+prop.DataUnitWidth/2;
      val ty=py+y*prop.DataUnitHeight;
      
      colorBuffer.get(color) match{
        case Some(b)=>{
          canvas.drawBitmap(b, tx, ty, null)
        }
        case None=>{
          val b:Bitmap=Bitmap.createBitmap(6,7,Bitmap.Config.ARGB_4444)
          val c:Canvas=new Canvas(b)
          c.drawColor(Color.TRANSPARENT)
          val p=new Paint()
          p.setColor(color)
          for(i<-0 until 7)
            for(j<-0 until 6)
            {
              if(pattern(i)(j)==1)
                c.drawPoint(j,i,p)
            }
          canvas.drawBitmap(b, tx,ty, null)
          colorBuffer+=(color->b)
        }
      }
    }
  }
}