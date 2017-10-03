package anran.hdcode2.library

import android.graphics.Point
import android.util.Log
/**
 * read a pixel from the original capture data
 */
class PixelReader(data:Array[Byte],p:CaptureProperty) {
  private val w=p.Width
  private val h=p.Height
  val frameSize=w*h
  val src_data=data
    
  private def getPixelYUV(X:Int,Y:Int):YUVColor={
    if(X>=w||X<0||Y>=h||Y<0)return Utils.debugwarn("[getPixelYUV]Pixel out of bound",null)
    val uvp=frameSize+(Y>>1)*w
    val yp=Y*w+X
    val XA=X&0xfffffffe
    
    def adjust(x:Int)=if(x<0)0 else x
    
    new YUVColor(adjust((0xff&data(yp))-16),
        0xff&data(uvp+XA),
        0xff&data(uvp+XA+1))
  }
  /**
   * get the original color information of given coordinate
   */
  def GetPixelYUV(X:Int,Y:Int)=getPixelYUV(X,Y)
  /**
   * only get the luminance of given coordinate
   */
  def GetPixelY(X:Int,Y:Int)={
    val uvp=frameSize+(Y>>1)*w
    val yp=(0xff&data(Y*w+X))-16
    yp
  }
  
  private val mem=new MemoryBuffer(200)({new YUVColor(0,0,0)})
  /**
   * get the original color information of given coordinate
   * using a allocated memory(to avoid GC) 
   */
  def GetPixelYUVBuffered(p:Point):YUVColor=GetPixelYUVBuffered(p.x,p.y)
  def GetPixelYUVBuffered(X:Int,Y:Int)=GetPixelYUVTo(X,Y,mem.newObj)
  def GetPixelYUVTo(X:Int,Y:Int,dest:YUVColor):YUVColor={
    if(X>=w||X<0||Y>=h||Y<0)return Utils.debugwarn("[getPixelYUV]Pixel out of bound",null)
    
    val uvp=frameSize+(Y>>1)*w
    val yp=Y*w+X
    val XA=X&0xfffffffe
    
    //def adjust(x:Int)=if(x<0)0 else x
    
    dest.Y=((0xff&data(yp))-16)
    dest.Y=if(dest.Y<0)0 else dest.Y
    dest.U=0xff&data(uvp+XA)
    dest.V=0xff&data(uvp+XA+1)
    
    dest
  }
  def GetPixelYUVTo(p:Point,dest:YUVColor):YUVColor=GetPixelYUVTo(p.x,p.y,dest);
  
  private val gfactor=Array(1,2,1,2,4,2,1,2,1)
  private val neighbour=Array((-1,-1),(0,-1),(1,-1),(-1,0),(0,0),(1,0),(-1,1),(0,1),(1,1))
  private val gtot=16
  /**
   * get a 9-pixel Gauss color for given coordinate
   */
  def GetPixelGaussYUVBuffered(p:Point):YUVColor=GetPixelGaussYUVBuffered(p.x, p.y)
  def GetPixelGaussYUVBuffered(x:Int,y:Int)={
    val dest=mem.newObj
    dest.Clear()
    
    def mix(colors:Array[YUVColor],i:Int):YUVColor={
      dest.Y+=gfactor(i)*colors(i).Y
      dest.U+=gfactor(i)*colors(i).U
      dest.V+=gfactor(i)*colors(i).V
      
      if(i==0){
        dest.Y/=gtot
        dest.U/=gtot
        dest.V/=gtot
        
        dest
      }
      else mix(colors,i-1)
    }
    (x>0,y>0,x<p.Width-1,y<p.Height-1) match{
      case (true,true,true,true)=>
        mix(neighbour.map{
              case (dx,dy)=>GetPixelYUVBuffered(dx+x,dy+y)
            },8)
      case _=>Utils.debugwarn("[getPixelYUV]Pixel out of bound",null)
    }
  }
}