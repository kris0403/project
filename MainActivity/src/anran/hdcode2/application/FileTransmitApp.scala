package anran.hdcode2.application

import anran.hdcode2.library.PresentProperty
import anran.hdcode2.physical.BlockState
import java.io.File
import java.io.IOException
import scala.actors.Actor
import anran.hdcode2.datalink.FrameConstructor
import anran.hdcode2.library.DataLinkProperty
import anran.hdcode2.library.FileTransmitProperty
import anran.hdcode2.library.FileTypes
import anran.hdcode2.datalink.ReaderStateMachine
import anran.hdcode2.library.CaptureProperty
import android.util.Log
import java.util.Random
import android.graphics.Point
import anran.hdcode2.PointsView
import anran.hdcode2.library.Utils
import anran.hdcode2.GlobalProperty

object FileTransmitApp{
  def CreateAsReceiver(capProp:CaptureProperty,destFolder:String,tmpPath:String,prog:Double=>Unit,msgActor:Actor,data:Array[Byte],onFinish:String=>Unit,pview:PointsView)=new HDCodeReceiverApp(){
    private val stateMachine=new ReaderStateMachine(capProp,this)
    stateMachine.pview=pview
    stateMachine.Start
    val handler=stateMachine.HandleFrameData(data)

    private var dlProp:DataLinkProperty=GlobalProperty.DataLinkProperty
    private val receivedFrameCount=new java.util.concurrent.atomic.AtomicInteger(0)
    
    def PushOriginalData()=handler()
    def write=stateMachine.WriteTrace
    private def readWindowId(b:BlockState)=b.Data(5)&127
    
    private var currentWid = -1
    private var currentWindowId=0
    
  var recCount:Int = 0
    
    def ReceiveData(frameId:Int){
      //if(blockId==0){
        //update window id
      //  currentWindowId=readWindowId(blockdata)
      //  currentWid=wid
      //  Log.i("ReceiveData","UpdateFT: "+currentWindowId+" "+currentWid)
      //}
      
      val newcount:Double=receivedFrameCount.incrementAndGet()
      prog(newcount/dlProp.RSStrengthFramesCount)
      //println("Rec count : " + newcount)
      
      if(receivedFrameCount.compareAndSet(dlProp.RSStrengthFramesCount,dlProp.RSStrengthFramesCount+1)){
        val finalpath=destFolder+"/dest.zip"//+ftProp.getExtension()
          //stateMachine.Stop
          //if(onFinish!=null)onFinish(finalpath)
      }
        //if (!fmap(frameId))
        {
          //fmap(frameId) = true
          recCount += 1
        }
        if (recCount == dlProp.RSStrengthFramesCount)
        {
          val finalpath=destFolder+"/dest.zip"//+ftProp.getExtension()
          stateMachine.Stop
          if(onFinish!=null)onFinish(finalpath)
        }
    }
    
    def ReceiveMessage(msg:Any):Unit=if(msg==null)return else{
     Log.i("Message received",msg.toString) 
     msg match{
      case (dlProp:DataLinkProperty)=>{
        Log.i("dlProp received", "..");
        if(this.dlProp==null){
          Log.i("dlProp assigned", "...");
          this.dlProp=dlProp 
        }
      }
      case p:Point=>{
        msgActor!p
      }
      case _=>{}
    }
    }
  }
}