package anran.hdcode2.library
/**
 * Capture properties that used in receiver
 */
class CaptureProperty(width:Int,height:Int) {
  val Width=width;
  val Height=height;
  
  val MinSide=math.min(Width,Height)
  
  val IsPatternColor1=(x:YUVColor)=>x.Y<50
  val IsPatternColor2=(x:YUVColor)=>(x.Y>=64 && x.U<x.V)
  val IsPatternColor3=(x:YUVColor)=>(x.Y>=64 && x.U>=x.V)
}