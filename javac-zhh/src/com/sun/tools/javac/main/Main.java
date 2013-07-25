/*
 * @(#)Main.java	1.115 07/03/21
 * 
 * Copyright (c) 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *  
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *  
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *  
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.javac.main;

import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.MissingResourceException;

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.JavacOption.Option;
import com.sun.tools.javac.main.RecognizedOptions.OptionHelper;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.processing.AnnotationProcessingError;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.annotation.processing.Processor;

/** This class provides a commandline interface to the GJC compiler.
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
@Version("@(#)Main.java	1.115 07/03/21")
public class Main {
	private static my.Debug DEBUG=new my.Debug(my.Debug.Main);//�Ҽ��ϵ�

    /** The name of the compiler, for use in diagnostics.
     */
    String ownName;

    /** The writer to use for diagnostic output.
     */
    PrintWriter out;

    /**
     * If true, any command line arg errors will cause an exception.
     */
    boolean fatalErrors;

    /** Result codes.
     */
    static final int
        EXIT_OK = 0,        // Compilation completed with no errors.
        EXIT_ERROR = 1,     // Completed but reported errors.
        EXIT_CMDERR = 2,    // Bad command-line arguments
        EXIT_SYSERR = 3,    // System error or resource exhaustion.
        EXIT_ABNORMAL = 4;  // Compiler terminated abnormally
        
    /*
    ʵ���ֶγ�ʼ��ϸ������:
    �������ڱ����ڼ䣬���ʵ���ֶεĳ�ʼ����(initializer code)������
    �е�һ����䲻�ǡ�this(��ѡ����)�����õĹ��췽��(constructor)��,
    ��JVM����ĳһ�����췽�����ɴ������ʵ��ʱ��ʵ���ֶεĳ�ʼ����ͱ�ִ���ˡ�
    
    ����:�������µ�Դ����Ƭ��
    private Option[] recognizedOptions = initializer code......
    
    public Main(String name, PrintWriter out) {
        this.ownName = name;
        this.out = out;
    }
    �������ڱ����ڼ�����µ�����������������(ֻΪ�˷�����⣬ʵ�ʲ�����ȫ��ͬ):
    public Main(String name, PrintWriter out) {
    	recognizedOptions = initializer code......//�������������֮ǰ
        this.ownName = name;
        this.out = out;
    }
    ����ϸ�ڲο�:com.sun.tools.javac.jvm.Gen���normalizeDefs()�������ڲ�ʵ��
    */
    
    /*
    recognizedOptions�ֶ�������Main���һ����ʵ��ʱ��
    Ĭ�ϱ���ʼ��Ϊ��51��Ԫ������Ϊ:
    com.sun.tools.javac.main.JavacOption.Option�����飬
    ��ӦRecognizedOptions�еġ�static Set<OptionName> javacOptions��
    */
    private Option[] recognizedOptions = RecognizedOptions.getJavaCompilerOptions(new OptionHelper() {//OptionHelper����RecognizedOptions�ڲ�����Ľӿ�

        public void setOut(PrintWriter out) {
            Main.this.out = out;
        }

        public void error(String key, Object... args) {
            Main.this.error(key, args);
        }

        public void printVersion() {
        	//��Ϊcom\sun\tools\javac\resources\version.properties�ļ�������
        	//�����޷�ȡ��version��Ϣ
        	//DEBUG.P("JavaCompiler.version()="+JavaCompiler.version());
        	/*
        	javac 1.7���ڡ�-version��ѡ������:
        	javac compiler message file broken: key=compiler.misc.version.resource.missing arguments=1.6.0-beta2, {1}, {2}, {3}, {4}, {5}, {6}, {7}
            
            javac 1.6���ڡ�-version��ѡ������: javac 1.6.0-beta2
            */
            Log.printLines(out, getLocalizedString("version", ownName,  JavaCompiler.version()));
        }

        public void printFullVersion() {
        	//��Ϊcom\sun\tools\javac\resources\version.properties�ļ�������
        	//�����޷�ȡ��fullVersion��Ϣ
        	//DEBUG.P("JavaCompiler.fullVersion()="+JavaCompiler.fullVersion());
        	/*
        	javac 1.7���ڡ�-fullversion��ѡ������:
        	javac �����汾 "compiler message file broken: key=compiler.misc.version.resource.missing arguments=1.6.0-beta2, {1}, {2}, {3}, {4}, {5}, {6}, {7}"
            
            javac 1.6���ڡ�-fullversion��ѡ������: javac �����汾 "1.6.0-beta2-b86"
            */
            Log.printLines(out, getLocalizedString("fullVersion", ownName,  JavaCompiler.fullVersion()));
        }

        public void printHelp() {
            help();
        }

        public void printXhelp() {
            xhelp();
        }

        public void addFile(File f) {
            if (!filenames.contains(f))
                filenames.append(f);
        }

        public void addClassName(String s) {
            classnames.append(s);
        }

    });

    /**
     * Construct a compiler instance.
     */
    public Main(String name) {
        this(name, new PrintWriter(System.err, true));
    }

    /**
     * Construct a compiler instance.
     */
    public Main(String name, PrintWriter out) {
    	/*
    	//recognizedOptions�������Ѿ���null�ˣ�ԭ���뿴�����ע��
    	//DEBUG.P("recognizedOptions="+recognizedOptions);
    	����������˳��Ҳ���Կ�����:
		class com.sun.tools.javac.main.RecognizedOptions===>getJavaCompilerOptions(1)
		-------------------------------------------------------------------------
		class com.sun.tools.javac.main.RecognizedOptions===>getOptions(2)
		-------------------------------------------------------------------------
		class com.sun.tools.javac.main.RecognizedOptions===>getOptions(2)  END
		-------------------------------------------------------------------------
		class com.sun.tools.javac.main.RecognizedOptions===>getJavaCompilerOptions(1)  END
		-------------------------------------------------------------------------
		com.sun.tools.javac.main.Main===>Main(2)
		-------------------------------------------------------------------------
		this.ownName=javac
		com.sun.tools.javac.main.Main===>Main(2)  END
		-------------------------------------------------------------------------
    	*/
    	
    	DEBUG.P(this,"Main(2)");

        this.ownName = name;
        this.out = out;
        
        DEBUG.P("this.ownName="+this.ownName);
        DEBUG.P(0,this,"Main(2)");
    }
    /** A table of all options that's passed to the JavaCompiler constructor.  */
    private Options options = null;

    /** The list of source files to process
     */
    public ListBuffer<File> filenames = null; // XXX sb protected

    /** List of class files names passed on the command line
     */
    public ListBuffer<String> classnames = null; // XXX sb protected

    /** Print a string that explains usage.
     */
    void help() {
    	/*����:
    	��com\sun\tools\javac\resources\javac.properties�ļ�������������:
    	-----------------------------------------------------
    	javac.msg.usage.header=\
		Usage: {0} <options> <source files>\n\
		where possible options include:
		-----------------------------------------------------
		��getLocalizedString()�����ڲ����ڵ�һ������"msg.usage.header"ǰ
		���ϡ�javac.���г�һ��Key=��javac.msg.usage.header"��Ȼ�����Key
		���Ҷ�Ӧ�����ݣ�Ȼ�����ò���"ownName"��ֵ�滻����ġ�{0}��,
		Log.printLines()������"\n"��ȡһ�в���ӡ���,���������:
		-----------------------------------------------------
		Usage: javac <options> <source files>
		where possible options include:
		-----------------------------------------------------
		*/
        Log.printLines(out, getLocalizedString("msg.usage.header", ownName));
        //�������������Ƶķ�����ӡÿ��һѡ��ĸ�ʽ��Ϣ
        for (int i=0; i<recognizedOptions.length; i++) {
            recognizedOptions[i].help(out);
        }
        out.println();
    }

    /** Print a string that explains usage for X options.
     */
    void xhelp() {
        for (int i=0; i<recognizedOptions.length; i++) {
            recognizedOptions[i].xhelp(out);
        }
        out.println();
        Log.printLines(out, getLocalizedString("msg.usage.nonstandard.footer"));
    }

    /** Report a usage error.
     */
    void error(String key, Object... args) {
        if (fatalErrors) {
            String msg = getLocalizedString(key, args);
            //��ȫ�޶�����:com.sun.tools.javac.util.PropagatedException
            throw new PropagatedException(new IllegalStateException(msg));
        }
        warning(key, args);
        Log.printLines(out, getLocalizedString("msg.usage", ownName));
    }

    /** Report a warning.
     */
    void warning(String key, Object... args) {
        Log.printLines(out, ownName + ": "
                       + getLocalizedString(key, args));
    }

    public Option getOption(String flag) {
        for (Option option : recognizedOptions) {
            if (option.matches(flag))
                return option;
        }
        return null;
    }

    public void setOptions(Options options) {
        if (options == null)
            throw new NullPointerException();
        this.options = options;
    }

    public void setFatalErrors(boolean fatalErrors) {
        this.fatalErrors = fatalErrors;
    }

    /** Process command line arguments: store all command line options
     *  in `options' table and return all source filenames.
     *  @param flags    The array of command line arguments.
     */
    public List<File> processArgs(String[] flags) { // XXX sb protected
    //String[] flags��ֵ����CommandLine.parse(args)�����,args�������в���
    try {//�Ҽ��ϵ�
    	DEBUG.P(this,"processArgs(1)");
		DEBUG.P("Options options.size()="+options.size());
        DEBUG.P("Options options.keySet()="+options.keySet());

        int ac = 0;
        while (ac < flags.length) {
        	DEBUG.P("flags["+(ac+1)+"]="+flags[ac]);
        	
            String flag = flags[ac];
            ac++;

            int j;
            // quick hack to speed up file processing: 
            // if the option does not begin with '-', there is no need to check
            // most of the compiler options.
            /*
            ����ĳ�����뼼���Ժ�ǿ��
            ��Ϊjavac�����е�ѡ�����ƶ�����'-'�ַ���ͷ��,recognizedOptions�����д�ŵ�
            ѡ��������һ����HiddenOption(SOURCEFILE)����'-'�ַ���ͷ�⣬��������ѡ��
            ���ƶ�����'-'�ַ���ͷ�ġ������javac�������г��ֲ�����'-'�ַ���ͷ��ѡ���
            ����λ��firstOptionToCheck��recognizedOptions������ĩβ��ʼ,
            (Ҳ����ֱ����recognizedOptions��������һ��ѡ��Ƚ�)
            ��Ҫô��Ҫ�����Դ�ļ���Ҫô�Ǵ����ѡ�
            
            ��������javac�������е�ѡ������'-'�ַ���ͷʱ��
            ����λ��firstOptionToCheck��recognizedOptions�����һ��Ԫ�ؿ�ʼ��ֱ��
            ����������recognizedOptions����(j == recognizedOptions.length)ʱ������
            ȷ���Ǵ����ѡ�
            */
            int firstOptionToCheck = flag.charAt(0) == '-' ? 0 : recognizedOptions.length-1;
            for (j=firstOptionToCheck; j<recognizedOptions.length; j++)
                if (recognizedOptions[j].matches(flag)) break;

            if (j == recognizedOptions.length) {
                error("err.invalid.flag", flag);
                return null;
            }
            

            Option option = recognizedOptions[j];
            //�ο�JavacOption.hasArg()�е�ע��
            if (option.hasArg()) {
                if (ac == flags.length) {
                	/*��������:
                	F:\Javac>javac -d
					javac: -d ��Ҫ����
					�÷�: javac <options> <source files>
					-help �����г����ܵ�ѡ��
					*/
                    error("err.req.arg", flag);
                    return null;
                }
                String operand = flags[ac];
                ac++;
                
                //�����process()�ڲ����ǰ�flag��operand����һ<K,V>�ԣ�
                //����options��,options���Կ�����һ��Map<K,V>
                //ϸ���뿴com.sun.tools.javac.main.RecognizedOptions���getAll()����
                if (option.process(options, flag, operand))
                    return null;
            } else {
            	//�����process()�ڲ����ǰ�flag��flag����һ<K,V>�ԣ�
                //����options��,options���Կ�����һ��Map<K,V>
                //ϸ���뿴com.sun.tools.javac.main.RecognizedOptions���getAll()����
                if (option.process(options, flag))
                    return null;
            }
        }
        
        //����javac��������ָ���ˡ�-d <Ŀ¼>��ѡ��ʱ��
        //���<Ŀ¼>�Ƿ���ڣ������ڻ���Ŀ¼����ʾ���󲢷���
        if (!checkDirectory("-d"))
            return null;
        //����javac��������ָ���ˡ�-s <Ŀ¼>��ѡ��ʱ��
        //���<Ŀ¼>�Ƿ���ڣ������ڻ���Ŀ¼����ʾ���󲢷���
        if (!checkDirectory("-s"))
            return null;
            
        //�����������û��-source��-targetѡ������Ĭ��ֵ
        String sourceString = options.get("-source");
        Source source = (sourceString != null)
        //������lookup()һ�����᷵��null,��Ϊ������
        //��(recognizedOptions[j].matches(flag))ʱ����д��Ѿ�������
            ? Source.lookup(sourceString)
            : Source.DEFAULT;
        String targetString = options.get("-target");
        //������lookup()һ�����᷵��null,��Ϊ������
        //��(recognizedOptions[j].matches(flag))ʱ����д��Ѿ�������
        Target target = (targetString != null)
            ? Target.lookup(targetString)
            : Target.DEFAULT;
        // We don't check source/target consistency for CLDC, as J2ME
        // profiles are not aligned with J2SE targets; moreover, a
        // single CLDC target may have many profiles.  In addition,
        // this is needed for the continued functioning of the JSR14
        // prototype.

        //�����"-target jsr14"������ִ������Ĵ���
        if (Character.isDigit(target.name.charAt(0))) {
        	//��target�İ汾��<source�İ汾��
            if (target.compareTo(source.requiredTarget()) < 0) {
                if (targetString != null) {
                    if (sourceString == null) {//ָ��-target��ûָ��-source�����
                    	/*��������:
                    	F:\Javac>javac -target 1.4
						javac: Ŀ��汾 1.4 ��Ĭ�ϵ�Դ�汾 1.5 ��ͻ
						*/
                        warning("warn.target.default.source.conflict",
                                targetString,
                                source.requiredTarget().name);
                    } else {//ָ��-target��ͬʱָ��-source�����
                    	/*��������:
                    	F:\Javac>javac -target 1.4 -source 1.5
						javac: Դ�汾 1.5 ��ҪĿ��汾 1.5
						*/
                        warning("warn.source.target.conflict",
                                sourceString,
                                source.requiredTarget().name);
                    }
                    return null;
                } else {
                	//û��ָ��-targetʱ��targetȡĬ�ϰ汾��(javac1.7Ĭ����1.6)
                	//���Ĭ�ϰ汾�Ż���source�ͣ���target�汾����source����
                    options.put("-target", source.requiredTarget().name);
                }
            } else {
            	//��target�İ汾��>=source�İ汾�����û�û��
            	//javac��������ָ����-target��ѡ��Ҳ�����ʹ��
            	//����ʱ��target�汾Ĭ��Ϊ1.4
                if (targetString == null && !source.allowGenerics()) {
                    options.put("-target", Target.JDK1_4.name);
                }
            }
        }
        return filenames.toList();
        
    }finally{//�Ҽ��ϵ�
    DEBUG.P("ListBuffer<File> filenames.size()="+filenames.size());
    DEBUG.P("ListBuffer<String> classnames.size()="+classnames.size());
    DEBUG.P("Options options.size()="+options.size());
    DEBUG.P("Options options.keySet()="+options.keySet());
	DEBUG.P(0,this,"processArgs(1)");
	}
	
    }
    // where
        private boolean checkDirectory(String optName) {
            String value = options.get(optName);
            if (value == null)
                return true;
            File file = new File(value);
            if (!file.exists()) {
                error("err.dir.not.found", value);
                return false;
            }
            if (!file.isDirectory()) {
                error("err.file.not.directory", value);
                return false;
            }
            return true;
        }

    /** Programmatic interface for main function.
     * @param args    The command line parameters.
     */
    public int compile(String[] args) {
    	DEBUG.P(this,"compile(1)");
    	
        Context context = new Context();
        JavacFileManager.preRegister(context); // can't create it until Log has been set up
        int result = compile(args, context);
        if (fileManager instanceof JavacFileManager) {
            // A fresh context was created above, so jfm must be a JavacFileManager
            ((JavacFileManager)fileManager).close();
        }
        
        DEBUG.P(0,this,"compile(1)");
        return result;
    }

    public int compile(String[] args, Context context) {
    	try {//�Ҽ��ϵ�
		DEBUG.P(this,"compile(2)");
		
		//��ȫ�޶�����:com.sun.tools.javac.util.List
		//��ȫ�޶�����:javax.tools.JavaFileObject
    	//List.<JavaFileObject>nil()��ʾ����һ����Ԫ��ΪJavaFileObject��
    	//�͵Ŀ�List(����null������ָsize=0)
        return compile(args, context, List.<JavaFileObject>nil(), null);
        
        }finally{//�Ҽ��ϵ�
		DEBUG.P(0,this,"compile(2)");
		}
    }

    /** Programmatic interface for main function.
     * @param args    The command line parameters.
     */
    public int compile(String[] args,
                       Context context,
                       List<JavaFileObject> fileObjects,
                       Iterable<? extends Processor> processors)
    {
    	try {//�Ҽ��ϵ�
    	DEBUG.P(this,"compile(4)");
    	
        if (options == null)
            options = Options.instance(context); // creates a new one
            
        //������ʵ���ֶε�ֵ�ڵ���processArgs()����ʱ��
        //����ͨ��RecognizedOptions.HiddenOption(SOURCEFILE)��process()�õ���.
        filenames = new ListBuffer<File>();//�����ʵ������
        classnames = new ListBuffer<String>();
        
        //��ȫ�޶�����:com.sun.tools.javac.main.JavaCompiler
        JavaCompiler comp = null;
        /*
         * TODO: Logic below about what is an acceptable command line
         * should be updated to take annotation processing semantics
         * into account.
         */
        try {
        	//��javac�����û���κ�ѡ�����ʱ��ʾ������Ϣ
            if (args.length == 0 && fileObjects.isEmpty()) {
                help();
                return EXIT_CMDERR;
            }

            List<File> filenames;//����Ǳ��ر�����ע�����滹�и�ͬ����ʵ������
            try {
                filenames = processArgs(CommandLine.parse(args));
                //��ѡ������ѡ���������ʱprocessArgs()�ķ���ֵ��Ϊnull
                if (filenames == null) {
                    // null signals an error in options, abort
                    return EXIT_CMDERR;
                } else if (filenames.isEmpty() && fileObjects.isEmpty() && classnames.isEmpty()) {
                    // it is allowed to compile nothing if just asking for help or version info
                    if (options.get("-help") != null
                        || options.get("-X") != null
                        || options.get("-version") != null
                        || options.get("-fullversion") != null)
                        return EXIT_OK;
                    error("err.no.source.files");
                    return EXIT_CMDERR;
                }
            } catch (java.io.FileNotFoundException e) {
            	DEBUG.P("java.io.FileNotFoundException");
            	//������쳣��֪�������׳�,
            	//��RecognizedOptions��new HiddenOption(SOURCEFILE)
            	//��process()����helper.error("err.file.not.found", f);
            	//���Դ�ļ�(.java)�����ڵĻ��������ﶼ�д�����ʾ��
            	//����ʹ�ļ������ڣ�Ҳ���׳�FileNotFoundException�쳣
                Log.printLines(out, ownName + ": " +
                               getLocalizedString("err.file.not.found",
                                                  e.getMessage()));
                return EXIT_SYSERR;
            }
            
            //��֪��"-Xstdout"�������"stdout"��ʲô����
            //�����������в�����ʹ��"stdout"
            //(�����ڳ����ڲ�����options��,������������Դ���룬Ҳû�ҵ����������)
            boolean forceStdOut = options.get("stdout") != null;
            if (forceStdOut) {
                out.flush();
                out = new PrintWriter(System.out, true);
            }
            
            DEBUG.P("����һ��JavacFileManager�����ʵ��...��ʼ");
            
            //����������䲻�ܵ����Ⱥ���򣬷������,
            //����ο�JavacFileManager.preRegister()�е�ע��
            context.put(Log.outKey, out);
            fileManager = context.get(JavaFileManager.class);
            
            DEBUG.P("����һ��JavacFileManager�����ʵ��...����");
            
            
            DEBUG.P(3);
            DEBUG.P("����һ��JavaCompiler�����ʵ��...��ʼ");
            //�ڵõ�JavaCompiler��ʵ���Ĺ���������˺ܶ��ʼ������
            comp = JavaCompiler.instance(context);
            DEBUG.P("����һ��JavaCompiler�����ʵ��...����");
            DEBUG.P(3);
            if (comp == null) return EXIT_SYSERR;

            if (!filenames.isEmpty()) {
                // add filenames to fileObjects
                comp = JavaCompiler.instance(context);
                List<JavaFileObject> otherFiles = List.nil();
                JavacFileManager dfm = (JavacFileManager)fileManager;
                //��JavacFileManager.getJavaFileObjectsFromFiles()�������
                //ÿһ��Ҫ�����Դ�ļ�������װ����һ��RegularFileObjectʵ����
                //RegularFileObject����JavacFileManager���ڲ��࣬ͬʱʵ����
                //JavaFileObject�ӿڣ�ͨ������getCharContent()��������һ��
                //java.nio.CharBufferʵ�������þͿ��Զ�Դ�ļ����ݽ��н����ˡ�
                //��com.sun.tools.javac.main.JavaCompiler���readSource()��
                //������������Ӧ��
                for (JavaFileObject fo : dfm.getJavaFileObjectsFromFiles(filenames))
                    otherFiles = otherFiles.prepend(fo);
                for (JavaFileObject fo : otherFiles)
                    fileObjects = fileObjects.prepend(fo);
            }
            comp.compile(fileObjects,
                         classnames.toList(),
                         processors);

            if (comp.errorCount() != 0 ||
                options.get("-Werror") != null && comp.warningCount() != 0)
                return EXIT_ERROR;
        } catch (IOException ex) {
            ioMessage(ex);
            return EXIT_SYSERR;
        } catch (OutOfMemoryError ex) {
            resourceMessage(ex);
            return EXIT_SYSERR;
        } catch (StackOverflowError ex) {
            resourceMessage(ex);
            return EXIT_SYSERR;
        } catch (FatalError ex) {
            feMessage(ex);
            return EXIT_SYSERR;
        } catch(AnnotationProcessingError ex) {
            apMessage(ex);
            return EXIT_SYSERR;
        } catch (ClientCodeException ex) {
            // as specified by javax.tools.JavaCompiler#getTask
            // and javax.tools.JavaCompiler.CompilationTask#call
            throw new RuntimeException(ex.getCause());
        } catch (PropagatedException ex) {
            throw ex.getCause();
        } catch (Throwable ex) {
            // Nasty.  If we've already reported an error, compensate
            // for buggy compiler error recovery by swallowing thrown
            // exceptions.
            if (comp == null || comp.errorCount() == 0 ||
                options == null || options.get("dev") != null)
                bugMessage(ex);
            return EXIT_ABNORMAL;
        } finally {
            if (comp != null) comp.close();
            filenames = null;
            options = null;
        }
        return EXIT_OK;
        
        }finally{//�Ҽ��ϵ�
		DEBUG.P(0,this,"compile(4)");
		}
    }

    /** Print a message reporting an internal error.
     */
    void bugMessage(Throwable ex) {
        Log.printLines(out, getLocalizedString("msg.bug",
                                               JavaCompiler.version()));
        ex.printStackTrace(out);
    }

    /** Print a message reporting an fatal error.
     */
    void feMessage(Throwable ex) {
        Log.printLines(out, ex.getMessage());
    }

    /** Print a message reporting an input/output error.
     */
    void ioMessage(Throwable ex) {
        Log.printLines(out, getLocalizedString("msg.io"));
        ex.printStackTrace(out);
    }

    /** Print a message reporting an out-of-resources error.
     */
    void resourceMessage(Throwable ex) {
        Log.printLines(out, getLocalizedString("msg.resource"));
//      System.out.println("(name buffer len = " + Name.names.length + " " + Name.nc);//DEBUG
        ex.printStackTrace(out);
    }

    /** Print a message reporting an uncaught exception from an
     * annotation processor.
     */
    void apMessage(AnnotationProcessingError ex) {
        Log.printLines(out,
                       getLocalizedString("msg.proc.annotation.uncaught.exception"));
        ex.getCause().printStackTrace();
    }
    
    //��ȫ�޶�����:javax.tools.JavaFileManager
    private JavaFileManager fileManager;

    /* ************************************************************************
     * Internationalization
     *************************************************************************/

    /** Find a localized string in the resource bundle.
     *  @param key     The key for the localized string.
     */
    public static String getLocalizedString(String key, Object... args) { // FIXME sb private
        try {
            if (messages == null)
                messages = new Messages(javacBundleName);
            return messages.getLocalizedString("javac." + key, args);
        }
        catch (MissingResourceException e) {
            throw new Error("Fatal Error: Resource for javac is missing", e);
        }
    }

    public static void useRawMessages(boolean enable) {
        if (enable) {
            messages = new Messages(javacBundleName) {
                    public String getLocalizedString(String key, Object... args) {
                        return key;
                    }
                };
        } else {
            messages = new Messages(javacBundleName);
        }
    }
    
    //��Դ�����Ƶ��ַ���ͨ����ȷ���ļ����������ļ���֮ǰ
    //���޶�����(�������"com.sun.tools.javac.resources")��
    //�����������·����ĳһĿ¼��
    private static final String javacBundleName =
        "com.sun.tools.javac.resources.javac";
        
    //��ȫ�޶�����:com.sun.tools.javac.util.Messages
    private static Messages messages;
}
