package anran.hdcode2.lib

import java.awt.Color


class YUVColor(y:Int,u:Int,v:Int) {
	var Y=y;
	var U=u;
	var V=v;
	
	private lazy val rgb=ColorTranslate.YUV2RGB(Y, U, V)
	lazy val Argb=rgb match { case (r,g,b)=>new Color(r,g,b)}
	
	final def Dist(yuv:YUVColor)={
	  def p(x:Int)=x*x
	  p(yuv.Y-Y)+p(yuv.U-U)+p(yuv.V-V)
	}
	final def DistUV(yuv:YUVColor)={
	  def p(x:Int)=x*x
	  p(yuv.U-U)+p(yuv.V-V)
	}
	final def Clear(){
	  Y=0
	  U=0
	  V=0
	}
	final def str()="("+Y+","+U+","+V+")"
	final def Assign(src:YUVColor){
	  this.Y=src.Y
	  this.U=src.U
	  this.V=src.V
	}
}