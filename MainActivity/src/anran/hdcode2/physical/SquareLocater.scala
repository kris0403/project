package anran.hdcode2.physical

import android.graphics.Point
import anran.hdcode2.library.CaptureProperty
import anran.hdcode2.library.MemoryBuffer
import android.util.Log

class SquareLocater extends Locater {
  /*def GetLocaterByCorner(lt:Point,rt:Point,rb:Point,lb:Point)=(blockSize:Int)=>((y:Int,x:Int)=>{
    def mkp(a:Int,b:Int)={
      val buffer=mem.newObj
      
      buffer.x=a
      buffer.y=b
      buffer
    }
    
    val fm=blockSize
    val fzxl=x
    val fzyl=y
    val fzx=fm-fzxl
    val fzy=fm-fzyl
    val tpx=(fzx*fzy*lt.x+fzxl*fzy*rt.x+fzx*fzyl*lb.x+fzxl*fzyl*rb.x)/(fm*fm)
    val tpy=(fzx*fzy*lt.y+fzxl*fzy*rt.y+fzx*fzyl*lb.y+fzxl*fzyl*rb.y)/(fm*fm)
    
    mkp(tpx,tpy)
  })
  */
  /**
   * Below are no-allocation methods for accelerating
   */
  var lt:Point=null
  var rt:Point=null
  var rb:Point=null
  var lb:Point=null
  var blockSize=0
  
  override def toString()=lt.toString()+" "+rt.toString()+" "+lb.toString()+rb.toString()+"; "+blockSize
  
  def AssignCorners(lt:Point,rt:Point,rb:Point,lb:Point,blocksize:Int){
    this.lt=lt
    this.rt=rt
    this.rb=rb
    this.lb=lb
    this.blockSize=blocksize
  }
  def AssignBlockSize(blocksize:Int){this.blockSize=blocksize}
  def LocateTo(y:Int,x:Int,dest:Point)={
    
    val fm=blockSize
    val fzxl=x
    val fzyl=y
    val fzx=fm-fzxl
    val fzy=fm-fzyl
    dest.x=(fzx*fzy*lt.x+fzxl*fzy*rt.x+fzx*fzyl*lb.x+fzxl*fzyl*rb.x)/(fm*fm)
    dest.y=(fzx*fzy*lt.y+fzxl*fzy*rt.y+fzx*fzyl*lb.y+fzxl*fzyl*rb.y)/(fm*fm)
    dest
  }
}