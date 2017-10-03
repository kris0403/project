package anran.hdcode2

import android.view.View
import android.net.Uri
import android.view.SurfaceView
import android.hardware.Camera
import android.view.SurfaceHolder
import android.util.Log
import android.widget.TextView
import android.widget.FrameLayout
import android.graphics.Color
import android.graphics.Point
import anran.hdcode2.application.FileTransmitApp
import anran.hdcode2.library.CaptureProperty
import scala.actors.Actor
import anran.hdcode2.application.HDCodeReceiverApp
import anran.hdcode2.physical.PatternLocater
import anran.hdcode2.library.PixelReader
import anran.hdcode2.zip.XZip
import android.app.Activity
import android.os.Bundle
import android.content.Intent 
import java.io.FileOutputStream
import java.io.File
import android.graphics.YuvImage
import android.graphics.ImageFormat
import android.graphics.Rect
import java.util.Arrays
import anran.hdcode2.datalink.ReaderStateMachine;
class CameraView(context:android.content.Context,attset:android.util.AttributeSet) extends SurfaceView(context,attset) with SurfaceHolder.Callback{
  var camera:Camera=null
  val surfaceHolder = getHolder();
  surfaceHolder.addCallback(this);
  val buffer=new Array[Byte](1280*720*2)
  var started=false
  var runed=false
  
  var widescreen:Boolean = false
  var seq:Int = 0
  
  var activity:Activity=null
  def setActivity(act:Activity){activity=act}

  var XYZ=new Array[Float](3)
  var Mag:Float = 0
  def setXYZMag(xyz:Array[Float], magnitude:Float){XYZ=xyz; Mag=magnitude;}
  
  override def surfaceChanged(holder:SurfaceHolder,format:Int, width:Int,height:Int){
    new File("/storage/emulated/0/HDcode/Capture/Acc.txt").delete()
    camera.setPreviewDisplay(surfaceHolder)
    val param=camera.getParameters()
    param.setPreviewSize(1280,720)
    param.setRecordingHint(true)
    param.set("ISO-value", "ISO1600")
    camera.setParameters(param)
    camera.addCallbackBuffer(buffer)
    camera.setPreviewCallbackWithBuffer(new android.hardware.Camera.PreviewCallback(){
      override def onPreviewFrame(data:Array[Byte],camera:Camera){
        camera.addCallbackBuffer(buffer)
        
        if(started){
          System.out.println(seq+" seq")
          if (seq >= 40 && seq < 240)//240)
          {
            
            val file:File = new File("/storage/emulated/0/HDcode/Capture/Capture" + (seq - 40).toString() + ".csv")
            val fos:FileOutputStream = new FileOutputStream(file)
           fos.write(data)
            fos.close()
            if (true)
            {
                var log:String=String.valueOf(XYZ(0)) + "," + String.valueOf(XYZ(1)) + "," + String.valueOf(XYZ(2)) + "," + String.valueOf(Mag) + "\r\n"
             
                val file2:File = new File("/storage/emulated/0/HDcode/Capture/Acc.txt")
                val fos2:FileOutputStream = new FileOutputStream(file2, true)
                fos2.write(log.getBytes())
                fos2.close()
            }
          }
          if(seq==240)
          {
            app.write();
            
          }
          
          else if(seq > 240) {
            System.out.println("Read Done!");
            txtMsg.setText("Read Done!");
            started=false;
            //readerstatemachine.Stop();
            //camera.stopPreview()//SELF CHANGE
            
            //app.write();
          }
          else{
            txtMsg.setText("current state: "+currstate+", current progress: "+seq);
          }
          /*else
          {
            camera.stopPreview()
            currstate = 1
            updateUI()
          }*/
          if(seq<=240)
          seq = seq + 1
          app.PushOriginalData()
          //updateUI()
        }
      }
    }) 
    
    camera.startPreview()
  }
  override def surfaceCreated(holder:SurfaceHolder){
    camera=Camera.open()
    val param=camera.getParameters()
    System.out.println("Camera PreviewFrameRate " + param.getPreviewFrameRate())
    System.out.println("Camera width " + param.getPreviewSize().width)
    System.out.println("Camera height " + param.getPreviewSize().height)
    System.out.println("Camera PreviewFormat " + param.getPreviewFormat())
    System.out.println("Camera BitsPerPixel " + ImageFormat.getBitsPerPixel(param.getPreviewFormat()))
    if (param.getPreviewSize().height / param.getPreviewSize().width != 0.75)
      widescreen = true
  }
  override def surfaceDestroyed(holder:SurfaceHolder){
    camera.stopPreview()
    camera.release()
  }
  private var txtMsg:TextView=null
  private var pview:PointsView=null
  private val capProp=new CaptureProperty(1280,720)
  private var progress:Double=0
  private var currstate:Int=0
  private var centerx=0
  private var centery=0
  private var app:HDCodeReceiverApp=null
  def Start(layout:FrameLayout){
    if(runed)
    {
      layout.removeView(txtMsg)
      layout.removeView(pview)
      currstate = 0
      progress = 0
    }
    started=true
    runed=true
    txtMsg=new TextView(layout.getContext(),attset)
    txtMsg.setTextColor(Color.GREEN)
    txtMsg.setText("current state: "+currstate+", current progress: "+progress)
    
    pview=new PointsView(layout.getContext(),attset)
    app=FileTransmitApp.CreateAsReceiver(capProp, "/storage/emulated/0/HDcode/temp/", "/storage/emulated/0/HDcode/tmp/", d=>{
        progress=d
        Log.i("progress",d+"")
      },new Actor(){
        def act(){
          while(true){
            receive{
              case p:Point=>pview.add(p)
            }
          }
        }
      start()
    },buffer,(path:String)=>{
      started = false
      var file:String=null
      try{
        file=XZip.UnZipFolder(path, "/storage/emulated/0/HDcode/temp/")
      }catch{
        case t:Throwable=>{Log.e("Zip Error",t.getMessage())}
      }
      if(file==null){
      val bundle=new Bundle()
      bundle.putString("Receive", "")
      val intent=new Intent(activity,classOf[FileList])
      intent.putExtras(bundle)
      activity.startActivity(intent)
      }
      else{
        val show=new Intent(Intent.ACTION_VIEW)
        val uri=Uri.parse("file://"+file)
        if(uri.toString().contains(".txt"))
            show.setDataAndType(uri, "text/plain")
        if(uri.toString().contains(".png"))
          show.setDataAndType(uri, "image/png")
          
        if(uri.toString().contains(".mp4"))
          show.setDataAndType(uri, "video/mp4")  
          
        activity.startActivity(show)
      }
    },pview)
    layout.addView(txtMsg)
    layout.addView(pview)
    
    camera.autoFocus(new android.hardware.Camera.AutoFocusCallback(){
      def onAutoFocus(ok:Boolean,c:Camera){
        started=true
      }
    })
  }
  private def updateUI(){
    txtMsg.setText("current state: "+currstate+", current progress: "+progress)
    pview.invalidate()
    //pview.clear
  }
  
}