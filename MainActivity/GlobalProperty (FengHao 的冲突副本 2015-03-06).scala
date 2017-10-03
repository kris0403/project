package anran.hdcode2

import anran.hdcode2.library.PhysicalProperty
import anran.hdcode2.library.DataLinkProperty
import anran.hdcode2.library.PresentProperty
import anran.hdcode2.library.CaptureProperty
import android.util.Log

object GlobalProperty {
  //shape customization
  
  val SHAPECUSTOM=false
  
  val shape=
      "_________,"+
      "__XX_XX__,"+
      "_XXXXXXX_,"+
      "XXXXXXXXX,"+
      "XXXXXXXXX,"+
      "_XXXXXXX_,"+
      "__XXXXX__,"+
      "___XXX___,"+
      "____X____"
      
  val shapeAlter=
      "X__X___,"+
      "X__X__X,"+
      "X__X___,"+
      "XXXX_XX,"+
      "X__X__X,"+
      "X__X__X,"+
      "X__X__X"
  
  var PhysicalProperty:PhysicalProperty=null//new PhysicalProperty(11,7,12)
  var DataLinkProperty:DataLinkProperty=null//=new DataLinkProperty(3,8,2,PhysicalProperty)
  val PresentProperty=new PresentProperty(6,6,25,228)
  val CaptureProperty=new CaptureProperty(1280,720)
  
  def Initialize(bx:Int,by:Int,dnpar:Int){
    this.PhysicalProperty=new PhysicalProperty(bx,by,12)
    PhysicalProperty.PresentProperty=PresentProperty
  
    this.DataLinkProperty=new DataLinkProperty(dnpar,3,192,2,PhysicalProperty)
  }
  val HEXAGON=false
  
  val TESTFRAME=false
  
  val KeepTrack=true
  
  
  //test native
  Log.i("Native",""+NativeBridge.test(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15));
}