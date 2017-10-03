package anran.hdcode2.lib

object Utils {
  val DEBUG=false
  val VERBOSE=false
  val Log=new {
    def w(tag:String,content:String)=System.out.println(tag+": "+content)
    val i=w _
  }
  def debugwarn[T](str: =>String,res: T):T=if(DEBUG)debugwarn("DEBUG",str,res) else res
  def debugwarn[T](tag: =>String,str: =>String,res:T):T={
    if(DEBUG)Log.w(tag,str)
    res
  }
  def debuginfo[T](str: =>String,res: T):T=if(DEBUG)debuginfo("DEBUG",str,res) else res
  def debuginfo[T](tag: =>String,str: =>String,res:T):T={
    if(DEBUG)Log.i(tag,str)
    res
  }
  def debugverb[T](str: =>String,res:T):T={
    if(VERBOSE)Log.i("VERBOSE",str)
    res
  }
}