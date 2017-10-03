package anran.hdcode2.lib


/**


 * @author anran
 * DataLink properties that is used in DataLink layer, 
 * doing (read original data and send them to app layer) or 
 * (make bits error correctable, and become blocks, frames then handled in physical layer)
 * RSStrengthInFrame denotes Reed Solomon error correction ability in bytes.
 * RSStrengthFramesCount denotes NOT RS Code but Window size
 */
class DataLinkProperty(npar:Int,rsStrengthInFrame:Int,rsStrengthFramesCount:Int,colorbit:Int,phProp:PhysicalProperty) {
  val RSStrengthInFrame=rsStrengthInFrame
  val RSStrengthFramesCount=rsStrengthFramesCount
  
  val InterBlockECCount=npar
  
  //candidates
  private val parity1=(0 until InterBlockECCount).map(i=>
    (Range(0,phProp.CenterBlockId) ++ Range(phProp.CenterBlockId+1, phProp.BlockAvailableCount)).toArray).toArray
  
  private val parity2=Array(
      (Range(0,phProp.CenterBlockId)++Range(phProp.CenterBlockId+1,phProp.BlockAvailableCount)).filter(_%2==(phProp.BlockAvailableCount)%2).toArray,
      (Range(0,phProp.CenterBlockId)++Range(phProp.CenterBlockId+1,phProp.BlockAvailableCount)).toArray
  )
  
  private val borderlist=Range(0,phProp.BlockAvailableCount)
    .filter(i=>i%phProp.BlockCountX==0||i%phProp.BlockCountX==phProp.BlockCountX-1||i<phProp.BlockCountX||i/phProp.BlockCountX==phProp.BlockCountY-1)
  private val alllist=(Range(0,phProp.CenterBlockId) ++ Range(phProp.CenterBlockId+1, phProp.BlockAvailableCount))
  private val nonborderlist=alllist diff borderlist
  
  private val parity3=Array((Range(0,phProp.CenterBlockId)++Range(phProp.CenterBlockId+1,phProp.BlockAvailableCount-2)).filter(i=>(phProp.BlockAvailableCount-i-3)%4<2).toArray) ++ parity2

  for(i<-0 until 3){
    var ts=""
    for(j<-0 until parity3(i).length)
      ts+=parity3(i)(j)+" "
    
    //System.out.println("parities: "+ts)
  }
  
  private val parityn=
    (0 until InterBlockECCount).map(i=>
      (Range(phProp.BlockAvailableCount*i/InterBlockECCount,phProp.BlockAvailableCount*(i+2)/InterBlockECCount)
          .filter(i=>i!=phProp.CenterBlockId&&i<phProp.BlockAvailableCount)).toArray).toArray
    
      
      
  val InterBlockParity=InterBlockECCount match{
        case 1=>parity1
        case 2=>parity2
        case 3=>parity3
        case _=>parityn
      }
    
    
    //(i until ((phProp.BlockAvailableCount-1)*(i+1))/InterBlockECCount).toArray).toArray
  
  val ColorBit=colorbit
  val ColorCount=(1<<ColorBit)
  val ColorsPerByte=8/ColorBit
  println("ColorBit " + ColorBit + " ColorCount " + ColorCount + " ColorsPerByte " + ColorsPerByte)
  val Colors=List(
    new YUVColor(235,-127,-127),
    new YUVColor(235,-127,127),
    new YUVColor(235,127,-127),
    new YUVColor(235,127,127)
  )
   // List(new YUVColor(255,0,0),new YUVColor(220,0,0),new YUVColor(180,0,0),new YUVColor(150,0,0))
  
  val PhysicalProperty=phProp
  /**
   * Notice: include the first check byte
   */
  lazy val GetBlockDataBytesCount=(phProp.BlockSize*phProp.BlockSize-4-ColorCount)/(8/ColorBit)-RSStrengthInFrame*2
}