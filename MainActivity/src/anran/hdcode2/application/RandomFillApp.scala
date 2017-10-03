package anran.hdcode2.application

import java.util.Random
import anran.hdcode2.datalink.FrameConstructor
import anran.hdcode2.library.PresentProperty
import android.graphics.Bitmap

class RandomFillApp(prop:PresentProperty,dest:Bitmap,seed:Int,frameComplete:Bitmap=>Unit) {
  def Start(){
    /*val r:Random=new Random(seed)
    val cons=new FrameConstructor(prop,frameComplete)
    
    val writer=cons.Start(dest)
    for(i<-0 to 10000)
      writer(i%255)
    writer(-1)*/
  }
}