@echo off 
rem ǰ���rem��ֻ����ʷ��¼�����ÿ�
rem java com.sun.tools.javac.Main -sourcepath F:\MyCompiler\bin -classpath F:\MyCompiler\bin -Xlint:path -verbose -Xbootclasspath/p:F:\MyCompiler\bin Test.java 1>myout.txt
rem java com.sun.tools.javac.Main -endorseddirs F:\MyCompiler\bin -Xlint:path -verbose -Xbootclasspath/p:F:\MyCompiler\bin Test.java 1>myout.txt
rem java com.sun.tools.javac.Main -proc:only -sourcepath F:\MyCompiler\bin -classpath F:\MyCompiler\bin -Xlint:path  -Xbootclasspath/p:F:\MyCompiler\bin Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -d mybin -printsource -s mysrc -verbose -sourcepath F:\javac\bin\mysrc;F:\Javac\bin\mysrc\my\test -classpath F:\javac\bin\classes;F:\javac\bin\mybin;F:\Javac\bin\classes\myJar.jar -Xlint:path  -Xbootclasspath/p:F:\javac\bin\classes;F:\Javac\bin\classes\myJar.jar mysrc\my\test\Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -d bin\mybin -printsource -s bin\mysrc -verbose -classpath F:\javac\bin\classes;F:\javac\bin\mybin;F:\Javac\bin\classes\myJar.jar -Xlint:path  -Xbootclasspath/p:F:\javac\bin\classes;F:\Javac\bin\classes\myJar.jar bin\mysrc\my\test\Test.java 1>myout.txt

rem �ӡ�-printsource�����������ֽ���
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -d bin\mybin -printsource -s bin\mysrc -classpath F:\javac\bin\classes;F:\javac\bin\mybin;F:\Javac\bin\classes\myJar.jar -Xlint:path  -Xbootclasspath/p:F:\javac\bin\classes;F:\Javac\bin\classes\myJar.jar bin\mysrc\my\test\Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\classes;F:\javac\bin\mybin;F:\Javac\bin\classes\myJar.jar -Xlint:path  -Xbootclasspath/p:F:\javac\bin\classes;F:\Javac\bin\classes\myJar.jar bin\mysrc\my\test\Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:F:\Javac\bin\mybin\myJar;F:\javac\bin\mybin;myout.txt -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -Xprefer:source -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -Xprefer:source -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\Test.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -Xprefer:source -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\package-info.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -Xprefer:source -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt
rem java -classpath F:\javac\bin\classes com.sun.tools.javac.Main -Xprefer:source -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\myenum\EnumTest.java 1>myEnumOut.txt
rem javac  -d bin\mybin -s bin\mysrc -classpath F:\javac\bin\mysrc;F:\javac\bin\mybin;F:\Javac\bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs F:\javac\bin\mybin bin\mysrc\my\test\Test.java 1>myout.txt

rem ��-Xjdbѡ��Ҫ����ʱ��������·���м���%JAVA_HOME%\lib\tools.jar
rem java -classpath %JAVA_HOME%\lib\tools.jar;bin\classes com.sun.tools.javac.Main -Xjdb

rem javac1.7�ı�׼ѡ�����-implicit:{none,class}
rem java -classpath bin\classes com.sun.tools.javac.Main -X

rem java -classpath bin\classes com.sun.tools.javac.Main -X -help -target 1.7 -moreinfo -implicit:none -implicit:class -g:lines,vars,source -Xprefer:source -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt

rem java -classpath bin\classes com.sun.tools.javac.Main -version -fullversion 1>myout.txt

rem javac -Aaaa=bbb -Accc -Addd= -Xprint -target 1.6 -moreinfo -g:lines,vars,source -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt

rem ͬʱ��-Aaaa=bbb -Accc -Addd= -Xprint���ڲ�����������:
rem java -classpath bin\classes com.sun.tools.javac.Main -Aaaa=bbb -Accc -Addd= -Xprint -target 1.7 -moreinfo -implicit:none -implicit:class -g:lines,vars,source -Xprefer:source -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt

rem javac  -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt

rem java -classpath bin\mybin my.test.Test3

rem java -classpath bin\classes com.sun.tools.javac.Main -Aaaa=bbb -Accc -Addd= -target 1.7 -moreinfo -implicit:none -implicit:class -g:lines,vars,source -Xprefer:source -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt

rem java -classpath bin\classes com.sun.tools.javac.Main -X -help -target 1.7 -moreinfo -implicit:none -implicit:class -g:lines,vars,source -Xprefer:source -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test3.java 1>myout.txt



java -classpath bin\classes com.sun.tools.javac.Main -Aaaa=bbb -Accc -Addd= -target 1.7 -moreinfo -implicit:none -implicit:class -g:lines,vars,source -Xprefer:source -d bin\mybin -s bin\mysrc -classpath bin\mysrc;bin\mybin;bin\mybin\myJar.jar;. -Xlint:path -Xbootclasspath/p:bin\mybin -endorseddirs bin\mybin bin\mysrc\my\test\Test.java 1>myout.txt