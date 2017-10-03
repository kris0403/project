package anran.hdcode2

import android.view.View
import android.graphics.Canvas
import java.util.ArrayList
import android.graphics.Point
import android.graphics.Paint
import android.graphics.Color

class PointsView(context:android.content.Context,attr:android.util.AttributeSet) extends View(context,attr) {
  val pointx=new ArrayList[Int]()
  val pointy=new ArrayList[Int]()
  
  def add(p:Point){
    pointx.add(p.x)
    pointy.add(p.y)
  }
  def add(x:Int,y:Int){
    pointx.add(x)
    pointy.add(y)
  }
  def clear()
  {
    pointx.clear()
    pointy.clear()
  }
  val paint=new Paint()
  paint.setColor(Color.RED)
   System.out.println(paint+"pointview red")
  paint.setStyle(android.graphics.Paint.Style.FILL_AND_STROKE)
  override def onDraw(canvas:Canvas){
    
    for(i<-0 until pointx.size())
      canvas.drawCircle(pointx.get(i), pointy.get(i), 4, paint)
  }
  
}