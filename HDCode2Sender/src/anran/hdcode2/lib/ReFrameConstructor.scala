package anran.hdcode2.lib

import scala.actors.Actor
import java.util.concurrent.Semaphore
import java.util.Random
import scala.collection.mutable.HashSet
import java.awt.image.BufferedImage
import jp.sourceforge.reedsolomon.RsEncode
import java.awt.Color

/**
 * put bytes to Bitmaps
 */
class ReFrameConstructor(dlProp:DataLinkProperty) {
  private val phProp=dlProp.PhysicalProperty
  
  private var started=false
  private def createBitmap()=new BufferedImage(dlProp.PhysicalProperty.PresentProperty.TotalWidth(dlProp.PhysicalProperty),
      dlProp.PhysicalProperty.PresentProperty.TotalHeight(dlProp.PhysicalProperty),
      BufferedImage.TYPE_INT_ARGB)
  
  private var bclistener:(Int,Int)=>Unit=null
  private var fclistener:(Int)=>Unit=null
  private var wclistener:()=>Unit=null
  
  private var windowCount = 0
  private var writing_bid = -1
  
  /**
   * export blocks to a bitmap
   */
  object BitmapMaker {
    //shape drawer
    private var drawer:Drawer=new SquareDrawer(phProp.PresentProperty)
    
    //multithread
    private val threadCount=1
    
    //an actor to make bitmaps
    class Maker extends Actor{
      def act(){
        while(true){
          receive{
            //dest: Bitmap
            //source:Frame[Int]
            //onFinish:(dest:Bitmap,FrameId:Int)=>Unit
            case (_dest,_source,_onFinish)=>{
              val dest=_dest.asInstanceOf[BufferedImage]
              val source=_source.asInstanceOf[Frame[Int]]
              val onFinish=_onFinish.asInstanceOf[(BufferedImage,Int)=>Unit]
              
              val painter=drawer.GetPainter(dest, phProp.PresentProperty.MarginLeft, phProp.PresentProperty.MarginTop)
              
              val trace1 = new java.io.PrintWriter(new java.io.File(".\\sender_trace" + windowCount + "(" + source.FrameId + ").txt"))
              windowCount += 1
              source.ForeachBlock((y,x,b)=>{
                if(dlProp.PhysicalProperty.BlockAvailable(y)(x))
                {
                  b.Seek(0,0)
                  def getc(c:Int)=c match {
                    case -3 => phProp.PatternColor3
                    case -2 => phProp.PatternColor2
                    case -1 => phProp.PatternColor1
                    case _ => if(c<dlProp.ColorCount)dlProp.Colors(c).Argb else Color.WHITE
                  }
                  def iter(id:Int):Unit={
                    if(id==phProp.BlockSize*phProp.BlockSize)return
                    
                    val d=b.Read()
                    trace1.write(d + " ")
                    
                    painter(y*phProp.BlockSize+id/phProp.BlockSize,x*phProp.BlockSize+id%phProp.BlockSize,getc(d))
                    iter(id+1)
                  }
                  trace1.write("\r\n" + y + " " + x + "\r\n")
                  
                  iter(0)
                  b.Seek(0,0)
                  
                  //other 3 locators
                  painter((y+1)*phProp.BlockSize,(x+1)*phProp.BlockSize,phProp.PatternColor1)
                  painter(y*phProp.BlockSize,(x+1)*phProp.BlockSize,phProp.PatternColor1)
                  painter((y+1)*phProp.BlockSize,x*phProp.BlockSize,phProp.PatternColor1)
                }
              })
              
              trace1.close()
              onFinish(dest,source.FrameId)
              source.Release
            }
            case _=>Utils.debugwarn("Drawer","unrecognized",Unit)
          }
        }
      }
      start()
    }
    
    private val makers=Array.fill[Maker](threadCount){new Maker()}
    private var currmaker=0
    
    def Enq(dest:BufferedImage,source:Frame[Int],onFinish:(BufferedImage,Int)=>Unit){
      source.Accquire
      makers(currmaker%threadCount)!(dest,source,onFinish)
      currmaker+=1
    }
    def WaitFrame(source:Frame[Int]){
      source.Accquire
      source.Release
    }
  }
  
  private val window:Array[Frame[Int]]=Array.fill[Frame[Int]](dlProp.RSStrengthFramesCount){new Frame(phProp)}
  
  /**
   * write a l-length binary to current position of block b as binary symbols
   * binary written order is from right to left
   */
  private def writeB(b:Block[Int],data:Int,l:Int):Boolean={
    if(l==0)true
    else
      b.Write(-2-data%2)&&writeB(b,data/2,l-1)
  }
  
  /**
   * initialize block b's header
   * must initialize every enabled block in every frame
   */
  private def initBlockHeader(b:Block[Int],frameId:Int){
    //last symbol
    b.Seek(phProp.BlockSize-1, phProp.BlockSize-1)
    writeB(b,frameId,1)
    
    //first symbol
    b.Seek(0,0)
    b.Write(-1)
    
    //then frame id
    writeB(b,frameId,2)
  }
  
  /**
   * initialize data block b by putting color patterns and other informations
   * position seek to the place that will write data
   * initialize once for every block, then use initBlockHeader instead.
   * 
   * Block format(h,w):
   * (0,0):Black pattern
   * (0,1):frameId&1
   * (0,2):frameId&2
   * (0,3..3+colorcount-1):color patterns
   * (0,3+colorcount) to (h-1,w-2):data, totally (w*h-4-colorcount)/baud_per_byte bytes
   *    first byte of data is the meta-data of this block: 0..3 bits are frame id and 4..7 bits are reserved
   * (h-1,w-1):frameId&1
   * 
   */
  private def initBlock(b:Block[Int],frameId:Int, func:Int){
    initBlockHeader(b,frameId)
    if (func != 0)
      trace.write("\r\nframeId " + frameId + " ")
    
    for(i<-0 until dlProp.ColorCount / 2)
      b.Write(3)
    for(i<-(dlProp.ColorCount / 2) until dlProp.ColorCount)
      b.Write(0)
      
    b.Seek(0,3 + dlProp.ColorCount)
    var data = frameId
    data /= 4
    data /= 4
    data /= 4
    b.Write(data%4)
    data = frameId
    data /= 4
    data /= 4
    b.Write(data%4)
    data = frameId
    data /= 4
    b.Write(data%4)
    b.Write(frameId%4)
  }
  
  /**
   * The black symbol coordinates within the center locator
   */
  private val neighbour2=if(!GlobalProperty.HEXAGON)/*Set((0,0),
          (0,2),(1,2),(2,2),(2,1),(2,0),
          (2,-1),(2,-2),(1,-2),(0,-2),(-1,-2),
          (-2,-2),(-2,-1),(-2,0),(-2,1),(-2,2),(-1,2))*/
          Set((-1,-1),(-1,0),(-1,1),(0,-1),(0,0),(0,1),(1,-1),(1,0),(1,1),
              (-3,-3),(-3,-2),(-3,-1),(-3,0),(-3,1),(-3,2),(-3,3),
              (-2,-3),(-2,3),(-1,-3),(-1,3),(0,-3),(0,3),(1,-3),(1,3),(2,-3),(2,3),
              (3,-3),(3,-2),(3,-1),(3,0),(3,1),(3,2),(3,3)
          )
      else Set((0,0),
          (-1,-2),(0,-2),(1,-2),
          (-2,-1),(1,-1),
          (-2,0),(2,0),
          (-2,1),(1,1),
          (-1,2),(0,2),(1,2),
          
          (-1,-1),(0,-1),
          (-1,1),(0,1))
          
  /**
   * The white symbol coordinates within the center locator
   */
  private val neighbour3=if(!GlobalProperty.HEXAGON)Set(/*(-1,-1),(-1,1),(0,1),(1,1),(1,0),(1,-1),(0,-1)*/(-2,-1),(-2,0),(-2,1),(-1,-2),(-1,2),(0,-2),(0,2),(1,-2),(1,2),(2,-1),(2,0),(2,1))
                         else                       Set(/*(-1,-1),(0,-1),*/(1,0)/*,(-1,1),(0,1)*/)
  /**
   * initialze center block b by putting frame configurations
   * must initialize in every frame (frame ID changed)
   * 
   * Block format(h,w):
   * (0,0):black pattern
   * (0,1):frameId&1
   * (0,2):frameId&2
   * 
   * (1,0-8):frameId
   * 
   * (h-1,w-1):frameId&1
   * 
   */
  private def initCenter(b:Block[Int],frameId:Int){
    initBlockHeader(b,frameId)
    
    //eight symbols directs the perspective
    b.Seek(phProp.BlockSize/2-2,phProp.BlockSize/2-2)
    b.Write(3)
    b.Seek(phProp.BlockSize/2-2,phProp.BlockSize/2+2)
    b.Write(2)
    b.Seek(phProp.BlockSize/2+2,phProp.BlockSize/2-2)
    b.Write(1)
    b.Seek(phProp.BlockSize/2+2,phProp.BlockSize/2+2)
    b.Write(0)
    
    //do write center locators
    neighbour3.foreach{case (x,y)=>{
      b.Seek(phProp.BlockSize/2+y,phProp.BlockSize/2+x)
      b.Write(1024)
    }
    }
    neighbour2.foreach{case (x,y)=>{
      b.Seek(phProp.BlockSize/2+y,phProp.BlockSize/2+x)
      b.Write(-1)
    }
    }
    
    b.Seek(1,0)
    writeFB(b,frameId,8)
    
  }
    
  private def writeFB(b:Block[Int],data:Int,l:Int):Boolean={
    if(l==0)true
    else
    {
      if (l < 4)
        b.Write(0)&&writeFB(b,0,l-1)
      else
        b.Write(data%4)&&writeFB(b,data/4,l-1)
    }
  }
    
  /**
   * initialize every windows
   * used in first time (started==false)
  */
  private def initWindowData()
  {
    for(i<-0 until window.length){
      val w=window(i)
      w.FrameId=i
      w.ForeachBlock((y,x,b)=>{
        initBlock(b,0, 0)
      })
    }
  }
  
  val trace = new java.io.PrintWriter(new java.io.File(".\\sender_trace.txt"))
  trace.write("\r\nframeId 0 ")
  
  var starting:Int = 0
  /**
   * Main function
   * return a function that receive a byte, outputs a boolean
   * if the byte<0 then Stop() will be called and the stream is closed. 
   * once a Bitmap is full, onFinish will be called with a (Bitmap,FrameID) parameter
   */
  def Start(onFinish:(BufferedImage,Int)=>Unit)=
  {
    initWindowData()
    started=true
    
    var currBlock=0
    var currFrame=0
    var currPos=0
    
    def currentFrameId=currFrame%dlProp.RSStrengthFramesCount
    
    val blockDataUnitOffset=3+dlProp.ColorCount
    val blockDataUnitCount=phProp.BlockSize*phProp.BlockSize-blockDataUnitOffset-1
    
    val blockByteCount=blockDataUnitCount/(8/dlProp.ColorBit)
    
    def blchg{if(bclistener!=null)bclistener(currFrame,currBlock)}
    def fmchg{if(fclistener!=null)fclistener(currFrame)}
    def wdchg{if(wclistener!=null)wclistener()}
    
    //return function
    def insertSymbolToBlock(data:Int):Boolean={
      if(!started)return false
      
      if (writing_bid != currBlock)
      {
        trace.write("currBlock " + currBlock + "\r\n")
        writing_bid = currBlock
      }
      val centerid = phProp.CenterBlockId
      window(currentFrameId).GetBlockById(currBlock).Write(data)
      trace.write(data + " ")
      //writeC(window(currentFrameId).GetBlockById(currBlock),data)
      currPos+=1
      
      if(currPos == phProp.BlockSize*phProp.BlockSize-dlProp.ColorCount-4-4){//out of data units
        currPos=0
        currBlock+=1
        
        blchg
        
        if(currBlock == centerid){//center
          initCenter(window(currentFrameId).GetBlockById(currBlock),currentFrameId)
          currBlock+=1
          
          blchg
        }
        else{
          if(currBlock == phProp.BlockAvailableCount){//out of blocks
            BitmapMaker.Enq(createBitmap(), window(currentFrameId), onFinish)
            currBlock=0
            currFrame+=1
            window(currentFrameId).GetBlockById(currBlock).Seek(0,0)
            fmchg
            blchg
            
            BitmapMaker.WaitFrame(window(currentFrameId))
          }
        }
        initBlock(window(currentFrameId).GetBlockById(currBlock),currFrame, 1)
        //println("initBlock " + currentFrameId + " " + currFrame)
      }
      true
    }
    
    
    (data:Int)=>{
      insertSymbolToBlock(data)
    }
  }
  /**
   * Stop the output stream
   */
  def Stop()
  {
    if(!started)return
    started=false
  }
  
  def trace_close()
  {
    trace.close()
  }
  
  def RegisterBlockChangeListener(listener:(Int,Int)=>Unit){bclistener=listener}
  def RegisterFrameChangeListener(listener:(Int)=>Unit){fclistener=listener}
  def RegisterWindowChangeListener(listener:()=>Unit){wclistener=listener}
}