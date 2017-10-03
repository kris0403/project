package anran.hdcode2.lib

import anran.hdcode2.sender.SenderApplication


object GlobalProperty {
  //shape customization
  
  val SHAPECUSTOM=false
  
  val shape=
      "_________,"+
      "__XX_XX__,"+
      "_XXXXXXX_,"+
      "XXXXXXXXX,"+
      "XXXXXXXXX,"+
      "_XXXXXXX_,"+
      "__XXXXX__,"+
      "___XXX___,"+
      "____X____"
      
  val shapeAlter=
      "X__X___,"+
      "X__X__X,"+
      "X__X___,"+
      "XXXX_XX,"+
      "X__X__X,"+
      "X__X__X,"+
      "X__X__X"
  
  val PhysicalProperty=new PhysicalProperty(SenderApplication.bx,SenderApplication.by,12)
  val DataLinkProperty=new DataLinkProperty(SenderApplication.npar,3,6,3,PhysicalProperty)
  val PresentProperty=new PresentProperty(SenderApplication.symbolsize,SenderApplication.symbolsize,SenderApplication.yoffset,SenderApplication.xoffset)
  PhysicalProperty.PresentProperty=PresentProperty
  
  val HEXAGON=false
  
  val TESTFRAME=false
  
  val KeepTrack=true
}