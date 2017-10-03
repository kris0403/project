package anran.hdcode2.lib

import java.awt.Point
import java.awt.Color

/**
 * @author anran
 * Physical properties that is used in physical layer, doing (camera data to locimport anran.hdcode2.lib.PresentProperty
ated points) or (data units to images)
 * blockCountX/Y denotes blocks' count in x axis and y axis
 * blockSize denotes one block has blockSize*blockSize data units
 */
class PhysicalProperty(blockCountX:Int,blockCountY:Int,blockSize:Int) {
  val BlockCountX=blockCountX
  val BlockCountY=blockCountY
  val BlockSize=blockSize
  
  val BlockAvailable=
    if(!GlobalProperty.SHAPECUSTOM)Array.fill(BlockCountY,BlockCountX)(true)
    else(GlobalProperty.shape.split(',').map(str=>
        str.map(c=>if(c=='_')false else true).toArray
      ).toArray)
  val BlockAvailableCount=(for(i<-0 until BlockCountY)
    yield 
    (for(j<-0 until BlockCountX)
      yield if(BlockAvailable(i)(j))1 else 0)
      .sum).sum
  
  val BlockById=(()=>{
    val res=Array.fill(BlockAvailableCount){new Point()}
    val resY=new Array[Int](BlockAvailableCount)
    var k=0
    for(i<-0 until BlockCountY)
      for(j<-0 until BlockCountX)
        if(BlockAvailable(i)(j)){
          res(k).x=j
          res(k).y=i
          k+=1
        }
    res
  })()
  val IdByBlock=(()=>{
    val res=Array.fill(BlockCountY,BlockCountX){-1}
    var k=0
    for(i<-0 until BlockCountY)
      for(j<-0 until BlockCountX)
        if(BlockAvailable(i)(j)){
          res(i)(j)=k
          k+=1
        }
    res
  })()
  val CenterBlockId=IdByBlock(BlockCountY/2)(BlockCountX/2)
  
  val DataUnitCountX=BlockCountX*BlockSize+1
  val DataUnitCountY=BlockCountY*BlockSize+1
 
  
  
  val PatternColor1=Color.BLACK
  val PatternColor2=new YUVColor(180,128,-128).Argb
  val PatternColor3=new YUVColor(180,-128,128).Argb
  
  var PresentProperty:PresentProperty=null
}