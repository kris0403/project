package anran.hdcode2.physical

import java.util.concurrent.Semaphore
import anran.hdcode2.library.PhysicalProperty

class Frame[T:ClassManifest](phProp:PhysicalProperty){
  //mutex for not change a not-available frame.
  private val mutex=new Semaphore(1) 
  
  //all blocks include disabled blocks
  private val blocks:Array[Block[T]]=Array.fill[Block[T]](phProp.BlockCountX*phProp.BlockCountY){new Block[T](phProp)}
  
  //The ID of this frame
  var FrameId=0
  
  def GetBlockByPos(y:Int,x:Int)={
    if(y<phProp.BlockCountY&&x<phProp.BlockCountX)blocks(y*phProp.BlockCountX+x)
    else null;
  }
  //enabled block pos by id
  def GetBlockById(id:Int)={
    val pos=phProp.BlockById(id)
    GetBlockByPos(pos.y, pos.x)
  }
  def ForeachBlock(f:(Int,Int,Block[T])=>Unit){
    var i=0
    while(i<phProp.BlockCountX*phProp.BlockCountY){
      f(i/phProp.BlockCountX,i%phProp.BlockCountX,blocks(i))
      i=i+1
    }
  }
  
  def Accquire=mutex.acquire()
  def Release=mutex.release()
}