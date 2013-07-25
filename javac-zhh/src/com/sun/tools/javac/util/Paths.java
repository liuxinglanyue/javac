/*
 * @(#)Paths.java	1.25 07/03/21
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

package com.sun.tools.javac.util;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import com.sun.tools.javac.code.Lint;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.Position;
import javax.tools.JavaFileManager.Location;

import static com.sun.tools.javac.main.OptionName.*;
import static javax.tools.StandardLocation.*;

/** This class converts command line arguments, environment variables
 *  and system properties (in File.pathSeparator-separated String form)
 *  into a boot class path, user class path, and source path (in
 *  Collection<String> form).
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
@Version("@(#)Paths.java	1.25 07/03/21")
public class Paths {
	
	private static my.Debug DEBUG=new my.Debug(my.Debug.Paths);//�Ҽ��ϵ�
	
    /** The context key for the todo list */
    protected static final Context.Key<Paths> pathsKey =
	new Context.Key<Paths>();

    /** Get the Paths instance for this context. */
    public static Paths instance(Context context) {
	Paths instance = context.get(pathsKey);
	if (instance == null)
	    instance = new Paths(context);
	return instance;
    }

    /** The log to use for warning output */
    private Log log;

    /** Collection of command-line options */
    private Options options;

    /** Handler for -Xlint options */
    private Lint lint;

    protected Paths(Context context) {
    DEBUG.P(this,"Paths(1)");
	context.put(pathsKey, this);
	pathsForLocation = new HashMap<Location,Path>(16);
        setContext(context);
    DEBUG.P(0,this,"Paths(1)");
    }

    void setContext(Context context) {
        log = Log.instance(context);
        options = Options.instance(context);
        lint = Lint.instance(context);
    }

    /** Whether to warn about non-existent path elements */
    private boolean warn;

    private Map<Location, Path> pathsForLocation;

    private boolean inited = false; // TODO? caching bad?

    /**
     * rt.jar as found on the default bootclass path.  If the user specified a
     * bootclasspath, null is used.
     */
    private File bootClassPathRtJar = null;

    Path getPathForLocation(Location location) {
    	DEBUG.P(this,"getPathForLocation(1)");
        Path path = pathsForLocation.get(location);
        DEBUG.P("path="+path);
        if (path == null)
            setPathForLocation(location, null);
            
        DEBUG.P(0,this,"getPathForLocation(1)");
	return pathsForLocation.get(location);
    }
    
    void setPathForLocation(Location location, Iterable<? extends File> path) {
    DEBUG.P(this,"setPathForLocation(2)");
	// TODO? if (inited) throw new IllegalStateException
	// TODO: otherwise reset sourceSearchPath, classSearchPath as needed
	Path p;
	if (path == null) {
	    if (location == CLASS_PATH)
		p = computeUserClassPath();
	    else if (location == PLATFORM_CLASS_PATH)
		p = computeBootClassPath();
	    else if (location == ANNOTATION_PROCESSOR_PATH)
		p = computeAnnotationProcessorPath();
	    else if (location == SOURCE_PATH)
		p = computeSourcePath();
	    else 
		// no defaults for other paths
		p = null;
	} else {
	    p = new Path();
	    for (File f: path)
		p.addFile(f, warn); // TODO: is use of warn appropriate? 
	}
	pathsForLocation.put(location, p);
	
	DEBUG.P(1,this,"setPathForLocation(2)");
    }

    protected void lazy() {
    DEBUG.P(this,"lazy()");
    DEBUG.P("inited="+inited);
    
    //�ڳ�ʼ��ʱִ��(Ҳ������parser֮ǰ)
	if (!inited) {
		//�Ƿ����Xlint:�е�pathѡ��,һ��Ϊû��
		//�������-Xlint:pathʱ�����·�����д�ʱ���ᷢ������
	    warn = lint.isEnabled(Lint.LintCategory.PATH);
	    
	    pathsForLocation.put(PLATFORM_CLASS_PATH, computeBootClassPath());
	    
	    DEBUG.P(this,"computeUserClassPath()");
	    pathsForLocation.put(CLASS_PATH, computeUserClassPath());
	    DEBUG.P(2,this,"computeUserClassPath()");
	    
	    DEBUG.P(this,"computeSourcePath()");
	    pathsForLocation.put(SOURCE_PATH, computeSourcePath());
	    DEBUG.P(2,this,"computeSourcePath()");

	    inited = true;
	}
	
	DEBUG.P(0,this,"lazy()");
    }

    public Collection<File> bootClassPath() {
        lazy();
        return Collections.unmodifiableCollection(getPathForLocation(PLATFORM_CLASS_PATH));
    }
    public Collection<File> userClassPath() {
        lazy();
        return Collections.unmodifiableCollection(getPathForLocation(CLASS_PATH));
    }
    public Collection<File> sourcePath() {
        lazy();
	Path p = getPathForLocation(SOURCE_PATH);
        return p == null || p.size() == 0
            ? null
            : Collections.unmodifiableCollection(p);
    }

    boolean isBootClassPathRtJar(File file) {
        return file.equals(bootClassPathRtJar);
    }
    
    /*
    �����Ҽ��ϵ�,���ڵ���,����com.sun.tools.javac.util.JavacFileManager===>openArchive(1)
    */
    public File getBootClassPathRtJar() {
    	return bootClassPathRtJar;
    }
    
    //ʵ����Iterable<String>�ӿڵ����������foreach���ĵط�(JDK>=1.5������)
    private static class PathIterator implements Iterable<String> {
	private int pos = 0;
	private final String path;
	private final String emptyPathDefault;
	
	//���ֺ�";"(windows)��ð��":"(unix/linux)�����·���ֿ� 
	public PathIterator(String path, String emptyPathDefault) {
	    this.path = path;
	    this.emptyPathDefault = emptyPathDefault;
	}
	public PathIterator(String path) { this(path, null); }
	public Iterator<String> iterator() {
	    return new Iterator<String>() {//�����������ʵ����Iterator<E>�ӿ�
		public boolean hasNext() {
		    return pos <= path.length();
		}
		public String next() {
		    int beg = pos;
		    //File.pathSeparator·���ָ���,windows�Ƿֺ�";",unix/linux��ð��":"
		    int end = path.indexOf(File.pathSeparator, beg);
		    if (end == -1)
			end = path.length();
		    pos = end + 1;

		    if (beg == end && emptyPathDefault != null)
			return emptyPathDefault;
		    else
			return path.substring(beg, end);
		}
		public void remove() {
		    throw new UnsupportedOperationException();
		}
	    };
	}
    }

    private class Path extends LinkedHashSet<File> {
	private static final long serialVersionUID = 0;

	private boolean expandJarClassPaths = false;
        private Set<File> canonicalValues = new HashSet<File>();

	public Path expandJarClassPaths(boolean x) {
	    expandJarClassPaths = x;
	    return this;
	}

	/** What to use when path element is the empty string */
	private String emptyPathDefault = null;

	public Path emptyPathDefault(String x) {
	    emptyPathDefault = x;
	    return this;
	}

	public Path() { super(); }

	public Path addDirectories(String dirs, boolean warn) {
		DEBUG.P(this,"addDirectories(2)");
		DEBUG.P("warn="+warn+" dirs="+dirs);
		
	    if (dirs != null)
		for (String dir : new PathIterator(dirs))
		    addDirectory(dir, warn);
		
		DEBUG.P(1,this,"addDirectories(2)");
	    return this;
	}

	public Path addDirectories(String dirs) {
	    return addDirectories(dirs, warn);
	}
	
	//�Ӹ���Ŀ¼�²����ļ�ʱ��ֻ����չ��Ϊjar��zip���ļ�
	private void addDirectory(String dir, boolean warn) {
		try {//�Ҽ��ϵ�
		DEBUG.P(this,"addDirectory(2)");
		DEBUG.P("warn="+warn+" dir="+dir);
		
	    if (! new File(dir).isDirectory()) {
		if (warn)
		    log.warning("dir.path.element.not.found", dir);
		return;
	    }

        File[] files = new File(dir).listFiles();//�г�dirĿ¼�µ��ļ���Ŀ¼(û�еݹ���Ŀ¼)
        if (files == null)
            return;
            
	    for (File direntry : files) {
			if (isArchive(direntry)) {
				DEBUG.P("direntry="+direntry);
		    	addFile(direntry, warn);
		    }
	    }
	    
	    } finally {
			DEBUG.P(0,this,"addDirectory(2)");
		}
	}

	public Path addFiles(String files, boolean warn) {
		DEBUG.P(this,"addFiles(2)");
		DEBUG.P("warn="+warn+" files="+files);
	    if (files != null)
		for (String file : new PathIterator(files, emptyPathDefault)) {
			//DEBUG.P("fileName="+file);
		    addFile(file, warn);
		}
		DEBUG.P(1,this,"addFiles(2)");
	    return this;
	}

	public Path addFiles(String files) {
	    return addFiles(files, warn);
	}
	
	public Path addFile(String file, boolean warn) {
	    addFile(new File(file), warn);
	    return this;
	}

	public void addFile(File file, boolean warn) {
		try {//�Ҽ��ϵ�
		DEBUG.P(this,"addFile(2)");
		DEBUG.P("warn="+warn+" file="+file);
		
		
        File canonFile;
        try {
            //�淶�����ļ�(һ���ǰ�������·�����ļ�)
            canonFile = file.getCanonicalFile();
        } catch (IOException e) {
            canonFile = file;
        }
        DEBUG.P("canonFile="+canonFile);
        
        
        //contains(file)����??? ��LinkedHashSet<File>(Path�̳���LinkedHashSet<File>)
	    if (contains(file) || canonicalValues.contains(canonFile)) {
			/* Discard duplicates and avoid infinite recursion */
			
			DEBUG.P("�ļ��Ѵ���,����");
			return;
	    }
	    
	    DEBUG.P("file.exists()="+file.exists());
	    DEBUG.P("file.isFile()="+file.isFile());
	    DEBUG.P("file.isArchive()="+isArchive(file));
	    DEBUG.P("expandJarClassPaths="+expandJarClassPaths);
	    

	    if (! file.exists()) {
			/* No such file or directory exists */
			
			//��javac����������-Xlint:pathѡ��ʱ,����ļ���Ŀ¼�����ھͷ���warning
			//��com\sun\tools\javac\resources\compiler.properties�ļ��ж�����������Ŀ:
			//compiler.warn.path.element.not.found=\
    		//[path] bad path element "{0}": no such file or directory
			if (warn)
			    log.warning("path.element.not.found", file);	
	    } else if (file.isFile()) {
			/* File is an ordinary file. */ 
			if (!isArchive(file)) {
			    /* Not a recognized extension; open it to see if
			     it looks like a valid zip file. */
			    try {
					ZipFile z = new ZipFile(file);
					z.close();
					if (warn)
				    	log.warning("unexpected.archive.file", file);
			    } catch (IOException e) {
	                        // FIXME: include e.getLocalizedMessage in warning
		            //��:javac -Xlint:path -Xbootclasspath/p:F:\Javac\myout.txt
		            //��:warning: [path] Unexpected file on path: F:\Javac\myout.txt
					if (warn)
					    log.warning("invalid.archive.file", file);
					return;
			    }
			}
	    }
        
	    /* Now what we have left is either a directory or a file name
	       confirming to archive naming convention */
	       
	    //���ļ���Ŀ¼������ʱ�����߻���ͬ�������ӵ�HashSet<File>
	    super.add(file);//���� java.util.HashSet �̳еķ���
        canonicalValues.add(canonFile);
        
        //�Ƿ�չ��ѹ���ļ�(��jar�ļ�)
	    if (expandJarClassPaths && file.exists() && file.isFile())
			addJarClassPath(file, warn);
		
		} finally {
			DEBUG.P(0,this,"addFile(2)");
		}
	}

	// Adds referenced classpath elements from a jar's Class-Path
	// Manifest entry.  In some future release, we may want to
	// update this code to recognize URLs rather than simple
	// filenames, but if we do, we should redo all path-related code.
	private void addJarClassPath(File jarFile, boolean warn) {
		DEBUG.P(this,"addJarClassPath(2)");
		DEBUG.P("warn="+warn+" jarFile="+jarFile);
	    try {
		String jarParent = jarFile.getParent();
		
		DEBUG.P("jarParent="+jarParent);
		
		JarFile jar = new JarFile(jarFile);

		try {
		    Manifest man = jar.getManifest();
		    if (man == null) return;

		    Attributes attr = man.getMainAttributes();
		    if (attr == null) return;
		    
		    //��ָ��java.util.jar.Attributes.Name
		    String path = attr.getValue(Attributes.Name.CLASS_PATH);
		    DEBUG.P("Attributes.Name.CLASS_PATH="+path);
		    if (path == null) return;

		    for (StringTokenizer st = new StringTokenizer(path);
			 st.hasMoreTokens();) {
			String elt = st.nextToken();
			File f = (jarParent == null ? new File(elt) : new File(jarParent, elt));
			addFile(f, warn);
		    }
		} finally {
		    jar.close();
		}
	    } catch (IOException e) {
		log.error("error.reading.file", jarFile, e.getLocalizedMessage());
	    }
	    DEBUG.P(0,this,"addJarClassPath(2)");
	}
    }

    private Path computeBootClassPath() {
    DEBUG.P(this,"computeBootClassPath()");
    
    bootClassPathRtJar = null;
	String optionValue;
	Path path = new Path();
	
	DEBUG.P(XBOOTCLASSPATH_PREPEND+"="+options.get(XBOOTCLASSPATH_PREPEND));
	/*
	XBOOTCLASSPATH_PREPEND("-Xbootclasspath/p:")��com.sun.tools.javac.main.OptionName����
	-Xbootclasspath/p:<·��>     ����������·��֮ǰ
	����:
	javac -verbose -Xbootclasspath/p:F:\MyCompiler\bin Test.java
	[search path for source files: .]
	[search path for class files: F:\MyCompiler\bin,D:\Java\jre1.6.0\lib\resources.j
	ar,D:\Java\jre1.6.0\lib\rt.jar,D:\Java\jre1.6.0\lib\sunrsasign.jar,D:\Java\jre1.
	6.0\lib\jsse.jar,D:\Java\jre1.6.0\lib\jce.jar,D:\Java\jre1.6.0\lib\charsets.jar,
	D:\Java\jre1.6.0\classes,D:\Java\jre1.6.0\lib\ext\dnsns.jar,D:\Java\jre1.6.0\lib
	\ext\sunjce_provider.jar,D:\Java\jre1.6.0\lib\ext\sunmscapi.jar,D:\Java\jre1.6.0
	\lib\ext\sunpkcs11.jar,D:\Java\jre1.6.0\lib\ext\localedata.jar,.]
	*/

	path.addFiles(options.get(XBOOTCLASSPATH_PREPEND));
	
	
	DEBUG.P(ENDORSEDDIRS+"="+options.get(ENDORSEDDIRS));
	
	//-endorseddirs <Ŀ¼> ����ǩ���ı�׼·����λ��
	if ((optionValue = options.get(ENDORSEDDIRS)) != null)
	    path.addDirectories(optionValue);
	else {
		DEBUG.P("java.endorsed.dirs="+System.getProperty("java.endorsed.dirs"));
		//���:D:\Java\jre1.6.0\lib\endorsed(��Ŀ¼һ�㲻����)
	    path.addDirectories(System.getProperty("java.endorsed.dirs"), false);
	}
	    
	//-bootclasspath <·��>        �����������ļ���λ��
	DEBUG.P(BOOTCLASSPATH+"="+options.get(BOOTCLASSPATH));
    if ((optionValue = options.get(BOOTCLASSPATH)) != null) {
        path.addFiles(optionValue);
    } else {
            DEBUG.P("sun.boot.class.path="+System.getProperty("sun.boot.class.path"));
            //���:sun.boot.class.path=D:\Java\jre1.6.0\lib\resources.jar;D:\Java\jre1.6.0\lib\rt.jar;D:\Java\jre1.6.0\lib\sunrsasign.jar;D:\Java\jre1.6.0\lib\jsse.jar;D:\Java\jre1.6.0\lib\jce.jar;D:\Java\jre1.6.0\lib\charsets.jar;D:\Java\jre1.6.0\classes
            
            // Standard system classes for this compiler's release.
            String files = System.getProperty("sun.boot.class.path");
            path.addFiles(files, false);
            File rt_jar = new File("rt.jar");
            
            for (String file : new PathIterator(files, null)) {
                File f = new File(file);
                if (new File(f.getName()).equals(rt_jar))
                    bootClassPathRtJar = f;
            }
    }
        
    DEBUG.P(XBOOTCLASSPATH_APPEND+"="+options.get(XBOOTCLASSPATH_APPEND));
	path.addFiles(options.get(XBOOTCLASSPATH_APPEND));
	
	
	DEBUG.P(EXTDIRS+"="+options.get(EXTDIRS));

	// Strictly speaking, standard extensions are not bootstrap
	// classes, but we treat them identically, so we'll pretend
	// that they are.
	if ((optionValue = options.get(EXTDIRS)) != null)
	    path.addDirectories(optionValue);
	else {
		DEBUG.P("java.ext.dirs="+System.getProperty("java.ext.dirs"));
	    path.addDirectories(System.getProperty("java.ext.dirs"), false);
	}
	
	DEBUG.P(2,this,"computeBootClassPath()");
	return path;
    }
    
    
    
    
    
    //�û��������·������˳������(ǰһ�������ڲ���������)��
    //javac -classpath==>OS��������CLASSPATH==>application.home(�����֪��������?)==>
    //java -classpath ==>��ǰĿ¼(.)
    //������·�����jar��zip�ļ���Ҫչ��
    private Path computeUserClassPath() {
    DEBUG.P(CLASSPATH+"="+options.get(CLASSPATH));
	DEBUG.P("env.class.path="+System.getProperty("env.class.path"));
	DEBUG.P("application.home="+System.getProperty("application.home"));
	DEBUG.P("java.class.path="+System.getProperty("java.class.path"));
    
	String cp = options.get(CLASSPATH);
	// CLASSPATH environment variable when run from `javac'.
	if (cp == null) cp = System.getProperty("env.class.path");

	// If invoked via a java VM (not the javac launcher), use the
	// platform class path
	if (cp == null && System.getProperty("application.home") == null)
	    cp = System.getProperty("java.class.path");

	// Default to current working directory.
	if (cp == null) cp = ".";

	return new Path()
	    .expandJarClassPaths(true) // Only search user jars for Class-Paths
	    .emptyPathDefault(".")     // Empty path elt ==> current directory
	    .addFiles(cp);
    }





    private Path computeSourcePath() {
    //-sourcepath <·��>           ָ����������Դ�ļ���λ��
    DEBUG.P(SOURCEPATH+"="+options.get(SOURCEPATH));
    
	String sourcePathArg = options.get(SOURCEPATH);
	if (sourcePathArg == null)
	    return null;

	return new Path().addFiles(sourcePathArg);
    }


    private Path computeAnnotationProcessorPath() {
    try {
    //-processorpath <·��>        ָ������ע�ʹ�������λ��
    DEBUG.P(this,"computeAnnotationProcessorPath()");
    DEBUG.P(PROCESSORPATH+"="+options.get(PROCESSORPATH));
    
	String processorPathArg = options.get(PROCESSORPATH);
	if (processorPathArg == null)
	    return null;

	return new Path().addFiles(processorPathArg);
	
	}finally{
	DEBUG.P(0,this,"computeAnnotationProcessorPath()");
	}
    }

    /** The actual effective locations searched for sources */
    private Path sourceSearchPath;

    public Collection<File> sourceSearchPath() {
	if (sourceSearchPath == null) {
	    lazy();
	    Path sourcePath = getPathForLocation(SOURCE_PATH);
	    Path userClassPath = getPathForLocation(CLASS_PATH);
	    sourceSearchPath = sourcePath != null ? sourcePath : userClassPath;
	}
	return Collections.unmodifiableCollection(sourceSearchPath);
    }

    /** The actual effective locations searched for classes */
    private Path classSearchPath;

    public Collection<File> classSearchPath() {
	if (classSearchPath == null) {
	    lazy();
	    Path bootClassPath = getPathForLocation(PLATFORM_CLASS_PATH);
	    Path userClassPath = getPathForLocation(CLASS_PATH);
	    classSearchPath = new Path();
	    classSearchPath.addAll(bootClassPath);
	    classSearchPath.addAll(userClassPath);
	}
	return Collections.unmodifiableCollection(classSearchPath);
    }
    
    /** The actual effective locations for non-source, non-class files */
    private Path otherSearchPath;
    
    Collection<File> otherSearchPath() {
	if (otherSearchPath == null) {
	    lazy();
	    Path userClassPath = getPathForLocation(CLASS_PATH);
	    Path sourcePath = getPathForLocation(SOURCE_PATH);
	    if (sourcePath == null)
		otherSearchPath = userClassPath;
	    else {
		otherSearchPath = new Path();
		otherSearchPath.addAll(userClassPath);
		otherSearchPath.addAll(sourcePath);
	    }
	}
	return Collections.unmodifiableCollection(otherSearchPath);
    }

    /** Is this the name of an archive file? */
    private static boolean isArchive(File file) {
	String n = file.getName().toLowerCase();
	return file.isFile()
	    && (n.endsWith(".jar") || n.endsWith(".zip"));
    }
}
