package anran.hdcode2.lib

import java.awt.image.BufferedImage
import java.awt.Color
import javax.imageio.ImageIO
import java.io.File


class SquareDrawer(prop:PresentProperty) extends Drawer {
  
  
  def GetPainter(bitmap:BufferedImage,px:Int,py:Int)={
    var colorBuffer=Map[Color,BufferedImage]()
  
    val canvas=bitmap.createGraphics()
    canvas.setColor(Color.WHITE)
    
    canvas.fillRect(0,0,bitmap.getWidth(), bitmap.getHeight())
    
    (y:Int,x:Int,color:Color)=>{
      val tx=px+x*prop.DataUnitWidth;
      val ty=py+y*prop.DataUnitHeight;
      
      colorBuffer.get(color) match{
        case Some(b)=>{
          canvas.drawImage(b, tx, ty, null)
        }
        case None=>{
          val b:BufferedImage=new BufferedImage(prop.DataUnitWidth,prop.DataUnitHeight,BufferedImage.TYPE_INT_ARGB)
          val c=b.createGraphics()
          c.setColor(color)
          c.fillRect(0,0,prop.DataUnitWidth, prop.DataUnitHeight)
          canvas.drawImage(b, tx,ty, null)
          
          //println(color)
          //ImageIO.write(b,"png",new File("test"+c.toString()+".png"))
          colorBuffer+=(color->b)
        }
      }
    }
  }
}