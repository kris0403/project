package anran.hdcode2.physical

import anran.hdcode2.library.DataLinkProperty
import anran.hdcode2.library.PixelReader
import android.graphics.Point
import anran.hdcode2.library.ColorMatcher
import anran.hdcode2.library.CaptureProperty
import jp.sourceforge.reedsolomon.RsDecode
import anran.hdcode2.library.PhysicalProperty
import anran.hdcode2.library.YUVColor
import anran.hdcode2.library.MemoryBuffer
import android.util.Log
import anran.hdcode2.library.Utils
import anran.hdcode2.GlobalProperty
/**
 * read a block(center or data)
 */
class BlockScanner(capProp:CaptureProperty) {
  private val destp=new Point()
  

    
  type DataUnitLocater=Int=>((Int,Int)=>Point)
  /**
   * read the center block's block size
   * note that block size is between [10,16]
   */
  def ReadBlockSize(pixelReader:PixelReader,squareReader:SquareLocater)={
    squareReader.AssignBlockSize(4)
    val p1=squareReader.LocateTo(0,2,destp)
    val c1=pixelReader.GetPixelGaussYUVBuffered(p1.x, p1.y)
    
    val p2=squareReader.LocateTo(2,0,destp)
    val c2=pixelReader.GetPixelGaussYUVBuffered(p2.x, p2.y)
    
    val bsize=10+((ColorMatcher.MatchPatternColor(capProp, c1)<<2)|(ColorMatcher.MatchPatternColor(capProp, c2)<<1))
    bsize
  }
  /**
   * parse the center block's containing data to DataLinkProperty
   * you must guarantee that the data is correct
   */
  def ParseProperty(bsize:Int,data:Array[Int])={
    val phProp=new PhysicalProperty((data(0)>>5<<1)+1,(((data(0)>>2)&7)<<1)+1,bsize)
    val res=GlobalProperty.DataLinkProperty//new DataLinkProperty(data(1)>>6,(data(1)>>3&7)<<1,if((data(1)&1)==1)4 else 2,phProp)
    phProp.CaptureProperty=capProp
    res
  }
  /**
   * read the original data from the center block
   * if error while decoding, return <0
   * otherwise the data is stored in dest and return >=0
   */
  def ReadOriProperty(pixelReader:PixelReader,bsize:Int,squareReader:SquareLocater,dest:Array[Int])={
    def blockReadByte(startIndex:Int)={
      def iter(i:Int,t:Int):Int=
        if(i<0)t else 
          iter(i-1,
              ColorMatcher.MatchPatternColor(capProp, 
                  pixelReader.GetPixelGaussYUVBuffered(squareReader.LocateTo((startIndex+i)/bsize,(startIndex+i)%bsize,destp)))
                  |(t<<1))
      iter(7,0)
      
    }
    val data=dest
    data(0)=blockReadByte(bsize)
    data(1)=blockReadByte(bsize*2)
    data(2)=blockReadByte(bsize*(bsize-2))
    data(3)=blockReadByte(bsize*(bsize-1))
    if(data(0)<0||data(1)<0||data(2)<0||data(3)<0)-1
    else{
      val decoder=new RsDecode(2)
      decoder.decode(data)
    }
  }
  /**
   * return the last 2 bit of frame id
   * return -1 if the block is not valid
   */
  def ReadFrameId(pixelReader:PixelReader,dlProp:DataLinkProperty,squareReader:SquareLocater)={
    
    val capProp=dlProp.PhysicalProperty.CaptureProperty
    val c1=pixelReader.GetPixelGaussYUVBuffered(squareReader.LocateTo(0,1,destp))
    val c2=pixelReader.GetPixelGaussYUVBuffered(squareReader.LocateTo(0,2,destp))
    val c3=pixelReader.GetPixelGaussYUVBuffered(squareReader.LocateTo(dlProp.PhysicalProperty.BlockSize-1,dlProp.PhysicalProperty.BlockSize-1,destp))
    if(c1==null||c2==null||c3==null)Utils.debugwarn("ReadFrameId","locater: "+squareReader.toString,0)
    val frameId1=ColorMatcher.MatchPatternColor(capProp, c1)
    val frameId2=ColorMatcher.MatchPatternColor(capProp, c2)
    val frameId1b=ColorMatcher.MatchPatternColor(capProp, c3)
    if(frameId1==frameId1b&&frameId1>=0)
      frameId2*2+frameId1
      else -1
  }
  /**
   * combines ReadBlockSize,ReadOriProperty and ParseProperty
   */
  def ReadProperty(pixelReader:PixelReader,squareReader:SquareLocater)={
    val bsize=ReadBlockSize(pixelReader,squareReader)
    Log.i("bsize",bsize+"")
    squareReader.AssignBlockSize(bsize)
    val data=new Array[Int](4)
    if(ReadOriProperty(pixelReader,bsize,squareReader,data)>=0)
      ParseProperty(bsize,data)
    else null
  }
  
  //useless
  /**
  private val colormem=new MemoryBuffer(400)({new YUVColor(0,0,0)})
  private var blockmem:MemoryBuffer[Block[YUVColor]]=null;
  /**
   * read a data block and get the original YUVColors(of course they are allocated)(in a Block)(or null if not applicable) data from it
   */
  def ReadBlock(pixelReader:PixelReader,dlProp:DataLinkProperty)=
    (squareReader:(Int,Int)=>Point,acceptParity:Int=>Boolean)=>{
      
      if(blockmem==null)blockmem=new MemoryBuffer(2)({new Block[YUVColor](dlProp.PhysicalProperty)})
      
      val capProp=dlProp.PhysicalProperty.CaptureProperty
      val blackcolor=pixelReader.GetPixelGaussYUVBuffered(squareReader(0,0))
      if(ColorMatcher.MatchPattern(capProp,blackcolor)){
        val frameId1=ColorMatcher.MatchPatternColor(capProp, pixelReader.GetPixelGaussYUVBuffered(squareReader(0,1)))
        val frameId2=ColorMatcher.MatchPatternColor(capProp, pixelReader.GetPixelGaussYUVBuffered(squareReader(0,2)))
        val frameId1b=ColorMatcher.MatchPatternColor(capProp, pixelReader.GetPixelGaussYUVBuffered(squareReader(dlProp.PhysicalProperty.BlockSize-1,dlProp.PhysicalProperty.BlockSize-1)))
        if(frameId1==frameId1b&&frameId1>=0&&
            (acceptParity==null||acceptParity(frameId1))){
          val block=blockmem.newObj
          block.FrameId=frameId2*2+frameId1
          block.Seek(0,0)
          for(i<-0 until dlProp.PhysicalProperty.BlockSize)//line y
            for(j<-0 until dlProp.PhysicalProperty.BlockSize){//col x
              block.Write(pixelReader.GetPixelYUVTo(squareReader(i,j), colormem.newObj))
          }
          block
        }
        else null
      }
      else null
    }
    
    */
}
