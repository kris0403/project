package anran.hdcode2.datalink

import anran.hdcode2.library.CaptureProperty

import android.graphics.Point
import anran.hdcode2.physical.PatternLocater
import anran.hdcode2.physical._
import anran.hdcode2.physical.BlockScanner
import anran.hdcode2.library.PixelReader
import anran.hdcode2.library.DataLinkProperty
import anran.hdcode2.physical.SquareLocater
import anran.hdcode2.application.HDCodeReceiverApp
import anran.hdcode2.physical.PatternLocater
import android.util.Log
import anran.hdcode2.PointsView
import anran.hdcode2.CameraView
import anran.hdcode2.library.Utils
import scala.actors.Actor
import anran.hdcode2.library.YUVColor
import anran.hdcode2.GlobalProperty
import anran.hdcode2.NativeBridge
import java.io.FileOutputStream
import java.io.File
import android.graphics.YuvImage
import android.graphics.ImageFormat
import android.graphics.Rect


class ReaderStateMachine(capProp:CaptureProperty,app:HDCodeReceiverApp) {
  //PointsView: Only for test
  //var camerav : CameraView = null
  var pview:PointsView=null
  var tp:Int = 0;
  def addp(x:Int,y:Int)=if(pview==null)Unit else pview.add(x, y)
  def addp(p:Point):Unit=addp(p.x,p.y)
  def clearp=if(pview==null)Unit else pview.clear
  //end
    val trace = new java.io.PrintWriter(new java.io.File("/storage/emulated/0/HDcode/receiver_trace.txt"))
  //A frame for storing frame data
  class ReceiveFrame(dlProp:DataLinkProperty){
    val Blocks=Array.fill(dlProp.PhysicalProperty.BlockAvailableCount){new BlockState(dlProp,null)}  
  }
  var readdone =false
  var started=false
  
  /**
   * Start receiving
   */
  def Start(){
    NativeBridge.startdecode(GlobalProperty.PhysicalProperty.BlockSize,GlobalProperty.PhysicalProperty.BlockSize,
        GlobalProperty.PhysicalProperty.BlockCountX,GlobalProperty.PhysicalProperty.BlockCountY,
        capProp.Width,capProp.Height)
    Utils.openLog
      
    started=true
  }
  /**
   * Stop receiving
   */
  def Stop(){
    WriteTrace()
    trace.close()
    
    Utils.closeLog
      
    started=false
  }
  var dlProp:DataLinkProperty=GlobalProperty.DataLinkProperty
    val fmap=Array.fill(dlProp.RSStrengthFramesCount + 1){false}
  //val fmap=Array.fill(500){false}
   var index = Array.fill(200){0}
  

   def getFid(c:Int):Int={
      if (index(c) == 0)
      {
       return -1
      }
      System.out.println("index(c)= "+index(c))
      var out:Int=0
      var seqs=Array.fill(200){-1}
      var fidCount=Array.fill(200){0}
      var output:String = ""
      for(i <- 0 until index(c)){        
        var bit0 = data_block(c * 117 + i)(0)
        var bit1 = data_block(c * 117 + i)(1)
        var bit2 = data_block(c * 117 + i)(2)
        var bit3 = data_block(c * 117 + i)(3)
        if(bit0 >= 4)
          bit0 = bit0 - 4
        bit0 = bit0 << 6
        if(bit1 >= 4)
          bit1 = bit1 - 4
        bit1 = bit1 << 4
        if(bit2 >= 4)
          bit2 = bit2 - 4
        bit2 = bit2 << 2
        if(bit3 >= 4)
          bit3 = bit3 - 4
        var fid:Int = bit0 + bit1 + bit2 + bit3
        out=fid
        System.out.println("fid:"+ fid.toString())
        if (fid > dlProp.RSStrengthFramesCount)
          fid = 199
        fidCount(fid) += 1
        output += fid.toString() + " "
      }
      var max_fid = 0
      for(i <- 1 until dlProp.RSStrengthFramesCount){
        if (fidCount(max_fid) < fidCount(i))
          max_fid = i
      }
      
      if (max_fid == 199) 
        println(output)
      if (!fmap(max_fid)){
        tp = tp + 1
        fmap(max_fid) = true        
        return max_fid
      }
      else  
      {
        if(readdone==false)
        return -1
        else
        return max_fid
      }
     }
  def WriteTrace(){
      readdone=true
     trace.write("Count: "+count +"\r\n")
    for(k <- 0 until count){
      var temp3 = getFid(k)
       trace.write("FID " +temp3 +"\r\n")
       System.out.println(index(k));
      for(i <- 0 until index(k)){
        var temp1 = res_array(k)(i)
        var temp2 = time_array(k * 117 + i)
        
        trace.write("raw data " + f"$temp1%3s" + "  " + f"$temp2%6s" + "  ")
        for(j <- 0 until array_length){
          trace.write(data_block(k * 117 + i)(j) +  " ")
        }
        trace.write("\r\n")
      }
    }
   trace.flush()

   System.out.println("IN2")
  }
  
  

  val array_length=GlobalProperty.PhysicalProperty.BlockSize*GlobalProperty.PhysicalProperty.BlockSize-3-dlProp.ColorCount-1;
  var elapsed_time = System.currentTimeMillis()
  var data_block = Array.fill(118 * 200, 145){-1}
  var res_array = Array.fill(200, 118){-1}
  var time_array:Array[Long] = new Array[Long](200 * 118)
  //var index = Array.fill(200){0}
  var count = 0

  
  /**
   * Receive raw data from camera
   */
  def HandleFrameData(data:Array[Byte]):()=>Unit={
    
    app.ReceiveMessage(GlobalProperty.DataLinkProperty)

    //fundamental functions
    def abs(x:Int)=if(x<0) -x else x
    
    //EC Layer
    
    object ECBuffer2{
      val finished=Array.fill(256){false}
      def emit(fid:Int){
        Utils.debuginfo("Emit ", fid + " ", 0)
        app.ReceiveData(fid)
      }
      
      def FrameReady(fid:Int){
        if (fid == -1){
          index(count) = 0
          return
        }
        Log.i("FrameReady ", fid.toString())
        count += 1
        emit(fid)
      }
    }
    
   
    
    def callNative(){
      val tick=System.currentTimeMillis()
      NativeBridge.pushdata(data)
      while(true){
        val data_temp:Array[Int]=new Array[Int](array_length)
        var res=NativeBridge.pulldata(dlProp.RSStrengthInFrame*2,data_temp)
        if(res == -1){
          ECBuffer2.FrameReady(getFid(count))
          NativeBridge.releasedata(data)   
          return
        }
        if (res != -2)
        {          
          if (count==200)
          {
            WriteTrace()
            count = 0
            index = Array.fill(200){0}
          }
          res_array(count)(index(count)) = res
          elapsed_time = System.currentTimeMillis()
          //time_array(count * 117 + index(count)) = elapsed_time;
          for(i <- 0 until array_length){
            data_block(count * 117 + index(count))(i) = data_temp(i)
          }
          index(count) += 1
        }
      }
      NativeBridge.releasedata(data)
    }
    
    val point_xs=new Array[Int](300)
    val point_ys=new Array[Int](300)
    
    //new handler
    def resnew():Unit={
      clearp;
      if(started){
        callNative()
        val count=NativeBridge.getpoints(point_xs,point_ys);
        for(i<-0 until count)addp(point_xs(i) / 18 * 27,point_ys(i));
      }
    }
    
    //result handler
      ()=>{resnew();if(pview!=null)pview.invalidate()}
  }
}