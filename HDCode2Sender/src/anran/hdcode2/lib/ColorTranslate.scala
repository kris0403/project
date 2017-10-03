package anran.hdcode2.lib
/**
 * translate between RGB and YUV color space
 */
object ColorTranslate {
	def RGB2YUV(R:Int,G:Int,B:Int)=(0.299*R+0.587*G+0.114*B,
	    -0.147*R-0.289*G+0.436*B,
	    0.615*R-0.515*G-0.1*B);
	def YUV2RGB(Y:Int,U:Int,V:Int)=(adjust(Y+1.140*V),
	    adjust(Y-0.394*U-0.581*V),
	    adjust(Y+2.032*U));
	
	private def a2(x:Int)=if(x<255)x else 255
	private def adjust(x:Double)=a2(if(x<0) 0 else x.asInstanceOf[Int]);
}