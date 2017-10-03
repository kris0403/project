package anran.hdcode2.library
/**
 * Memory buffer for fast allocation and garbage collection
 */
class MemoryBuffer[T:ClassManifest](n:Int=100)(f: =>T) {
  class tripleVar[A,B,C](_a:A,_b:B,_c:C){
    var a=_a
    var b=_b
    var c=_c
  }
  private val buffer=Array.fill(n){new tripleVar(true,0,f)}
  private var curr=0
  private val currlock=0
  def newObj()={
    while(!buffer(curr%n).a)curr+=1
    val res=buffer(curr%n)
    curr=curr+1
    res.c
  }
  def newLock()={
    while(!buffer(curr%n).a)curr+=1
    buffer(curr%n).a=false;
    buffer(curr%n).b=curr%n
    val res=buffer(curr%n)
    curr=curr+1
    res
  }
  def releaseLock(obj:tripleVar[Boolean,Int,T])
  {
    buffer(obj.b).a=true
  }
}