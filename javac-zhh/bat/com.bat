@echo off
rem 前面加rem的只是历史记录,不用看
rem javac -classpath ..\javac;..\javac\bin\classes;. -d bin\classes my\Debug.java

rem javac -classpath ..\javac;..\javac\bin\classes;. -d bin\classes com\sun\tools\javac\jvm\ClassWriter.java

rem javac -classpath ..\javac;..\javac\bin\classes;. -d bin\classes my\Debug.java com\sun\tools\javac\main\JavaCompiler.java

rem javac -classpath ..\javac;..\javac\bin\classes;. -d bin\classes my\Debug.java com\sun\tools\javac\parser\Parser.java

rem javac -classpath ..\javac;..\javac\bin\classes;. -d bin\classes my\Debug.java com\sun\tools\javac\main\Main.java


javac -classpath ..\javac;..\javac\bin\classes;. -d bin\classes my\Debug.java com\sun\tools\javac\Main.java