package anran.hdcode2.lib

class Block[T:ClassManifest](phProp:PhysicalProperty){
  val OriginalData:Array[T]=new Array[T](phProp.BlockSize*phProp.BlockSize)
  
  private var currentIter=0
  
  var FrameId=0
  var BlockPosition=0
  
  def Write(data:T)={
    if(currentIter==OriginalData.length)Utils.debugwarn("[Block.Write]Out of bound",false)
    else{                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
      OriginalData(currentIter)=data
      currentIter+=1
      true
    }
  }
  def Seek(y:Int,x:Int)={
    if(x<phProp.BlockSize&&y<phProp.BlockSize){
      currentIter=phProp.BlockSize*y+x
      true
    }
    else
      Utils.debugwarn("[Block.Seek]Out of bound: y="+y+", x="+x,false)
  }
  def Read():T={
    if(currentIter>=OriginalData.length)throw Utils.debugwarn("Block.Read]Out of bound", new Exception())
    val res=OriginalData(currentIter)
    currentIter+=1
    res
  }
  
  
}