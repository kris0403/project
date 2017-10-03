package anran.hdcode2.physical

import android.graphics.Point
import anran.hdcode2.library.DataLinkProperty

class BlockState(dlProp:DataLinkProperty,sumBlock:BlockState) {
  var Error=false
  var LastWindowId=0
  val Data:Array[Int]=new Array[Int]((dlProp.PhysicalProperty.BlockSize*dlProp.PhysicalProperty.BlockSize-4-dlProp.ColorCount)/(8/dlProp.ColorBit))
  
  def Xor(dest:BlockState){
    var i=0
    while(i<Data.length){
      dest.Data(i)^=Data(i)
      i+=1
    }
  }
}