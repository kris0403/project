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

class CameraView(context:android.content.Context,attset:android.util.AttributeSet) extends SurfaceView(context,attset) with SurfaceHolder.Callback{
  var camera:Camera=null
  val surfaceHolder = getHolder();
  surfaceHolder.addCallback(this);
  val buffer=new Array[Byte](720*1280*2)
  var started=false
  var runed=false
  
  var widescreen:Boolean = false
  var seq:Int = 0
  
  var activity:Activity=null
  def setActivity(act:Activity){activity=act}
  def testSpeed(data:Array[Byte])={
    val preader=new PixelReader(data,GlobalProperty.CaptureProperty)
    val dataarr=Array.fill(GlobalProperty.CaptureProperty.Height,GlobalProperty.CaptureProperty.Width)(0)
    val currt=System.currentTimeMillis()
    var i=0
    var j=0
    while(i<GlobalProperty.CaptureProperty.Height){
      j=0
      while(j<GlobalProperty.CaptureProperty.Width)
      {
        val black=preader.GetPixelY(j, i)
        if(black<60)
          dataarr(i)(j)=1
          else
            dataarr(i)(j)=0
        j+=2
      }
      i+=2
    }
    Log.i("IterTime",System.currentTimeMillis()-currt+"")
    dataarr
  }
  override def surfaceChanged(holder:SurfaceHolder,format:Int, width:Int,height:Int){
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
          if (seq >= 100 && seq < 500)
          {
            val file:File = new File("/storage/emulated/0/HDcode/Capture/Capture" + (seq - 100).toString() + ".txt")
            val fos:FileOutputStream = new FileOutputStream(file)
            fos.write(data)
            fos.close()
          }
//          else
//          {
//            camera.stopPreview()
//            currstate = 1
//            updateUI()
//          }
          seq = seq + 1
          //app.PushOriginalData()
          updateUI()
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
        //Log.i("progress",d+"")
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