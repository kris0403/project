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
class FrameConstructor(dlProp:DataLinkProperty) {
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
      trace.write("\r\n" + frameId + " ")
    
    for(i<-0 until dlProp.ColorCount)
      b.Write(i)
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
    b.Write(0)
    b.Seek(phProp.BlockSize/2-2,phProp.BlockSize/2+2)
    b.Write(1)
    b.Seek(phProp.BlockSize/2+2,phProp.BlockSize/2-2)
    b.Write(2)
    b.Seek(phProp.BlockSize/2+2,phProp.BlockSize/2+2)
    b.Write(3)
    
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
    
    b.Seek(phProp.BlockSize/2-2,phProp.BlockSize/2-1)
    b.Write(4)
    b.Seek(phProp.BlockSize/2-1,phProp.BlockSize/2+2)
    b.Write(5)
    b.Seek(phProp.BlockSize/2+1,phProp.BlockSize/2-2)
    b.Write(6)
    b.Seek(phProp.BlockSize/2+2,phProp.BlockSize/2+1)
    b.Write(7)
    
    b.Seek(1,0)
    writeFB(b,frameId,8)
    
  }
    
  private def writeFB(b:Block[Int],data:Int,l:Int):Boolean={
    if(l==0)true
    else
      b.Write(data%2)&&writeB(b,data/2,l-1)
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
  
  /**
   * write a char to given radix block
   * binary written order is from right to left
   */
  //ColorBit 3 ColorCount 8 ColorsPerByte 2
  def writeC(b:Block[Int],data:Int)={
    var i=dlProp.ColorsPerByte-1
    while(i>=0){
      var c=(data>>(i*dlProp.ColorBit))&(dlProp.ColorCount-1)
      b.Write(c)
      trace.write(c + " ")
      i=i-1
    }
    true
  }
  
  def writeC3(b:Block[Int],data1:Int,data2:Int,data:Int)={
    if (starting < 10)
      println("Rec " + data1 + " " + data2 + " " + data)
      starting += 1
    val data3 = data1 << 16 | data2 << 8 | data
    var i = 8 - 1
    while(i >= 0)
    {
      var c = (data3 >> (i * 3)) & (dlProp.ColorCount-1)
      b.Write(c)
      trace.write(c + " ")
      i = i - 1
    }
    true
  }
  
  def writeC2(b:Block[Int],data1:Int,data:Int)={
    val data2 = data1 << 8 | data
    var i = 4 - 1
    while(i >= 0)
    {
      var c = (data2 >> (i * 3)) & (dlProp.ColorCount-1)
      b.Write(c)
      trace.write(c + " ")
      i = i - 1
    }
    true
  }
  
  //TEST FOR SINGLE FRAME PERFORMANCE
  def TestFrame(dest:BufferedImage)={
    val frame=new Frame[Int](phProp)
    val blockDataUnitOffset=3+dlProp.ColorCount
    val blockDataUnitCount=phProp.BlockSize*phProp.BlockSize-blockDataUnitOffset-1
    val blockAvailableDataUnitCount=blockDataUnitCount-dlProp.RSStrengthInFrame*2*(8/dlProp.ColorBit);
    
    val blockByteCount=blockDataUnitCount/(8/dlProp.ColorBit)
    val blockAvailableByteCount=blockAvailableDataUnitCount/(8/dlProp.ColorBit)
    
    frame.ForeachBlock((i,j,block)=>{
      initBlock(block,0, 0)
      for(i<-0 until blockByteCount)writeC(block,27)
      if(i==phProp.BlockCountY/2&&j==phProp.BlockCountX/2){
        initCenter(block,0)
      }
    })
    val s=new Semaphore(1)
    s.acquire()
    BitmapMaker.Enq(dest, frame, (b,i)=>s.release())
    s.acquire()
    s.release()
    dest
  }
  
  val trace = new java.io.PrintWriter(new java.io.File(".\\sender_trace.txt"))
  
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
    val blockAvailableDataUnitCount=blockDataUnitCount-dlProp.RSStrengthInFrame*2*(8/dlProp.ColorBit);
    
    val blockByteCount=blockDataUnitCount/(8/dlProp.ColorBit)
    val blockAvailableByteCount=blockAvailableDataUnitCount/(8/dlProp.ColorBit)
    
    val blockDataCount = blockByteCount - dlProp.RSStrengthInFrame * 2 -1
    var data1:Int = 0
    var data2:Int = 0
    println("blockDataCount " + blockDataCount)
    
    val currRSWindow=new Array[Int](blockByteCount)
    val totalRSWindow=Array.fill(phProp.BlockAvailableCount){Array.fill(blockByteCount){0}}
    val parityBlocks=Array.fill(dlProp.InterBlockECCount){Array.fill(blockByteCount){0}}
    
    //four bits frame id, 2 bits window id
    //def getBlockMeta=(currFrame&255)//(currentFrameId<<4)|((currFrame/dlProp.RSStrengthFramesCount)&3)|(if(currFrame/dlProp.RSStrengthFramesCount==0)4 else 0)
      
    def blchg{if(bclistener!=null)bclistener(currFrame,currBlock)}
    def fmchg{if(fclistener!=null)fclistener(currFrame)}
    def wdchg{if(wclistener!=null)wclistener()}
    
    //Total bytes processed count  
    var count=0
    val random=new Random()
    
    //get reversed parity block map
    val parityMap=(0 until phProp.BlockAvailableCount).map(id=>{
      val set=new HashSet[Int]()
      for(i<-0 until dlProp.InterBlockECCount)
        for(j<-0 until dlProp.InterBlockParity(i).length)if(dlProp.InterBlockParity(i)(j)==id)
          set.add(i)
      set.toArray
    }).toArray
    
    
    //return function
    //meta==false indicates that there should be insert a meta byte if it's the first symbol of a block
    def insertByteToBlock(data:Int,meta:Boolean=false):Boolean={
      if(!started)return false
      if(data == -1){
        //fill all data units in this window
        while(!(currentFrameId==0 && currBlock==0 && currPos==0)){
          
          insertByteToBlock(random.nextInt(256))
        }
        started=false
        return true
      }
      count+=1
      
      //if(!meta&&currPos==0)insertByteToBlock(getBlockMeta,true)
      
      if (writing_bid != currBlock)
      {
        trace.write("\r\n currBlock " + currBlock + "\r\n")
        writing_bid = currBlock
      }
      val centerid=phProp.CenterBlockId
      if (currPos % 3 == 0)
        data1 = data
      if (currPos % 3 == 1)
        data2 = data
      if (currPos % 3 == 2)
      {
        trace.write(" data3 ")
        writeC3(window(currentFrameId).GetBlockById(currBlock),data1,data2,data)
      }
      if (currPos == 39)
      {
        trace.write(" data2 ")
        writeC2(window(currentFrameId).GetBlockById(currBlock),data,0)
        currPos += 1
      }
      //trace.write(" data ")
      //writeC(window(currentFrameId).GetBlockById(currBlock),data)
      
      currRSWindow(currPos)=data
      totalRSWindow(currBlock)(currPos)^=data
      for(i<-parityMap(currBlock))
        parityBlocks(i)(currPos)^=data
        
      currPos+=1
      
      def makeLastBit(arr:Array[Int]){
        arr(blockAvailableByteCount-1)=0
        for(i<-0 until blockAvailableByteCount-1)
          arr(blockAvailableByteCount-1)=(arr(blockAvailableByteCount-1)+arr(i)+1)%256
      }
      
      if(currPos>=blockAvailableByteCount-1){//out of data units
        makeLastBit(currRSWindow)
//        if (currBlock == 0)
//        {
//          println("currRSWindow")
//          for(i<-currRSWindow)
//            print(i + " ")
//          println("")
//        }
        //encoderb.encode(currRSWindow)
//        if (currBlock == 0)
//        {
//          println("encode(currRSWindow)")
//          for(i<-currRSWindow)
//            print(i + " ")
//          println("")
//        }
        while(currPos<blockByteCount){
          trace.write(" currRSWindow ")
          writeC(window(currentFrameId).GetBlockById(currBlock),currRSWindow(currPos))
          currPos+=1
        }
        window(currentFrameId).GetBlockById(currBlock).Seek(0,0)
        currPos=0
        currBlock+=1
        blchg
        
        //check if the new block is parity-check block
        for(i<-0 until dlProp.InterBlockECCount){
          val arr=dlProp.InterBlockParity(i)
          if(arr(arr.length-1)==currBlock){
            //this block is parity-check block
            initBlock(window(currentFrameId).GetBlockById(currBlock),currFrame, 1)
            //parityBlocks(i)(0)=getBlockMeta
            makeLastBit(parityBlocks(i))
            //encoderb.encode(parityBlocks(i))
            
            //now currPos==0
            while(currPos<blockByteCount){
              totalRSWindow(currBlock)(currPos)^=parityBlocks(i)(currPos)
              for(k<-parityMap(currBlock))
                if(k!=i)
                  parityBlocks(k)(currPos)^=parityBlocks(i)(currPos)
              if (writing_bid != currBlock)
              {
                trace.write("\r\n currBlock " + currBlock + "\r\n")
                writing_bid = currBlock
              }
              trace.write(" parityBlocks ")
      
              writeC(window(currentFrameId).GetBlockById(currBlock),parityBlocks(i)(currPos))
              parityBlocks(i)(currPos)=0
              currPos+=1
            }
            currBlock+=1
            currPos=0
            blchg
          }
        }
        
        if(currBlock==centerid){//center
          initCenter(window(currentFrameId).GetBlockById(currBlock),currentFrameId)
          currBlock+=1
          
          blchg
        }
        else{
          if(currBlock==phProp.BlockAvailableCount){//out of blocks
            BitmapMaker.Enq(createBitmap(), window(currentFrameId), onFinish)
            currBlock=0
            currFrame+=1
            fmchg
            blchg
            
            BitmapMaker.WaitFrame(window(currentFrameId))
          }
        }
        initBlock(window(currentFrameId).GetBlockById(currBlock),currFrame, 1)
        
      }
      true
    }
    
    
    (data:Int)=>{
      insertByteToBlock(data)
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