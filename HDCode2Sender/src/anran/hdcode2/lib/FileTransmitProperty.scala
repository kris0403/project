package anran.hdcode2.lib

class FileType
object FileTypes{
  case object ZIP extends FileType
  case object PlainText extends FileType
  case object JPEGImage extends FileType
  case object PNGImage extends FileType
}
/**
 * File transmit properties in Application layer in both sender and receiver
 */
class FileTransmitProperty(dlProp:DataLinkProperty, filetype:FileType, fileLength:Int, windowId:Int=0) {
  val DataLinkProperty=dlProp
  val FileType=filetype
  val FileLength=fileLength
  var WindowId=windowId
  
  def getExtension()=FileType match{
    case FileTypes.ZIP=>"zip"
    case FileTypes.PlainText=>"txt"
    case FileTypes.JPEGImage=>"jpg"
    case FileTypes.PNGImage=>"png"
    case _=>"bin"
  }
  lazy val GetHeaderLength=8//dlProp.GetBlockDataBytesCount//(8+24+1+7)/8
  //println("GetHeaderLength " + GetHeaderLength)
  //map a DataLink layer block id to FileTransmit block id
  val blockMap=(()=>{
    val result=new Array[Int](dlProp.PhysicalProperty.BlockAvailableCount)
    for(i<-1 until dlProp.PhysicalProperty.BlockAvailableCount) yield {
      if(dlProp.PhysicalProperty.CenterBlockId==i) result(i)=result(i-1)
      else{
        var isparity=false
        for(p<-dlProp.InterBlockParity) yield for(p<-dlProp.InterBlockParity)
          if(p(p.length-1)==i){
            result(i)=result(i-1)
            isparity=true
          }
        if(!isparity)
          result(i)=result(i-1)+1
      }
    }
    result
  })()
  val BlockAvailableCount=blockMap(blockMap.length-1)+1
  
  //bid is FileTransmit block id
  def GetBlockId(wid:Int,fid:Int,bid:Int)={
    val binf=BlockAvailableCount
    val ftot=fid*binf
    val wtot=wid*(dlProp.RSStrengthFramesCount-1)*binf
    val res=bid+ftot+wtot
    
    res
  }
  
  def GetPosition(wid:Int,fid:Int,bid:Int)={
    val binf=BlockAvailableCount
    val binb=dlProp.GetBlockDataBytesCount-2
    val ftot=fid*(binf*binb-GetHeaderLength)
    val wtot=wid*(dlProp.RSStrengthFramesCount-1)*(binf*binb-GetHeaderLength)
    val res=wtot+ftot+
      (if(bid==0)
        0 
      else bid*binb-GetHeaderLength)
    res
    
  }
  
  lazy val GetTotalWindowCount=(FileLength-1)/GetPosition(1,0,0)+1
  
  lazy val GetTotalBlockCount=(()=>{
    val fcount=((FileLength-GetPosition(GetTotalWindowCount-1,0,0))-1)/GetPosition(0,1,0)+1
    val bcount=((FileLength-GetPosition(GetTotalWindowCount-1,fcount-1,0))-1+GetHeaderLength)/(dlProp.GetBlockDataBytesCount-2)
    //Log.i("totalWindowCount",GetTotalWindowCount+" "+GetPosition(1,0,0))
    GetBlockId(GetTotalWindowCount-1,fcount-1,0)+bcount+1
  })()
  
}