package anran.hdcode2.datalink

import anran.hdcode2.physical.Block
import anran.hdcode2.library.YUVColor
import anran.hdcode2.library.DataLinkProperty
import anran.hdcode2.library.MemoryBuffer
import anran.hdcode2.library.PixelReader
import anran.hdcode2.library.ColorMatcher
import jp.sourceforge.reedsolomon.RsDecode
import anran.hdcode2.physical.BlockState
import anran.hdcode2.library.Utils
import android.graphics.Point
import anran.hdcode2.physical.SquareLocater
import android.util.Log
import anran.hdcode2.GlobalProperty
import anran.hdcode2.NativeBridge

class BlockReader(dlProp:DataLinkProperty) {
  val blockSize=dlProp.PhysicalProperty.BlockSize
  val colors=Array.fill(dlProp.ColorCount){new YUVColor(-1,0,0)}
  val decoder=new RsDecode(dlProp.RSStrengthInFrame*2)
  val destp=new Point(0,0)
  val destc=new YUVColor(0,0,0)
  
  val COLORSMOOTH=0
  val LEFTCOLORSMOOTH=1-COLORSMOOTH
  def smooth(dest:YUVColor,src:YUVColor){
    if(dest.Y== -1)
      dest.Assign(src)
    else{
      dest.Y=(dest.Y*COLORSMOOTH+src.Y*LEFTCOLORSMOOTH).asInstanceOf[Int]
      dest.U=(dest.U*COLORSMOOTH+src.U*LEFTCOLORSMOOTH).asInstanceOf[Int]
      dest.V=(dest.V*COLORSMOOTH+src.V*LEFTCOLORSMOOTH).asInstanceOf[Int]
      
    }
  }
  def GetOriginalData(block:Block[YUVColor],dest:BlockState)={
    val res=dest.Data
    block.Seek(0,3)
    
    def itcolor(i:Int):Unit=
      if(i==dlProp.ColorCount)return else {
        smooth(colors(i),block.Read)
        itcolor(i+1)
      }
    itcolor(0);
    val matcher=ColorMatcher.MatchColor(colors,dlProp.ColorCount)
    
    val len=8/dlProp.ColorBit
    def itDataUnit(t:Int):Int={
      var res=0
      for(i<-0 until len)
        res=(res|(matcher(block.Read())<<(i*dlProp.ColorBit)))
      res
    }
    var i=0
    val reslen=res.length
    while(i<reslen){
      res(i)=itDataUnit(len)
      i=i+1
    }
    if(decoder.decode(res)<0){
      dest.Error=true
      Utils.debugwarn("[BlockReader.GetOriginalData]Error while RS decoding",false)
    }
    else{
      dest.Error=false
      
      true
    }

  }
  
  def GetOriginalData(pixelReader:PixelReader, locater:SquareLocater,dest:BlockState):BlockState={
    if(NativeBridge.decodeblock(locater.lt.x, locater.lt.y,
        locater.rt.x, locater.rt.y, 
        locater.lb.x, locater.lb.y, 
        locater.rb.x, locater.rb.y, 
        locater.blockSize, GlobalProperty.DataLinkProperty.RSStrengthInFrame*2,
        GlobalProperty.CaptureProperty.Width, GlobalProperty.CaptureProperty.Height, 
        pixelReader.src_data, dest.Data)){
          //Log.i("getOriginalData","true")
          dest.Error=false
          return dest;
    }
    
    //Log.i("getOriginalData","false")
          
    return null;
    
    //val tt=System.currentTimeMillis()
    val capProp=dlProp.PhysicalProperty.CaptureProperty
    
    val blackcolor=pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(0,0,destp))
    if(blackcolor==null)Utils.debugwarn("GetOriginalData", "locater: "+locater.toString(), 0)
    if(ColorMatcher.MatchPattern(capProp,blackcolor)){
      val frameId1=ColorMatcher.MatchPatternColor(capProp, pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(0,1,destp)))
      val frameId2=ColorMatcher.MatchPatternColor(capProp, pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(0,2,destp)))
      val frameId1b=ColorMatcher.MatchPatternColor(capProp, pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(dlProp.PhysicalProperty.BlockSize-1,dlProp.PhysicalProperty.BlockSize-1,destp)))
      if(frameId1==frameId1b&&frameId1>=0){
        var byte=0
        var t=0
        var i=0
        for(i<-0 until dlProp.ColorCount)
          colors(i)=pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(0, 3+i,destp))
        //Utils.debuginfo("colors",colors(0).str+" "+colors(1).str+" "+colors(2).str+" "+colors(3).str,0)
        ColorMatcher.AssignColors(colors, dlProp.ColorCount)
        i=3+dlProp.ColorCount
        while(i<dlProp.PhysicalProperty.BlockSize*dlProp.PhysicalProperty.BlockSize-1){
          byte=((byte<<dlProp.ColorBit)|ColorMatcher.Match(pixelReader.GetPixelYUVTo(locater.LocateTo(i/dlProp.PhysicalProperty.BlockSize, i%dlProp.PhysicalProperty.BlockSize, destp),destc)))
          if(i%dlProp.ColorsPerByte==2){
            dest.Data(t)=byte
            byte=0
            t=t+1
          }
          i=i+1
        }
        if(GlobalProperty.TESTFRAME||decoder.decode(dest.Data)<0){
          dest.Error=true
          Utils.debugwarn("[BlockReader.GetOriginalData]Error while RS decoding",false)
          dest
        }
        else{
          dest.Error=false
          //Log.i("GetOriginalData time",System.currentTimeMillis()-tt+" ")
          dest
        }
      }
      else null
    }
    else null
  }
  def GetTestData(pixelReader:PixelReader,locater:SquareLocater,dest:Array[Int])={
    val capProp=dlProp.PhysicalProperty.CaptureProperty
    
    val blackcolor=pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(0,0,destp))
    if(ColorMatcher.MatchPattern(capProp,blackcolor)){
      for(i<-0 until dlProp.ColorCount)
        colors(i)=pixelReader.GetPixelGaussYUVBuffered(locater.LocateTo(0, 3+i,destp))
      //Utils.debuginfo("colors",colors(0).str+" "+colors(1).str+" "+colors(2).str+" "+colors(3).str,0)
      ColorMatcher.AssignColors(colors, dlProp.ColorCount)
      var i=3+dlProp.ColorCount
      while(i<dlProp.PhysicalProperty.BlockSize*dlProp.PhysicalProperty.BlockSize-1){
        dest(i-3-dlProp.ColorCount)=ColorMatcher.Match(pixelReader.GetPixelYUVTo(
            locater.LocateTo(i/dlProp.PhysicalProperty.BlockSize, i%dlProp.PhysicalProperty.BlockSize, destp),destc))
            i=i+1
      }
      
      dest
    }
    else null
  }
}