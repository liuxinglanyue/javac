/*方便copy的调试信息:
private static my.Debug DEBUG=new my.Debug(my.Debug.flag);//我加上的
//类全限定名称:
try {//我加上的
DEBUG.P(this,"loadClass()");
DEBUG.P("env="+env);

}finally{//我加上的
DEBUG.P(0,this,"loadClass");
}
DEBUG.P("kind="+Kinds.toString(kind));

DEBUG.P("flag="+Flags.toString(flag));

DEBUG.P("type.getKind()="+type.getKind());

DEBUG.P("tag="+TypeTags.toString(tag));

DEBUG.P("typeTag.tag="+TypeTags.toString(typeTag.tag));

DEBUG.P("tree.kind="+tree.getKind());
*/

package my;
public class Debug {
	//对应的类文件是否输出调试信息
	public static boolean Main=true, //com.sun.tools.javac.main.Main
	RecognizedOptions=true, //com.sun.tools.javac.main.RecognizedOptions
	JavacFileManager=true, //com.sun.tools.javac.util.JavacFileManager
	JavaCompiler=true, //com.sun.tools.javac.main.JavaCompiler
	JavacProcessingEnvironment=false, //com.sun.tools.javac.processing.JavacProcessingEnvironment
	DocCommentScanner=false,
	Keywords=true,
	Name=true,
	Scanner=true,
	Parser=true,
	Convert=true,
	Paths=true,
	
	TransTypes=false,TreeTranslator=false,
	Gen=false,Code=false,Items=false,ClassWriter=false;


	private boolean flag=true;
	private String className="";
	
	public Debug() {}
	
	public Debug(boolean flag) {
		this.flag=flag;
	}
	public Debug(boolean flag,String className) {
		this.flag=flag;
		this.className=className;
	}
	
	public void P(String s) {
		if(flag) System.out.println(s);
	}
	
	public void P(String s1,String s2) {
		P(s1,s2,false);
	}
	
	public void P(String s1,String s2,boolean b) {
		if(flag) System.out.println(s1+STR1+s2);
		if(b) System.exit(1);
	}
	
	public void P(String s,boolean b) {
		if(flag) System.out.println(s);
		if(b) System.exit(1);
	}
	
	public void P(Object o,boolean b) {
		if(flag) System.out.println(o);
		if(b) System.exit(1);
	}
	
	public void P(Object s1,Object s2) {
		P(s1,s2,false);
	}
	
	public void P(int n,Object s1,Object s2) {
		P(n,s1,s2,false);
	}
	
	public void P(int n,Object s1,Object s2,boolean b) {
		if(flag) {
			//String s=s1.toString();
			//if(s.indexOf("@")!=-1) s=s.substring(0,s.indexOf("@"));
			if(s1.getClass().getName().equals("java.lang.Class"))//static方法的情况
				System.out.print(s1+STR1+s2);
			else
				System.out.print(s1.getClass().getName()+STR1+s2);
			System.out.println("  END");
			System.out.println(STR2);
				
			for(int i=0;i<n;i++) System.out.println("");
		}
		if(b) System.exit(1);
	}
	
	public void P(Object s1,Object s2,boolean b) {
		if(flag) {
			
			//String s=s1.toString();
			//if(s.indexOf("@")!=-1) s=s.substring(0,s.indexOf("@"));
			if(s1.getClass().getName().equals("java.lang.Class"))//static方法的情况
				System.out.println(s1+STR1+s2);
			else
				System.out.println(s1.getClass().getName()+STR1+s2);
			System.out.println(STR2);
		}
		if(b) System.exit(1);
	}
	public void P(int n) {
		if(flag) {	
			for(int i=0;i<n;i++) System.out.println("");
		}
	}
	
	private String STR1="===>";
	private String STR2="-------------------------------------------------------------------------";
	
}
	
	