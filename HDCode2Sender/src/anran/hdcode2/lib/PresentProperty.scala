package anran.hdcode2.lib

/**
 * Present properties used in sender
 */
class PresentProperty(dataUnitWidth:Int,dataUnitHeight:Int,marginTop:Int,marginLeft:Int) {
  val DataUnitWidth=dataUnitWidth
  val DataUnitHeight=dataUnitHeight

  def TotalWidth(phProp:PhysicalProperty)=phProp.DataUnitCountX*DataUnitWidth+2*MarginLeft
  def TotalHeight(phProp:PhysicalProperty)=phProp.DataUnitCountY*DataUnitHeight+2*MarginTop
  
  val MarginTop=marginTop
  val MarginLeft=marginLeft
  
  var FPS=6
}