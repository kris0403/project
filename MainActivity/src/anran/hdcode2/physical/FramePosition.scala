package anran.hdcode2.physical

import android.graphics.Point
import anran.hdcode2.library.PhysicalProperty
import anran.hdcode2.library.Utils

class Position

case object LT extends Position
case object RT extends Position
case object LB extends Position
case object RB extends Position

object BlackClassifier{
  private var t_mid=50
  private var t_width=5
  
  def GetTypicalBlackClassifier(mid:Int=t_mid,width:Int=t_width)=new BlackClassifier(mid,width)
  def SaveToTypicalBlackClassifier(bc:BlackClassifier){
    t_mid=bc.M
    t_width=bc.W
  }
}
class BlackClassifier(initmid:Int,initwidth:Int){
  var M=initmid
  var W=0//initwidth
  
  def GetProbMul4(y:Int)={
    //for reduce time:
    if(y<M-(W))4
    else if(y>M+(W))0
    else 2
  }
  def IsBlack(y:Int)=y<M-(W)
  def IsNonBlack(y:Int)=y>M+(W)
  
  def weight=2
  def adjust(blacky:Int,nony:Int){
    M=(M*weight+(3*blacky+nony)/4)/(weight+1)
    //W=(W*weight+(nony-blacky)/4)/(weight+1)
    //Utils.debuginfo("BC_adjust",M+" "+W,0)
  }
  
  def GetProb(y:Int)=GetProbMul4(y)/4.0
  
  def Reset(){
    M=initmid
    W=0//initwidth
  }
}

object BlockPosition{
  val DISABLED=0
  val NOTLOCATED=2
  val NOTADJUSTED=3
  val TOLOCATE=4
  val TOADJUST=5
  val LOCATED=8
  val FAILED=16
  
  def isTo(b:BlockPosition)={
    if((b.State&4)>0)false
    else if((b.State&2)>0){
      b.State+=2
      true
    }
    else
      false
  }
}
class BlockPosition(state:Int,ltPosition:Point)
{
  var State=state
  def Ready=(State==BlockPosition.LOCATED)
  val LTPosition=ltPosition
  val BClassifier=BlackClassifier.GetTypicalBlackClassifier()
  //var Adjust:Int=0
}



class FramePosition(phProp:PhysicalProperty) {
  val XCount=phProp.BlockCountX
  val YCount=phProp.BlockCountY
  
  
  val BlockPositions=Array.fill(XCount+1,YCount+1){new BlockPosition(BlockPosition.DISABLED,new Point(0,0))}
  
  def GetBlockPosition(bx:Int,by:Int)=(p:Position)=>{
    def get(x:Int,y:Int)=if(BlockPositions(x)(y).Ready)BlockPositions(x)(y).LTPosition else null
    p match {
      case LT=>get(bx,by)
      case RT=>get(bx+1,by)
      case LB=>get(bx,by+1)
      case RB=>get(bx+1,by+1)
      
    }
  }
  def GetCenterBlockPosition(p:Position)={
    GetBlockPosition(XCount/2,YCount/2)(p)
  }
  def SetBlockPosition(bx:Int,by:Int)=(p:Position,px:Int,py:Int)=>{
    def update(x:Int,y:Int)={
      BlockPositions(x)(y).LTPosition.x=px
      BlockPositions(x)(y).LTPosition.y=py
      BlockPositions(x)(y)
    }
    p match {
      case LT=>update(bx,by)
      case RT=>update(bx+1,by)
      case LB=>update(bx,by+1)
      case RB=>update(bx+1,by+1)
    }
  }
  
  for(i<-0 until phProp.BlockAvailableCount){
    val p=phProp.BlockById(i)
    BlockPositions(p.x)(p.y).State=BlockPosition.NOTLOCATED
    BlockPositions(p.x+1)(p.y).State=BlockPosition.NOTLOCATED
    BlockPositions(p.x)(p.y+1).State=BlockPosition.NOTLOCATED
    BlockPositions(p.x+1)(p.y+1).State=BlockPosition.NOTLOCATED
  }
  
  def Refresh()={
    def ref(p:BlockPosition)={
      if(p.State==BlockPosition.FAILED||p.State==BlockPosition.NOTLOCATED)p.State=BlockPosition.NOTLOCATED
      else if(p.State!=BlockPosition.DISABLED)p.State=BlockPosition.NOTADJUSTED
    }
    for(i<-BlockPositions){
      for(j<-i)
        ref(j)
    }
  }
}